(ns menus-example.core
  (:require
    cljsjs.react
    [clojure.string :refer [blank?]]
    [quiescent :include-macros true]
    [sablono.core :as sablono :include-macros true]
    [menus-example.util :refer [by-id js-log log set-html!]]))

;;------------------------------------------------------------------------------
;; Data
;;------------------------------------------------------------------------------

(def us-states (js->clj js/US_STATES))

;;------------------------------------------------------------------------------
;; App State
;;------------------------------------------------------------------------------

(def initial-state {
  :start-state-text ""
  :finish-state-text ""
  })

(def app-state (atom initial-state))

;;------------------------------------------------------------------------------
;; Events
;;------------------------------------------------------------------------------

(defn- on-change-input [kwd js-evt]
  (let [new-value (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc kwd new-value)))

;;------------------------------------------------------------------------------
;; React Components
;;------------------------------------------------------------------------------

(quiescent/defcomponent Filters []
  (sablono/html [:div "filters"]))

(quiescent/defcomponent StateDropdown [txt]
  (sablono/html
    [:div.menu "state dropdown"]))

(quiescent/defcomponent StartInput [txt]
  (sablono/html
    [:div.input-wrapper
      [:label "Start City:"]
      [:input {:on-change (partial on-change-input :start-state-text)
               :type "text"
               :value txt}]
      (when-not (blank? txt)
        (StateDropdown txt))]))

(quiescent/defcomponent FinishInput [txt]
  (sablono/html
    [:div.input-wrapper
      [:label "Finish City:"]
      [:input {:on-change (partial on-change-input :finish-state-text)
               :type "text"
               :value txt}]]))

(quiescent/defcomponent StartFinishInputs [[start-txt finish-txt]]
  (sablono/html
    [:fieldset
      [:legend "Select Start and End Cities"]
      (StartInput start-txt)
      (FinishInput finish-txt)
      [:div.clearfix]]))

(quiescent/defcomponent MenusApp [state]
  (sablono/html
    [:div.app-wrapper
      (StartFinishInputs [(:start-state-text state) (:finish-state-text state)])
      (when (:show-filter-inputs? state)
        (Filters state))]))

;;------------------------------------------------------------------------------
;; Render Loop
;;------------------------------------------------------------------------------

(def container-el (by-id "appContainer"))

(defn- on-change-app-state [_ _ _ new-state]
  (.render js/React (MenusApp new-state) container-el))

(add-watch app-state :change on-change-app-state)

;; TODO: use CLJS pprint here
(defn- show-app-state [_ _ _ new-state]
  (let [js-state (clj->js new-state)]
    (set-html! "appStateContainer" (.stringify js/JSON js-state nil 2))))

(add-watch app-state :show-state show-app-state)

;;------------------------------------------------------------------------------
;; Global App Init
;;------------------------------------------------------------------------------

(defn- init! []
  (swap! app-state identity))

(.addEventListener js/window "load" init!)
