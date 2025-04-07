(ns metrepl.exporters
  (:require
   [metrepl.config :as config]
   [metrepl.exporters.file :as exporters.file]
   [metrepl.exporters.stdout :as exporters.stdout])
  (:import
   [java.time Instant]))

(defn ^:private export!* [metric data]
  (let [metric-cfg (config/metric metric)
        data (assoc data
                    :timestamp (Instant/now)
                    :level (:level metric-cfg))]
    (when-not (identical? :off (:level metric-cfg))
      (doseq [[exporter exporter-cfg] (config/exporters)]
        (when (:enabled? exporter-cfg)
          (case exporter
            :stdout (exporters.stdout/export! data metric-cfg exporter-cfg)
            :file (exporters.file/export! data metric-cfg exporter-cfg)
            nil))))))

(defn export! [{:keys [metric] :as data}]
  (try
    (export!* metric data)
    (catch Exception e
      (println (.printStacktrace e)))))
