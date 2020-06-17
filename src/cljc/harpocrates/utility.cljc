(ns harpocrates.utility)

(defn qualify-km
  "Qualify the keys within the map with the given namespace"
  [m ns]
  (->> m
       (mapv (fn [entry] [(->> (first entry) name (keyword ns)) (second entry)]))
       (into {})))
