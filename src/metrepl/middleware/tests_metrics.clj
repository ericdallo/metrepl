(ns metrepl.middleware.tests-metrics
  (:require
   [clojure.walk :as walk]
   [metrepl.metrics :as metrics]
   [metrepl.transport :as m.transport]
   [nrepl.middleware :as middleware]))

(defn ^:private handle-test [response]
  (when-let [summary (some-> (get response "summary") walk/keywordize-keys)]
    (let [results (get response "results")]
      (metrics/metrify :event/test-executed
                       {:elapsed-time-ms (get-in response ["elapsed-time" "ms"])
                        :ns (keys results)
                        :summary (walk/keywordize-keys summary)}))))

(defn wrap-tests-metrics
  "Wrap test related ops metrifying, emmiting these events:
   - `:event/test-executed` when test finished successfully or failed."
  [handler]
  (fn [{:keys [op] :as msg}]
    (if (= "test" op)
      (handler (m.transport/wrap msg {:on-before-send handle-test}))
      (handler msg))))

(middleware/set-descriptor!
 #'wrap-tests-metrics
 {:doc (:doc (meta #'wrap-tests-metrics))
  :expects #{"test"}
  :handles {}})
