(ns leiningen.dalap-cljsbuild
  (:require [dalap-cljsbuild
             [transform :refer [transform-to-cljs-file]]
             [transform-rules :refer
              [-mzero-transform-rule
               -mappend-transform-rules
               cljs-default-transform-rules]]]
            [robert.hooke]
            [leiningen.cljsbuild]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; parsing transform-rules from cljsbuild's build maps
;;

(defn -normalize-input-path-dalap-options [options-map-or-output-path]
  (cond
    (string? options-map-or-output-path) {:output options-map-or-output-path}
    (map? options-map-or-output-path) options-map-or-output-path))

(defn -get-output-path [options-map-or-output-path]
  (cond
    (map? options-map-or-output-path) (:output options-map-or-output-path)
    (string? options-map-or-output-path) options-map-or-output-path))

(defn -parse-user-transform-rules
  ([build-map]
     (-parse-user-transform-rules build-map -mzero-transform-rule))
  ([build-map cljsbuild-transform-rules]
     (let [top-level-transform-rules (get-in build-map
                                             [:dalap :transform-rules]
                                             -mzero-transform-rule)]
       (apply
        merge
        (for [[input-path options-or-path] (get-in build-map
                                                   [:dalap :paths]
                                                   -mzero-transform-rule)]
          (let [options (-normalize-input-path-dalap-options
                         options-or-path)
                output-path (:output options)
                path-specific-transform-rules (get options
                                                   :transform-rules
                                                   -mzero-transform-rule)]
            ;; { input-path => transform-rules }
            { [input-path output-path]
             (-mappend-transform-rules cljs-default-transform-rules
                                       cljsbuild-transform-rules
                                       top-level-transform-rules
                                       path-specific-transform-rules)}))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn transform-clj-files-to-cljs-files
  [run-compiler-fn
   project
   {:keys [builds] :as opts}
   build-ids watch?]
  (let [cljsbuild-transform-rules (get-in project
                                          [:cljsbuild :dalap :transform-rules]
                                          -mzero-transform-rule)]
    ;;
    (doseq [build builds]
      (let [user-transform-rules (-parse-user-transform-rules
                                  build
                                  cljsbuild-transform-rules)
            paths-to-transform (get-in build [:dalap :paths])]
        ;;
        (doseq [[input-path output-path] paths-to-transform]
          (let [output-path (-get-output-path output-path)]
            (println
             (str "[build: " (:id build) "]")
             "transforming" input-path "to" output-path)
            (spit output-path
                  (transform-to-cljs-file input-path
                                          (get user-transform-rules
                                               [input-path output-path]))))))))
  (run-compiler-fn project opts build-ids watch?))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn activate []
  (robert.hooke/add-hook #'leiningen.cljsbuild/run-compiler
                         #'transform-clj-files-to-cljs-files))
