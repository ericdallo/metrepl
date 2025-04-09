(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.build.api :as b]))

(def lib 'dev.ericdallo/metrepl)
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
  (b/copy-file {:src (str class-dir "/META-INF/maven/" lib "/pom.xml") :target "pom.xml"})
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

(defn deploy-clojars [opts]
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   (merge {:installer :remote
           :artifact jar-file
           :pom-file "pom.xml"}
          opts)))
