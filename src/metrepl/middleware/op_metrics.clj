(ns metrepl.middleware.op-metrics
  (:require
   [metrepl.metrics :as metrics]
   [metrepl.transport :as m.transport]
   [nrepl.middleware :as middleware]
   [nrepl.middleware.session])
  (:import
   [java.lang.management ManagementFactory]))

(defonce first-op-received?* (atom false))
(defonce middlewares* (atom nil))

(defn ^:private capture-middlewares-from-describe
  "Wraps the message to capture middlewares from describe op response."
  [msg]
  (if (= "describe" (:op msg))
    (m.transport/wrap
     msg
     {:on-before-send
      (fn [response]
        (when-let [auxs (:aux response)]
          (when-let [mws (get auxs "middleware")]
            (reset! middlewares* (vec mws)))))})
    msg))

(defn wrap-op-metrics
  "Wrap all ops metrifying each one, emmiting these events:
   - `:info/repl-ready` when receiving the first op metrifying project/repl info.
   - `:event/op-requested` when receiving any op.
   - `:event/op-completed` after completing an op."
  [handler]
  (fn [msg]
    (when-not @first-op-received?*
      (reset! first-op-received?* true)
      (metrics/metrify-repl-ready (.getUptime (ManagementFactory/getRuntimeMXBean)) @middlewares*))
    (handler (-> msg
                 capture-middlewares-from-describe
                 metrics/metrify-op-task))))

(middleware/set-descriptor!
 #'wrap-op-metrics
 {:doc (:doc (meta #'wrap-op-metrics))
  :expects #{#'nrepl.middleware.session/session}
  :handles {}})
