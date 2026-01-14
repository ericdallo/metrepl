(ns metrepl.metrics
  (:require
   [clojure.java.classpath :as classpath]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [metrepl.config :as config]
   [metrepl.exporters :as exporters]
   [metrepl.transport :as m.transport])
  (:import
   [java.lang.management ManagementFactory]
   [java.util.jar JarFile]))

(defonce first-load-file?* (atom true))

(defn ^:private msg->payload [{:keys [op] :as msg}]
  (merge {:op op}
         (case op
           "clone" (select-keys msg [:client-name :client-version])
           "describe" (select-keys msg [:middleware])
           "load-file" (cond-> (select-keys msg [:file-name :file-path])
                         @first-load-file?* (assoc :first-time true))
           "eval" (select-keys msg [:ns])
           "test" (select-keys msg [:ns :tests])
           "close" {:session-time-ms (.getUptime (ManagementFactory/getRuntimeMXBean))}
           nil)))

(defn metrify* [metric content-fn]
  (try
    (exporters/export! {:metric metric
                        :payload (content-fn)})
    (catch Exception e
      (doseq [[handler cfg] (config/error-handler)]
        (when (:enabled? cfg)
          (let [msg (str (with-out-str (binding [*err* *out*] (.printStackTrace e))) "\n")]
            (case handler
              :stdout (println "metrepl export error:" msg)
              :file (spit (io/file (:path cfg)) msg :append true))))))))

(defn metrify [metric content]
  (metrify* metric (constantly content)))

(defn metrify-op-task [msg]
  (let [payload (msg->payload msg)
        _ (metrify :event/op-requested payload)
        start-time (System/currentTimeMillis)]
    (when (and (= "load-file" (:op msg))
               @first-load-file?*)
      (reset! first-load-file?* false))
    (m.transport/wrap
     msg
     {:on-before-send
      (fn [response]
        (when (contains? (:status response) :done)
          (let [end-time (- (System/currentTimeMillis) start-time)]
            (metrify :event/op-completed (assoc payload :time-ms end-time)))))})))

(def ^:private type-by-file
  {"project.clj" "lein"
   "deps.edn" "deps"
   "bb.edn" "babashka"
   "shadow-cljs.edn" "shadow-cljs"
   "build.boot" "boot"
   "nbb.edn" "nbb"
   "build.gradle" "gradle"
   "build.gradle.kts" "gradle"})

(defn metrify-repl-ready [startup-time-ms]
  (metrify* :info/repl-ready
            (fn []
              (let [project-types (vec (keep
                                        #(get type-by-file %)
                                        (.list (io/file "."))))
                    dependencies (reduce
                                  (fn [acc ^JarFile jar]
                                    (let [[_ group artifact version] (re-find #".*.m2/repository/(.*)/(.*)/(.*)/.*.jar$" (.getName jar))]
                                      (assoc acc (str (string/replace group "/" ".") "/" artifact) version)))
                                  {}
                                  (classpath/classpath-jarfiles))]
                {:startup-time-ms startup-time-ms
                 :project-types project-types
                 :dependencies dependencies}))))
