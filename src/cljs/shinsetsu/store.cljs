(ns shinsetsu.store
  (:require [malli.core :as m]))

(def browser-get-spec (m/schema :keyword))
(def browser-set-spec (m/schema [:cat :keyword :string]))
(def browser-get-validator (m/validator browser-get-spec))
(def browser-set-validator (m/validator browser-set-spec))

(defprotocol Storage
  "Used to handle storage of various platforms"
  (get-key [s ks])
  (set-key [s ks v]))

(defrecord BrowserStorage
  []
  Storage
  (get-key
    [s k]
    (if (browser-get-validator k)
      (let [k-str (name k)]
        ;; If not current available, grab from local storage.
        (if-let [v (get s k)]
          v
          (if-let [v (.getItem js/localStorage k-str)]
            (do
              (assoc s k v)
              v))))))
  (set-key
    [s k v]
    (if (browser-set-validator [k v])
      (let [k-str (name k)]
        (.setItem js/localStorage k-str v)
        (assoc s k v)))))

(def store (atom (->BrowserStorage)))

(comment
  (require '[cljs.core :refer [get set]])
  (name :key)
  (->BrowserStorage)
  (def foo (->BrowserStorage))
  (browser-set-validator ["a" "b"])
  (-> foo
      (get-key :b)
      )
  (let [s (->BrowserStorage {})]
    (println (set-key s [:a :b] "foo"))
    (get-key s [:a :b])))
