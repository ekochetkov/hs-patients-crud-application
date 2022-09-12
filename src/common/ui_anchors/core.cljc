(ns common.ui-anchors.core)

(defn make-anchor
  ([anchor]      [:span {:id anchor :style {:display :none}}])
  ([anchor body] [:span {:id anchor} body]))
