(ns metrepl.middleware.op-metrics
  (:require
   [metrepl.metrics :as metrics]
   [nrepl.middleware :as middleware]
   [nrepl.middleware.session])
  (:import
   [java.lang.management ManagementFactory]))

(defonce first-op-received?* (atom false))

(defn wrap-op-metrics
  "Wrap all ops metrifying each one, emmiting these events:
   - `:info/repl-ready` when receiving the first op metrifying project/repl info.
   - `:event/op-requested` when receiving any op.
   - `:event/op-completed` after completing an op."
  [handler]
  (fn [msg]
    (when-not @first-op-received?*
      (reset! first-op-received?* true)
      (metrics/metrify-repl-ready (.getUptime (ManagementFactory/getRuntimeMXBean))))
    (handler (metrics/metrify-op-task msg))))

(middleware/set-descriptor!
 #'wrap-op-metrics
 {:doc (:doc (meta #'wrap-op-metrics))
  :expects #{#'nrepl.middleware.session/session}
  :handles {}})
