(ns quiescent-examples.dom)

;;------------------------------------------------------------------------------
;; DOM Helper Functions
;;------------------------------------------------------------------------------

(defn by-id [id]
  (.getElementById js/document id))

(defn set-html! [id-or-el html]
  (let [el (if (string? id-or-el) (by-id id-or-el) id-or-el)]
    (aset el "innerHTML" html)))
