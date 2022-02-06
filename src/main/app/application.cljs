(ns app.application
  (:require
    [com.fulcrologic.fulcro.application :as app]))

(defonce app (app/fulcro-app))
