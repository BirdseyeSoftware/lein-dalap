(defproject hello_world "0.1.0-SNAPSHOT"
  :description "Example application for lein-dalap usage"
  :url ""
  :main hello-world.core
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.4.0"]]

  :plugins [[lein-cljsbuild "0.2.9"]
            [com.birdseye-sw/lein-dalap "0.1.0"]]

  :hooks [leiningen.dalap]

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-path "src/cljs"
     :compiler
     {:optimizations :whitespace
      :pretty-print true
      :output-to "resources/js/hello_world_dev.js"}}]})
