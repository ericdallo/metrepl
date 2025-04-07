(ns metrepl.middleware.op-metrics
  (:require
   [metrepl.metrics :as metrics]
   [nrepl.middleware :as middleware]
   [nrepl.middleware.session])
  (:import
   [java.lang.management ManagementFactory]))

(defonce first-op-metrified?* (atom false))

(defonce ^:private startup-jvm-uptime (.getUptime (ManagementFactory/getRuntimeMXBean)))

;; Workaround to export the startup JVM time
;; as close as possible to REPL ready to user.
(defonce ^:private _export-startup-metric!!!
  (metrics/metrify
   :event/jvm-started
   {:uptime-ms startup-jvm-uptime}))

(defn wrap-op-metrics
  "TODO ..."
  [handler]
  (fn [msg]
    (when-not @first-op-metrified?*
      (reset! first-op-metrified?* true)
      (metrics/metrify :event/first-op-requested
                       {:op (:op msg)
                        :time-since-startup-ms (- (.getUptime (ManagementFactory/getRuntimeMXBean)) startup-jvm-uptime)}))
    (metrics/metrify-op-task
     msg
     (fn [] (handler msg)))))

(middleware/set-descriptor!
  #'wrap-op-metrics
  {:doc "TODO"
   :expects #{#'nrepl.middleware.session/session}
   :handles {}})
