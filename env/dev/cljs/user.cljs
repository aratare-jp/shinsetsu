(ns user
  (:require [cljs.spec.alpha :as s]
            [expound.alpha :as expound]
            [harpocrates.core :as core]))

(defn init
  "Init with a few extra things"
  []
  (extend-protocol IPrintWithWriter
    js/Symbol
    (-pr-writer [sym writer _]
      (-write writer (str "\"" (.toString sym) "\""))))

  (set! s/*explain-out* expound/printer)

  (enable-console-print!)

  (core/init))