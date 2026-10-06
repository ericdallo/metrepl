(ns metrepl.format-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [metrepl.format :as format]))

(deftest parse-data-json-test
  (testing "serializes keywords, strings, numbers, bools, nested maps and vectors"
    (is (= "{\"metric\":\"event/op-completed\",\"level\":\"info\",\"payload\":{\"op\":\"eval\",\"time-ms\":12}}"
           (format/parse-data {:metric :event/op-completed
                               :level :info
                               :payload {:op "eval" :time-ms 12}}
                              :json))))
  (testing "does not escape forward slashes in namespaced keywords"
    (is (= "{\"metric\":\"info/repl-ready\"}"
           (format/parse-data {:metric :info/repl-ready} :json))))
  (testing "serializes nested maps and vectors"
    (is (= "{\"summary\":{\"pass\":3,\"fail\":0},\"ns\":[\"a\",\"b\"]}"
           (format/parse-data {:summary {:pass 3 :fail 0} :ns ["a" "b"]} :json)))))

(deftest parse-data-edn-test
  (testing "serializes as edn with stringified timestamp"
    (is (= "{:timestamp \"2026-10-05\", :op \"eval\"}"
           (format/parse-data {:timestamp "2026-10-05" :op "eval"} :edn)))))

(deftest parse-data-summary-test
  (testing "renders the summary line"
    (is (= "2026-10-05 host INFO [event/op-completed] - {:op \"eval\"}"
           (format/parse-data {:timestamp "2026-10-05"
                               :hostname "host"
                               :level :info
                               :metric :event/op-completed
                               :payload {:op "eval"}}
                              :summary)))))
