(ns metrepl.config
  "Waterfall of ways to get metrepl config, deep merging from top to bottom:

  1. base: fixed config var `metrepl.config/initial-config`.
  2. classpath: searching for a `metrepl.exports/config.edn` file in the current classpath.
  3. env var: searching for a `METREPL_CONFIG` env var which should contains a valid edn config.
  4. config-file: searching from a local `.metrepl.edn` file.
  5. dynamic-var: the dynamic value in `metrepl.config/*config*`."
  (:refer-clojure :exclude [get error-handler])
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(def initial-config
  "Check `docs/all-metrics.edn` and `docs/all-configs.edn` for
   all available options."
  {:metrics {:event/first-op-requested {:level :info}
             :event/op-requested {:level :info}
             :event/op-completed {:level :info}
             :event/tests-executed {:level :info}
             :event/test-passed {:level :info}
             :event/test-failed {:level :info}
             :event/test-errored {:level :info}}
   :exporters {:stdout {:enabled? false
                        :format :summary}
               :file {:enabled? false
                      :path "./metrepl.txt"
                      :format :summary}
               :otlp {:enabled? false
                      :config {"otel.service.name" "metrepl"
                               "otel.metrics.exporter" "none"
                               "otel.traces.exporter" "none"}}}
   :error-handler {:stdout {:enabled? false}
                   :file {:enabled? false
                          :path "./metrepl-error.txt"}}})

(defn safe-read-edn-string [raw-string]
  (try
    (->> raw-string
         (edn/read-string {:readers {'re re-pattern}}))
    (catch Exception _
      nil)))

(def ^:dynamic *config*
  "Change this dynamic var to use custom metrepl config"
  {})

(defn ^:private config-from-classpath* []
  (io/resource "metrepl.exports/config.edn"))

(def ^:private config-from-classpath (memoize config-from-classpath*))

(defn ^:private config-from-envvar* []
  (some-> (System/getenv "METREPL_CONFIG")
          (safe-read-edn-string)))

(def ^:private config-from-envvar (memoize config-from-envvar*))

(defn ^:private config-from-file* []
  (let [config-file (io/file ".metrepl.edn")]
    (when (.exists config-file)
      (safe-read-edn-string (slurp config-file)))))

(def ^:private config-from-file (memoize config-from-file*))

(defn deep-merge [& maps]
  (apply merge-with (fn [& args]
                      (if (every? #(or (map? %) (nil? %)) args)
                        (apply deep-merge args)
                        (last args)))
         maps))

(defn all []
  (deep-merge initial-config
              (config-from-classpath)
              (config-from-envvar)
              (config-from-file)
              *config*))

(defn metric [metric]
  (get-in (all) [:metrics metric]))

(defn exporters []
  (get-in (all) [:exporters]))

(defn error-handler []
  (get-in (all) [:error-handler]))
