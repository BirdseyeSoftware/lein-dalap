(ns leiningen.cljsbuild-dalap
  (:require [clojure [pprint :refer [pprint]]]
            ;;[clojure.tools.namespace :refer [find-clojure-sources-in-dir]]
            [clojure.java.io :refer [as-file]]
            [cljsbuild-dalap
             [transform :refer [transform-to-cljs-file]]
             [transform-rules :refer [cljs-default-transform-rules]]]
            [robert.hooke]
            [leiningen.cljsbuild]
            ))


(defn run-compiler [run-compiler-
                    {:keys [transform-files] :as project}
                    {:keys [builds] :as opts}
                    build-ids watch?]

  ;; (println "HEY JO! you are about to compile the source code")
  ;; (println "=====")
  ;; (pprint opts)
  ;; (println "=====")
  ;; (pprint build-ids)
  ;; (println "=====")
  ;; (pprint watch?)
  ;; (println "=====")
  ;;(pprint (mapcat (comp find-clojure-sources-in-dir :source-path) builds))
  (doseq [[input-file output-file] transform-files]
    (println "transforming" input-file "to" output-file)
    (spit output-file (transform-to-cljs-file input-file)))
  (run-compiler- project opts build-ids watch?))

(defn activate []
  (robert.hooke/add-hook #'leiningen.cljsbuild/run-compiler #'run-compiler))

 (defn cljsbuild-dalap
   "I don't do a lot."
   [project & args]
   (println "Hi!"))