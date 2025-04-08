(ns metrepl.exporters
  (:require
   [metrepl.config :as config]
   [metrepl.exporters.file :as exporters.file]
   [metrepl.exporters.stdout :as exporters.stdout])
  (:import
   [java.time Instant]))

(defn ^:private system-data* []
  {:os-name (System/getProperty "os.name")
   :os-version (System/getProperty "os.version")
   :os-arch (System/getProperty "os.arch")
   :hostname (try (.getHostName (java.net.InetAddress/getLocalHost))
                  (catch java.net.UnknownHostException _ nil))})

(def ^:private system-data (memoize system-data*))

(defn ^:private enhance-data [data metric-cfg]
  (merge
   (assoc data
          :timestamp (Instant/now)
          :level (:level metric-cfg))
   (system-data)))

(defn ^:private export!* [metric data]
  (let [metric-cfg (config/metric metric)
        data (enhance-data data metric-cfg)]
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
