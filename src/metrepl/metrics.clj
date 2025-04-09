(ns metrepl.metrics
  (:require
   [metrepl.exporters :as exporters]))

(defn ^:private msg->payload [{:keys [op] :as msg}]
  (merge {:op op}
         (case op
           "clone" (select-keys msg [:client-name :client-version])
           "load-file" (select-keys msg [:file-name :file-path])
           "eval" (select-keys msg [:ns])
           nil)))

(defn metrify [metric content]
  (exporters/export! {:metric metric
                      :payload content}))

(defn metrify-op-task [msg handle-op-fn]
  (let [payload (msg->payload msg)
        _ (exporters/export! {:metric :event/op-requested
                              :payload payload})
        start-time (System/currentTimeMillis)
        _ (handle-op-fn)
        end-time (- (System/currentTimeMillis) start-time)]
    (exporters/export! {:metric :event/op-completed
                        :payload (assoc payload :time-ms end-time)})))
