(ns metrepl
  (:require
   [metrepl.middleware.op-metrics :as middleware.load-file-metrics]))

(def middleware (mapv
                 symbol
                 [#'middleware.load-file-metrics/wrap-op-metrics]))
