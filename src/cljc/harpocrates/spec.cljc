(ns harpocrates.spec
  (:require #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s]))
  (:import java.util.UUID))

(def uuid-regex
  #"[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
(def email-regex
  #"^[a-zA-Z0-9_+&*-]+(?:\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,7}$")

(s/def ::uuid? (s/and string? #(re-matches uuid-regex %)))
(s/def ::email? (s/and string? #(re-matches email-regex %)))
