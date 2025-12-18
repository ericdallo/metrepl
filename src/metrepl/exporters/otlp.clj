(ns metrepl.exporters.otlp
  (:require
   [clojure.string :as string]
   [metrepl.format :as format])
  (:import
   [io.opentelemetry.api.common AttributeKey Attributes]
   [io.opentelemetry.api.logs Severity]
   [io.opentelemetry.sdk OpenTelemetrySdk]
   [io.opentelemetry.sdk.autoconfigure AutoConfiguredOpenTelemetrySdk]
   [java.util.function Function]))

(set! *warn-on-reflection* true)

(defonce ^:private otlp-provider* (atom nil))

(defn ^:private setup-sdk [otel-config]
  (reset! otlp-provider*
          (-> (AutoConfiguredOpenTelemetrySdk/builder)
              (.addPropertiesCustomizer ^Function (constantly otel-config))
              (.build)
              .getOpenTelemetrySdk)))

(defn ^:private ->severity [level]
  (case level
    :debug Severity/DEBUG
    :info Severity/INFO
    :warn Severity/WARN
    :error Severity/ERROR
    Severity/INFO))

(defn ^:private ->attributes
  [m]
  (let [builder (Attributes/builder)]
    (doseq [[k v] m
            :let [value (cond
                          (keyword? v) (string/join "" (drop 1 (str v)))
                          (number? v) v
                          (boolean? v) v
                          :else (str v))]]
      (.put builder (AttributeKey/stringKey (name k)) value))
    (.build builder)))

(defn export!
  [{:keys [payload level timestamp] :as data {:keys [op]} :payload} _metric-cfg {:keys [config]}]
  (when-not @otlp-provider*
    (setup-sdk config))
  (let [^OpenTelemetrySdk sdk @otlp-provider*
        base-atrributes (dissoc data :timestamp :level :payload)
        log-attributes (->attributes base-atrributes)
        log-record-builder (-> (.getSdkLoggerProvider sdk)
                               (.get (str *ns*))
                               (.logRecordBuilder)
                               (.setBody (format/parse-data payload :json))
                               (.setSeverity (->severity level))
                               (.setTimestamp timestamp)
                               (.setAllAttributes log-attributes))
        long-counter (-> (.getSdkMeterProvider sdk)
                         (.get (str *ns*))
                         (.counterBuilder "metrepl.events.total")
                         (.setUnit "events")
                         (.setDescription "Total number of REPL events")
                         (.build))
        metric-attributes (->attributes (cond-> base-atrributes op (assoc :operation op)))]
    (.emit log-record-builder)
    (.add long-counter 1 metric-attributes)))
