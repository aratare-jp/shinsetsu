(ns harpocrates.common-ui
  (:require ["@elastic/eui" :refer [EuiButton]]
            [com.fulcrologic.fulcro.algorithms.react-interop :as interop]))

(def ui-button (interop/react-factory EuiButton))