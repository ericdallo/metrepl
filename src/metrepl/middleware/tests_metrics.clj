(ns metrepl.middleware.tests-metrics
  (:require
   [clojure.walk :as walk]
   [metrepl.metrics :as metrics]
   [metrepl.transport :as m.transport]
   [nrepl.middleware :as middleware]))

(defn ^:private test-results-summary [results]
  (into {}
        (for [[ns m] results
              [var v] m]
          [(keyword (name ns) (name var))
           v])))

(defn ^:private handle-test [response]
  (when-let [summary (some-> (get response "summary") walk/keywordize-keys)]
    (let [results (walk/keywordize-keys (get response "results"))
          tests (test-results-summary results)]
      (metrics/metrify :event/tests-executed
                       {:elapsed-time-ms (get-in response ["elapsed-time" "ms"])
                        :ns (keys results)
                        :summary (walk/keywordize-keys summary)})
      (doseq [[_var tests] tests]
        (doseq [test tests]
          (when-let [event-name (case (:type test)
                                  "pass" :event/test-passed
                                  "fail" :event/test-failed
                                  "error" :event/test-errored
                                  nil)]
            (metrics/metrify event-name (select-keys test [:ns :var :file :error :context]))))))))

(defn wrap-tests-metrics
  "Wrap test related ops metrifying, emmiting these events:
   - `:event/tests-executed` when test finished successfully or failed."
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
