(ns metrepl.format
  (:require
   [clojure.string :as string]
   [jsonista.core :as j]))

(defn ^:private ->summary [data]
  (format "%s %s [%s] - %s"
          (:timestamp data)
          (string/upper-case (name (:level data)))
          (str (namespace (:metric data)) "/" (name (:metric data)))
          (:payload data)))

(defn ^:private ->edn [data]
  (pr-str (update data :timestamp str)))

(defn ^:private ->json [data]
  (j/write-value-as-string data))

(defn parse-data [data format]
  (case format
    :summary (->summary data)
    :edn (->edn data)
    :json (->json data)
    nil))
