(ns metrepl.transport
  (:require
   [nrepl.transport :refer [Transport]]))

(defn wrap [msg {:keys [on-before-send]}]
  (update msg :transport
          (fn [transport]
            (reify Transport
              (recv [_] (.recv transport))
              (recv [_ timeout] (.recv transport timeout))
              (send [_ response]
                (when on-before-send
                  (on-before-send response))
                (.send transport response))))))
