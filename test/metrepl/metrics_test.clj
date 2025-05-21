(ns metrepl.metrics-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [matcher-combinators.matchers :as matchers]
   [matcher-combinators.test :refer [match?]]
   [metrepl.exporters :as exporters]
   [metrepl.metrics :as metrics]
   [nrepl.transport :refer [Transport]]))

(def mock-transport
  (reify Transport
    (recv [_])
    (recv [_ _])
    (send [_ _])))

(deftest metrify-op-task-test
  (testing "clone op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "clone"
                                                               :client-name "Foo"
                                                               :client-version "1.3.4"}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "clone"
                                                               :client-name "Foo"
                                                               :client-version "1.3.4"
                                                               :time-ms int?}}
                                                    metric))))]
      (-> (metrics/metrify-op-task {:op "clone" :client-name "Foo" :client-version "1.3.4" :transport mock-transport})
          :transport
          (.send {:status #{:done}}))))
  (testing "eval minimal op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "eval"}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "eval" :time-ms int?}}
                                                    metric))))]
      (-> (metrics/metrify-op-task {:op "eval" :code "(+ 1 2)" :transport mock-transport})
          :transport
          (.send {:status #{:done}}))))
  (testing "eval all op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "eval"
                                                               :ns "foo.bar"}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "eval"
                                                               :ns "foo.bar"
                                                               :time-ms int?}}
                                                    metric))))]
      (-> (metrics/metrify-op-task {:op "eval" :code "(+ 1 2)" :ns "foo.bar" :transport mock-transport})
          :transport
          (.send {:status #{:done}}))))
  (testing "load-file minimal op"
    (reset! metrics/first-load-file?* true)
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "load-file" :first-time true}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "load-file" :time-ms int? :first-time true}}
                                                    metric))))]
      (-> (metrics/metrify-op-task {:op "load-file" :file "(+ 1 2)" :transport mock-transport})
          :transport
          (.send {:status #{:done}}))))
  (testing "load-file all op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "load-file"
                                                               :first-time matchers/absent
                                                               :file-name "baz" :file-path "foo/bar/baz"}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "load-file"
                                                               :first-time matchers/absent
                                                               :file-name "baz" :file-path "foo/bar/baz"
                                                               :time-ms int?}}
                                                    metric))))]
      (-> (metrics/metrify-op-task {:op "load-file" :file "(+ 1 2)" :file-name "baz" :file-path "foo/bar/baz" :transport mock-transport})
          :transport
          (.send {:status #{:done}}))))
  (testing "test op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "test"
                                                               :ns "foo.bar"
                                                               :tests ["bar-test" "baz"]}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "test"
                                                               :ns "foo.bar"
                                                               :tests ["bar-test" "baz"]
                                                               :time-ms int?}}
                                                    metric))))]
      (-> (metrics/metrify-op-task {:op "test" :ns "foo.bar" :tests ["bar-test" "baz"] :transport mock-transport})
          :transport
          (.send {:status #{:done}}))))
  (testing "close op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "close"
                                                               :session-time-ms int?}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "close"
                                                               :session-time-ms int?}}
                                                    metric))))]
      (-> (metrics/metrify-op-task {:op "close" :transport mock-transport})
          :transport
          (.send {:status #{:done}})))))

(deftest metrify-repl-ready-test
  (with-redefs [exporters/export! (fn [metric]
                                    (case (:metric metric)
                                      :info/repl-ready
                                      (is (match? {:metric :info/repl-ready
                                                   :payload {:startup-time-ms 123
                                                             :project-types ["deps" "babashka"]
                                                             :middlewares (matchers/embeds ["metrepl.middleware.op-metrics/wrap-op-metrics"])
                                                             :dependencies {"org.clojure/clojure" "1.12.0"}}}
                                                  metric))))]
    (metrics/metrify-repl-ready 123)))
