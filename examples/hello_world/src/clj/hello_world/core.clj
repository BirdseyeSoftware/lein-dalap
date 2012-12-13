(ns hello-world.core
  (:refer-clojure :exclude [println]))

(defn println [& args]
  ^{:cljs
    '(.log js/console (apply str args))}
  (apply clojure.core/println args))

(defn -main [& args]
  (println "Hello, World!"))
