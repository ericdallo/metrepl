(ns metrepl.middleware.tests-metrics
  (:require
   [clojure.walk :as walk]
   [metrepl.metrics :as metrics]
   [nrepl.middleware :as middleware]
   [nrepl.transport :refer [Transport]]))

(defn ^:private handle-test [transport]
  (reify Transport
    (recv [_ timeout] (.recv transport timeout))
    (send [_ response]
      (when-let [summary (get response "summary")]
        (metrics/metrify :event/test-executed
                         {:elapsed-time-ms (get-in response ["elapsed-time" "ms"])
                          :ns (keys (get response "results"))
                          :summary (walk/keywordize-keys summary)}))
      (.send transport response))))

(defn wrap-tests-metrics
  "TODO"
  [handler]
  (fn [{:keys [op transport] :as msg}]
    (if (= "test" op)
      (handler (assoc msg :transport (handle-test transport)))
      (handler msg))))

(middleware/set-descriptor!
 #'wrap-tests-metrics
 {:doc (:doc (meta #'wrap-tests-metrics))
  :expects #{"test"}
  :handles {}})
