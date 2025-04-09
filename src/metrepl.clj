(ns metrepl
  (:require
   [metrepl.middleware.op-metrics]))

(def middleware (mapv
                 symbol
                 [#'metrepl.middleware.op-metrics/wrap-op-metrics]))
