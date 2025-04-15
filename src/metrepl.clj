(ns metrepl
  (:require
   [metrepl.middleware.op-metrics]
   [metrepl.middleware.tests-metrics]))

(def middleware (mapv
                 symbol
                 [#'metrepl.middleware.op-metrics/wrap-op-metrics
                  #'metrepl.middleware.tests-metrics/wrap-tests-metrics]))
