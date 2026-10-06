(ns metrepl.format
  (:require
   [charred.api :as json]
   [clojure.string :as string]))

(defn ^:private ->summary [data]
  (format "%s %s %s [%s] - %s"
          (:timestamp data)
          (:hostname data)
          (string/upper-case (name (:level data)))
          (str (namespace (:metric data)) "/" (name (:metric data)))
          (:payload data)))

(defn ^:private ->edn [data]
  (pr-str (update data :timestamp str)))

(defn ^:private ->json [data]
  (json/write-json-str data :escape-slash false))

(defn parse-data ^String [data format]
  (case format
    :summary (->summary data)
    :edn (->edn data)
    :json (->json data)
    nil))
