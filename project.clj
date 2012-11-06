(defproject lein-dalap-cljsbuild "0.1.0-SNAPSHOT"
  :description "Provides clojure to clojurescript transformation using dalap."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[lein-cljsbuild "0.2.9"]
                 [dalap "0.0.1-SNAPSHOT"]]
  :plugins [[lein-swank "1.4.4"]]
  :eval-in-leiningen true)