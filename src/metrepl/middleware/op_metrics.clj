(ns metrepl.middleware.op-metrics
  (:require
   [metrepl.metrics :as metrics]
   [nrepl.middleware :as middleware])
  (:import
   [java.lang.management ManagementFactory]))

(defonce startup-metrified?* (atom false))

;; Workaround to export the startup JVM time
;; as close as possible to REPL ready to user.
(defonce ^:private _export-startup-metric!!!
  (metrics/metrify
   :event/jvm-started
   {:uptime-ms (.getUptime (ManagementFactory/getRuntimeMXBean))}))

(defn wrap-op-metrics
  "TODO ..."
  [handler]
  (fn [{:keys [op] :as msg}]
    (when-not @startup-metrified?*
      (reset! startup-metrified?* true)
      (metrics/metrify :event/first-op-requested {:op (:op msg)}))
    (metrics/metrify-op-task
     op
     (fn [] (handler msg)))))

(middleware/set-descriptor!
 #'wrap-op-metrics
 {:doc "TODO"
  :expects #{}
  :handles {}})
