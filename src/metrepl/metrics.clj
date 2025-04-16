(ns metrepl.metrics
  (:require
   [metrepl.exporters :as exporters]
   [metrepl.transport :as m.transport])
  (:import
   [java.lang.management ManagementFactory]))

(defn ^:private msg->payload [{:keys [op] :as msg}]
  (merge {:op op}
         (case op
           "clone" (select-keys msg [:client-name :client-version])
           "load-file" (select-keys msg [:file-name :file-path])
           "eval" (select-keys msg [:ns])
           "test" (select-keys msg [:ns :tests])
           "close" {:session-time-ms (.getUptime (ManagementFactory/getRuntimeMXBean))}
           nil)))

(defn metrify [metric content]
  (exporters/export! {:metric metric
                      :payload content}))

(defn metrify-op-task [msg]
  (let [payload (msg->payload msg)
        _ (exporters/export! {:metric :event/op-requested
                              :payload payload})
        start-time (System/currentTimeMillis)]
    (m.transport/wrap
     msg
     {:on-before-send
      (fn [response]
        (when (contains? (:status response) :done)
          (let [end-time (- (System/currentTimeMillis) start-time)]
            (exporters/export! {:metric :event/op-completed
                                :payload (assoc payload :time-ms end-time)}))))})))
