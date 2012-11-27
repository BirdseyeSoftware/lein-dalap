(defproject lein-dalap "0.1.0-SNAPSHOT"
  :description "Provides clojure to clojurescript transformation using dalap"
  :license {:name "EPL"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[dalap "0.0.1-SNAPSHOT"]
                 [fs "1.1.2"]
                 [watchtower "0.1.1"]
                 [lein-cljsbuild "0.2.9"]]
  :eval-in-leiningen true)