(ns menus-example.core
  (:require
    cljsjs.react
    [clojure.string :refer [blank? lower-case]]
    [quiescent :include-macros true]
    [sablono.core :as sablono :include-macros true]
    [menus-example.util :refer [by-id js-log log set-html!]]))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def us-cities (js->clj js/US_CITIES))

;; NOTE: this is a very basic matching algorithm; could be greatly improved for
;; actual production use
(defn- city-match? [search-txt city-name]
  (let [search-txt (lower-case search-txt)
        city-name  (lower-case city-name)]
    (not= -1 (.indexOf city-name search-txt))))

(def dropdown-menu-limit 8)

(defn- find-cities
  "Finds cities that match search-txt."
  [search-txt]
  (->> us-cities
       (filter (partial city-match? search-txt))
       (take dropdown-menu-limit)))

(def mem-find-cities (memoize find-cities))

;;------------------------------------------------------------------------------
;; App State
;;------------------------------------------------------------------------------

(def initial-app-state {
  :start-city nil
  :finish-city nil

  :start-city-search-text ""
  :finish-city-search-text ""

  :active-filter nil
  })

(def app-state (atom initial-app-state))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- on-change-input [kwd js-evt]
  (let [new-value (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc kwd new-value)))

(defn- click-dropdown-option [search-text-kwd city-kwd city]
  (swap! app-state assoc city-kwd city
                         search-text-kwd ""))

(defn- click-city-token [app-state-kwd]
  (swap! app-state assoc app-state-kwd nil))

(defn- click-filter-link [filter-kwd]
  (swap! app-state assoc :active-filter filter-kwd))

;;------------------------------------------------------------------------------
;; React Components
;;------------------------------------------------------------------------------

(sablono/defhtml filter-links []
  [:div.filter-links
    [:span {:on-click (partial click-filter-link :fruits)} "Fruits"]
    [:span {:on-click (partial click-filter-link :vegetables)} "Vegetables"]
    [:span {:on-click (partial click-filter-link :meats)} "Meats"]
    [:span {:on-click (partial click-filter-link :drinks)} "Drinks"]])

(quiescent/defcomponent Filters []
  (sablono/html
    [:div.filters-wrapper
      [:label "Filters:"]
      (filter-links)
      ;; TODO: filter tokens here
      ]))

(sablono/defhtml city-option [search-text-kwd city-kwd city]
  [:div.dropdown-option {:on-click (partial click-dropdown-option search-text-kwd city-kwd city)}
    city])

(quiescent/defcomponent CityDropdown [[search-txt search-text-kwd city-kwd]]
  (let [matching-cities (mem-find-cities search-txt)]
    (sablono/html
      [:div.menu
        (if (empty? matching-cities)
          [:div.no-match "No matching cities."]
          (map (partial city-option search-text-kwd city-kwd) matching-cities))])))

(quiescent/defcomponent SelectedCityToken [[city kwd]]
  (sablono/html
    [:div.city-token
      {:on-click (partial click-city-token kwd)}
      city
      [:span.x "x"]]))

(quiescent/defcomponent CityInput [[search-txt search-text-kwd city-kwd]]
  (sablono/html
    [:div
      [:input {:on-change (partial on-change-input search-text-kwd)
               :placeholder "Please select a city"
               :type "text"
               :value search-txt}]
      (when-not (blank? search-txt)
        (CityDropdown [search-txt search-text-kwd city-kwd]))]))

(quiescent/defcomponent TokenOrInput [{:keys [label selected-city search-txt city-kwd search-text-kwd]}]
  (sablono/html
    [:div.input-wrapper
      [:label (str label ":")]
      (if selected-city
        (SelectedCityToken [selected-city city-kwd])
        (CityInput [search-txt search-text-kwd city-kwd]))]))

(quiescent/defcomponent SelectCitiesInputs [state]
  (sablono/html
    [:div
      (TokenOrInput {:label "Start City"
                  :selected-city (:start-city state)
                  :search-txt (:start-city-search-text state)
                  :city-kwd :start-city
                  :search-text-kwd :start-city-search-text})
      (TokenOrInput {:label "Finish City"
                  :selected-city (:finish-city state)
                  :search-txt (:finish-city-search-text state)
                  :city-kwd :finish-city
                  :search-text-kwd :finish-city-search-text})
      [:div.clearfix]]))

(defn- show-filters? [state]
  (and (:start-city state)
       (:finish-city state)))

(quiescent/defcomponent MenusApp [state]
  (sablono/html
    [:div.app-wrapper
      (SelectCitiesInputs state)
      (when (show-filters? state)
        (Filters state))]))

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def container-el (by-id "appContainer"))

(defn- on-change-app-state [_ _ _ new-state]
  (.render js/React (MenusApp new-state) container-el))

(add-watch app-state :render-dom on-change-app-state)

;;------------------------------------------------------------------------------
;; Show the State as JSON
;;------------------------------------------------------------------------------

;; TODO: use CLJS pprint here
(defn- show-app-state [_ _ _ new-state]
  (let [js-state (clj->js new-state)]
    (set-html! "appStateContainer" (.stringify js/JSON js-state nil 2))))

(add-watch app-state :show-state show-app-state)

;;------------------------------------------------------------------------------
;; Time Travel with the Slider
;;------------------------------------------------------------------------------

(def slider-el (by-id "historySlider"))
(def past-states (atom []))

(defn- save-app-state [_ _ _ new-state]
  ;; save this state
  (swap! past-states conj new-state)
  ;; update the slider
  (aset slider-el "max" (count @past-states))
  (aset slider-el "value" (count @past-states)))

(add-watch app-state :save-state save-app-state)

;;------------------------------------------------------------------------------
;; Native DOM Events
;;------------------------------------------------------------------------------

(defn- change-slider [js-evt]
  (let [state-idx (int (aget js-evt "target" "value"))
        state (get @past-states state-idx)]
    (when state
      (on-change-app-state nil nil nil state)
      (show-app-state nil nil nil state))))

(defn- add-events! []
  (.addEventListener slider-el "input" change-slider))

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init! []
  (add-events!)
  (swap! app-state identity))

(.addEventListener js/window "load" init!)
