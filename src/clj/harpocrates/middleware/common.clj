(ns harpocrates.middleware.common)

(defn form-params->keywords
  "Convert form-params keys into keywords"
  [handler]
  (fn [req]
    (handler (update req :form-params (fn [fp]
                                        (into {} (mapv (fn [[k v]]
                                                         [(keyword k) v]) fp)))))))