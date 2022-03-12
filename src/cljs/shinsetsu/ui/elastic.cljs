(ns shinsetsu.ui.elastic
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :as ri]
    ["@elastic/eui" :refer [EuiButton]]))

(def button (ri/react-factory EuiButton))
