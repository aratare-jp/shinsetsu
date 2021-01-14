(ns harpocrates.middleware.authentication
  (:require [buddy.auth :as auth]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [harpocrates.config :refer [env]]))

(def ^:private backend (jws-backend {:secret (:secret env)}))

(def wrap-auth
  [wrap-authentication backend])
