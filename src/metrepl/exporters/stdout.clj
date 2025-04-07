(ns metrepl.exporters.stdout
  (:require
   [metrepl.format :as format]))

(defn export! [data _metric-cfg {:keys [format]}]
  (let [out-str (format/parse-data data format)]
    (println out-str)))
