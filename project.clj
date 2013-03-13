(defproject com.birdseye-sw/lein-dalap "0.1.1-SNAPSHOT"
  :description "Provides clojure to clojurescript transformation using dalap"
  :url "http://birdseye-sw.com/oss/lein-dalap/"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.birdseye-sw/dalap "0.1.0"]
                 [fs "1.1.2"]
                 [watchtower "0.1.1"]
                 [lein-cljsbuild "0.3.0"]]
  :eval-in-leiningen true)
