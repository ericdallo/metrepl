(ns metrepl.middleware.op-metrics
  (:require
   [metrepl.metrics :as metrics]
   [nrepl.middleware :as middleware]
   [nrepl.middleware.session])
  (:import
   [java.lang.management ManagementFactory]))

(defonce first-op-metrified?* (atom false))

(defn wrap-op-metrics
  "Wrap all ops metrifying each one, emmiting these events:
   - `:event/first-op-requested` when receiving the first op.
   - `:event/op-requested` when receiving any op.
   - `:event/op-completed` after completing an op."
  [handler]
  (fn [msg]
    (when-not @first-op-metrified?*
      (reset! first-op-metrified?* true)
      (metrics/metrify :event/first-op-requested
                       {:op (:op msg)
                        :startup-time-ms (.getUptime (ManagementFactory/getRuntimeMXBean))}))
    (handler (metrics/metrify-op-task msg))))

(middleware/set-descriptor!
 #'wrap-op-metrics
 {:doc (:doc (meta #'wrap-op-metrics))
  :expects #{#'nrepl.middleware.session/session}
  :handles {}})
