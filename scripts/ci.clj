(ns ci
  (:require
   [babashka.tasks :refer [shell]]
   [clojure.string :as string]))

(defn ^:private replace-in-file [file regex content]
  (as-> (slurp file) $
    (string/replace $ regex content)
    (spit file $)))

(defn ^:private add-changelog-entry [tag comment]
  (replace-in-file "CHANGELOG.md"
                   #"## Unreleased"
                   (if comment
                     (format "## Unreleased\n\n## %s\n\n- %s" tag comment)
                     (format "## Unreleased\n\n## %s" tag))))

(defn jar []
  (shell "clojure -T:build jar"))

(defn install []
  (shell "clojure -T:build install"))

(defn tag [& [tag]]
  (shell "git fetch origin")
  (shell "git pull origin HEAD")
  (spit "resources/METREPL_VERSION" tag)
  (add-changelog-entry tag nil)
  (jar)
  (shell "git add pom.xml resources/METREPL_VERSION CHANGELOG.md")
  (shell (format "git commit -m \"Release: %s\"" tag))
  (shell (str "git tag " tag))
  (shell "git push origin HEAD")
  (shell "git push origin --tags"))

(defn deploy-clojars []
  (shell "clojure -T:build deploy-clojars"))

(defn tests []
  (shell "clojure -Mtest"))
