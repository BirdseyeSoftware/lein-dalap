;; This file was generated with lein-dalap from
;;
;; src/clj/hello_world/core.clj @ Thu Dec 13 00:58:45 UTC 2012
;;
(ns hello-world.core (:refer-clojure :exclude [println]))
(defn println [& args] (.log js/console (apply str args)))
(defn -main [& args] (println "Hello, World!"))