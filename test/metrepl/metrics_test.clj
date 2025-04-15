(ns metrepl.metrics-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [matcher-combinators.test :refer [match?]]
   [metrepl.exporters :as exporters]
   [metrepl.metrics :as metrics]))

(def op-handled*? (atom false))

(defn ^:private reset-state! []
  (reset! op-handled*? false))

(use-fixtures :each (fn [test-f] (reset-state!) (test-f)))

(defn handle-op-fn []
  (reset! op-handled*? true))

(deftest metrify-op-task-test
  (testing "clone op"
    (reset-state!)
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
      (metrics/metrify-op-task {:op "clone" :client-name "Foo" :client-version "1.3.4"} handle-op-fn)
      (is @op-handled*?)))
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
      (metrics/metrify-op-task {:op "eval" :code "(+ 1 2)"} handle-op-fn)
      (is @op-handled*?)))
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
      (metrics/metrify-op-task {:op "eval" :code "(+ 1 2)" :ns "foo.bar"} handle-op-fn)
      (is @op-handled*?)))
  (testing "load-file minimal op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "load-file"}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "load-file" :time-ms int?}}
                                                    metric))))]
      (metrics/metrify-op-task {:op "load-file" :file "(+ 1 2)"} handle-op-fn)
      (is @op-handled*?)))
  (testing "load-file all op"
    (with-redefs [exporters/export! (fn [metric]
                                      (case (:metric metric)
                                        :event/op-requested
                                        (is (match? {:metric :event/op-requested
                                                     :payload {:op "load-file"
                                                               :file-name "baz" :file-path "foo/bar/baz"}}
                                                    metric))
                                        :event/op-completed
                                        (is (match? {:metric :event/op-completed
                                                     :payload {:op "load-file"
                                                               :file-name "baz" :file-path "foo/bar/baz"
                                                               :time-ms int?}}
                                                    metric))))]
      (metrics/metrify-op-task {:op "load-file" :file "(+ 1 2)" :file-name "baz" :file-path "foo/bar/baz"} handle-op-fn)
      (is @op-handled*?)))
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
      (metrics/metrify-op-task {:op "test" :ns "foo.bar" :tests ["bar-test" "baz"]} handle-op-fn)
      (is @op-handled*?))))
