(ns metrepl.metrics
  (:require
   [metrepl.exporters :as exporters]))

(defn metrify [metric content]
  (exporters/export! {:metric metric
                      :payload content}))

(defn metrify-op-task [op op-fn]
  (exporters/export! {:metric :event/op-requested
                      :payload {:op op}})
  (let [start-time (System/currentTimeMillis)
        _ (op-fn)
        end-time (- (System/currentTimeMillis) start-time)]
    (exporters/export! {:metric :event/op-completed
                        :payload {:op op
                                  :time-ms end-time}})))
