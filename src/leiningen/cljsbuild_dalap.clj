(ns leiningen.cljsbuild-dalap
  (:require [cljsbuild-dalap
             [transform :refer [transform-to-cljs-file]]
             [transform-rules :refer [cljs-default-transform-rules]]]
            [robert.hooke]
            [leiningen.cljsbuild]
            ))


(defn -mappend-transform-rules
  ([tr1 tr2]
     (cond
       ;; give precedence to the transform-rules that are specified
       ;; at the end (e.g stack them)
       (and (vector? tr1) (fn? tr2))     (tr2 tr1)
       (and (vector? tr1) (vector? tr2)) (vec (concat tr2 tr1))
       (and (fn? tr1) (fn? tr2))         (comp tr2 tr1)
       (and (fn? tr1) (vector? tr2))     (tr1 tr2)
       :else
       (throw (Exception.
               (str "-mappend-transform-rules invalid argument types"
                    (type tr1) " => " (str tr1) "\n"
                    (type tr2) " => " (str tr2))))))
  ([tr1 tr2 & rest]
     (reduce -mappend-transform-rules
             (-mappend-transform-rules tr1 tr2)
             rest)))

(defn -normalize-input-path-dalap-options [options-map-or-output-file]
  (cond
    (string? options-map-or-output-file) {:output options-map-or-output-file}
    (map? options-map-or-output-file) options-map-or-output-file))

(defn -parse-user-transform-rules [build-map]
  (let [top-level-transform-rules (get-in build-map
                                          [:dalap :transform-rules] [])]
    (apply merge
           (for [[input-file
                  options-or-output-file] (get-in build-map [:dalap :paths] [])]
             (let [options (-normalize-input-path-dalap-options options-or-output-file)
                   path-specific-transform-rules (get options :transform-rules [])]
               ;; { input-file => transform-rules }
               {input-file
                (-mappend-transform-rules (vec cljs-default-transform-rules)
                                          top-level-transform-rules
                                          path-specific-transform-rules)})))))



(defn transform-clj-files-to-cljs-files
  [run-compiler-fn
   project
   {:keys [builds] :as opts}
   build-ids watch?]

  (doseq [build builds]
    (let [files-to-transform (get-in build [:dalap :transform-files])]
      (doseq [[input-file output-file] files-to-transform]
        (println (str "[" (:id build) "]")
                 "transforming" input-file "to" output-file)
        (spit output-file (transform-to-cljs-file input-file)))))
  (run-compiler-fn project opts build-ids watch?))

(defn activate []
  (robert.hooke/add-hook #'leiningen.cljsbuild/run-compiler
                         #'transform-clj-files-to-cljs-files))

 (defn cljsbuild-dalap
   "I don't do a lot."
   [project & args]
   (println "Hi!"))