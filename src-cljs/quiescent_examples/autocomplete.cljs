(ns quiescent-examples.autocomplete
  (:require
    cljsjs.react
    [cljs.pprint :refer [pprint]]
    [clojure.string :refer [blank? lower-case]]
    [quiescent :include-macros true]
    [sablono.core :as sablono :include-macros true]
    [quiescent-examples.dom :refer [by-id set-html!]]
    [quiescent-examples.util :refer [js-log log]]))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def us-cities (js->clj js/US_CITIES))

;; NOTE: this is a very basic matching algorithm; could be improved for actual
;; production use
(defn- city-match? [search-txt city-name]
  (let [search-txt (lower-case search-txt)
        city-name  (lower-case city-name)]
    (not= -1 (.indexOf city-name search-txt))))

(def dropdown-menu-limit 8)

(defn- find-cities
  "Finds cities that match search-txt."
  [search-txt]
  (if (blank? search-txt) []
    (->> us-cities
         (filter (partial city-match? search-txt))
         (take dropdown-menu-limit)
         vec)))

(def mem-find-cities (memoize find-cities))

;;------------------------------------------------------------------------------
;; App State
;;------------------------------------------------------------------------------

(def initial-app-state {
  :start-city nil
  :start-city-search-text ""
  :start-city-active-option-idx 0
  :start-city-options []

  :end-city nil
  :end-city-search-text ""
  :end-city-active-option-idx 0
  :end-city-options []})

(def app-state (atom initial-app-state))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- change-search-text [start-or-end js-evt]
  (let [search-txt-kwd (if (= start-or-end :start) :start-city-search-text :end-city-search-text)
        options-kwd    (if (= start-or-end :start) :start-city-options     :end-city-options)
        new-search-txt (aget js-evt "currentTarget" "value")
        matching-cities (find-cities new-search-txt)]
    (swap! app-state assoc search-txt-kwd new-search-txt
                           options-kwd matching-cities)))

(def enter-keycode 13)
(def down-keycode  40)
(def up-keycode    38)

(defn- keydown-input [start-or-end js-evt]
  (let [keycode (int (aget js-evt "keyCode"))
        idx-kwd (if (= start-or-end :start) :start-city-active-idx :end-city-active-idx)
        current-idx (idx-kwd @app-state)]
    (cond
      (= keycode up-keycode)
        (swap! app-state update-in [idx-kwd] dec)
      (and (= keycode down-keycode)
           (not= 0 current-idx))
        (swap! app-state update-in [idx-kwd] inc)
      :else
        nil)))

(defn- click-dropdown-option [start-or-end city]
  (let [city-kwd (if (= start-or-end :start) :start-city :end-city)
        search-text-kwd (if (= start-or-end :start) :start-city-search-text :end-city-search-text)]
    (swap! app-state assoc city-kwd city
                           search-text-kwd "")))

(defn- click-city-token
  "Remove the selection."
  [kwd]
  (swap! app-state assoc kwd nil))

(defn- mouseenter-dropdown-row [start-or-end idx]
  (let [kwd (if (= start-or-end :start) :start-city-active-option-idx :end-city-active-option-idx)]
    (swap! app-state assoc kwd idx)))

;;------------------------------------------------------------------------------
;; React Components
;;------------------------------------------------------------------------------

(quiescent/defcomponent MenuOption [[start-or-end idx city]]
  (sablono/html
    [:div {:class (str "dropdown-option" (when (= active-idx idx) " active"))
           :on-click (partial click-dropdown-option start-or-end city)
           :on-mouse-enter (partial mouseenter-dropdown-row start-or-end idx)}
      city]))

(quiescent/defcomponent DropdownMenu [[start-or-end options]]
  (sablono/html
    [:div.menu
      (if (empty? options)
        [:div.no-match "No matching cities."]
        (map-indexed #(MenuOption [start-or-end %1 %2]) options))]))

(quiescent/defcomponent SelectedOptionToken [[city kwd]]
  (sablono/html
    [:div.city-token
      {:on-click (partial click-city-token kwd)}
      city
      [:span.x "x"]]))

(quiescent/defcomponent AutocompleteInput [[start-or-end search-txt options]]
  (sablono/html
    [:div
      [:input {:on-change (partial change-search-text start-or-end)
               :on-key-down (partial keydown-input start-or-end)
               :placeholder "Please select a city"
               :type "text"
               :value search-txt}]
      (when-not (blank? search-txt)
        (DropdownMenu [start-or-end options]))]))

(quiescent/defcomponent App [{:keys [start-city start-city-search-text start-city-options
                                     end-city end-city-search-text end-city-options]}]
  (sablono/html
    [:div
      [:div.input-wrapper
        [:label "Start City:"]
        (if start-city
          (SelectedOptionToken start-city :start-city)
          (AutocompleteInput [:start start-city-search-text start-city-options]))]
      [:div.input-wrapper
        [:label "End City:"]
        (if end-city
          (SelectedOptionToken end-city :end-city)
          (AutocompleteInput [:end end-city-search-text end-city-options]))]
      [:div.clearfix]]))

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def container-el (by-id "appContainer"))

(defn- on-change-app-state [_ _ _ new-state]
  (.render js/React (App new-state) container-el))

(add-watch app-state :render-dom on-change-app-state)

;;------------------------------------------------------------------------------
;; Show the State as EDN
;;------------------------------------------------------------------------------

;; capture the result of pprint in a string
(def pprint-butter (atom ""))

(defn- init-pprint!
  "Sets *print-fn* to pipe to the pprint-buffer atom so we can use the result as
   a string."
  []
  (set! *print-newline* false)
  (set! *print-fn* (fn [x]
    (swap! pprint-butter str x))))

(def app-state-text-el (by-id "appStateText"))

(defn- show-app-state [_ _ _ new-state]
  (reset! pprint-butter "")
  (pprint new-state)
  (set-html! app-state-text-el @pprint-butter))

(add-watch app-state :show-state show-app-state)

;;------------------------------------------------------------------------------
;; Time Travel with the Slider
;;------------------------------------------------------------------------------

(def slider-el (by-id "historySlider"))
(def past-states (atom []))

(defn- save-app-state [_ _ _ new-state]
  ;; save this state
  (swap! past-states conj new-state)

  (let [num-states (count @past-states)]
    ;; update the slider
    (aset slider-el "max" num-states)
    (aset slider-el "value" num-states)
    ;; update state counts
    (set-html! "currentState" num-states)
    (set-html! "totalStates" num-states)))

(add-watch app-state :save-state save-app-state)

;;------------------------------------------------------------------------------
;; Native DOM Events
;;------------------------------------------------------------------------------

(defn- change-slider [js-evt]
  (let [state-idx (int (aget js-evt "target" "value"))
        state (get @past-states state-idx)]
    (when state
      (on-change-app-state nil nil nil state)
      (show-app-state nil nil nil state)
      (set-html! "currentState" (inc state-idx)))))

(defn- add-events! []
  (.addEventListener slider-el "input" change-slider))

;;------------------------------------------------------------------------------
;; Initialize the Example
;;------------------------------------------------------------------------------

(defn- init! []
  (init-pprint!)
  (add-events!)
  (swap! app-state identity))

(goog/exportSymbol "initAutocomplete" init!)
