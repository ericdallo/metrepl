(ns metrepl.exporters.file
  (:require
   [clojure.java.io :as io]
   [metrepl.format :as format]))

(defn export! [data _metric-cfg {:keys [format path]}]
  (let [content-str (format/parse-data data format)]
    (io/make-parents path)
    (spit (io/file path) (str content-str "\n") :append true)))
