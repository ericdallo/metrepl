(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.build.api :as b]))

(def lib 'com.github.ericdallo/metrepl)
(def version (string/trim (slurp (io/resource "METREPL_VERSION"))))
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" lib version))
(def basis {:project "deps.edn"})

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :basis (b/create-basis basis)
                :version version})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install [opts]
  (jar opts)
  (println "Installing to local mvn repo...")
  (b/install {:basis (b/create-basis (update basis :aliases concat [:debug]))
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))
