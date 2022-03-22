(ns shinsetsu.schema-test
  (:require
    [expectations.clojure.test :refer [defexpect expect]]
    [shinsetsu.schema :as s]
    [malli.core :as m]))

(defexpect normal-hex-colour-string
  (expect true (s/is-hex-colour-str? "#fff"))
  (expect true (s/is-hex-colour-str? "#ffffff")))

(defexpect invalid-hex-colour-string
  (expect false (s/is-hex-colour-str? ""))
  (expect false (s/is-hex-colour-str? "#a"))
  (expect false (s/is-hex-colour-str? "a")))

(defexpect normal-hex-colour-spec
  (expect true (m/validate s/hex-colour-spec "#ffffff"))
  (expect true (m/validate s/hex-colour-spec "#fff")))

(defexpect invalid-hex-colour-spec
  (expect false (m/validate s/hex-colour-spec ""))
  (expect false (m/validate s/hex-colour-spec "#f"))
  (expect false (m/validate s/hex-colour-spec "a")))

(comment
  (require '[kaocha.repl :as k])
  (k/run 'shinsetsu.schema-test)
  (k/run #'shinsetsu.schema-test/invalid-hex-colour-spec))
