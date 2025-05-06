(ns metrepl.exporters.otlp
  (:require
   [clojure.string :as string]
   [metrepl.format :as format])
  (:import
   [io.opentelemetry.api.common AttributeKey]
   [io.opentelemetry.api.logs Severity]
   [io.opentelemetry.sdk.autoconfigure AutoConfiguredOpenTelemetrySdk]
   [io.opentelemetry.sdk.logs SdkLoggerProvider]
   [java.util.function Function]))

(set! *warn-on-reflection* true)

(defonce otlp-logger-provider* (atom nil))

(defn ^:private setup-logger [otlp-config]
  (reset! otlp-logger-provider*
          (-> (AutoConfiguredOpenTelemetrySdk/builder)
              (.setResultAsGlobal)
              (.addPropertiesCustomizer (reify Function (apply [_ _]
                                                          otlp-config)))
              (.build)
              .getOpenTelemetrySdk
              .getSdkLoggerProvider)))

(defn ^:private ->severity [level]
  (case level
    :debug Severity/DEBUG
    :info Severity/INFO
    :warn Severity/WARN
    :error Severity/ERROR
    Severity/INFO))

(defn ^:private ->raw-value [value]
  (cond
    (keyword? value) (string/join "" (drop 1 (str value)))
    (number? value) value
    (boolean? value) value
    :else (str value)))

(defn export! [data _metric-cfg {:keys [config]}]
  (when-not @otlp-logger-provider*
    (setup-logger config))
  (let [log-record-builder (-> (.get ^SdkLoggerProvider @otlp-logger-provider* (str *ns*))
                               (.logRecordBuilder)
                               (.setBody (format/parse-data (:payload data) :json))
                               (.setSeverity (->severity (:level data)))
                               (.setTimestamp (:timestamp data)))]
    (doseq [[field value] (dissoc data :timestamp :level :payload)]
      (.setAttribute log-record-builder (AttributeKey/stringKey (name field)) (->raw-value value)))
    (.emit log-record-builder)))
