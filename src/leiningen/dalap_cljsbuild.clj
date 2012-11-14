(ns leiningen.dalap-cljsbuild
  ;; TODO: make sure there is no race condition
  (:require [dalap-cljsbuild
             [transform :refer [transform-to-cljs-file]]
             [transform-rules :refer
              [-mzero-transform-rule
               -mappend-transform-rules
               cljs-default-transform-rules]]]
            [fs.core :as fs]
            [watchtower.core :as wt]
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

(defn -dalap-compile-file
  [build input-path output-path user-transform-rules]
  ;;(fs/mkdirs output-path)
  (println (str "[build: " (:id build) "]")
           "transforming" input-path "to" output-path)
  (spit output-path
        (transform-to-cljs-file input-path
                                (get user-transform-rules
                                     [input-path output-path]))))

(defn -mk-dalap-file-compiler
  [build input-path output-path user-transform-rules]
  (fn [& args]
    (-dalap-compile-file build input-path output-path user-transform-rules)))

(defn dalap-compile [project builds watch?]
  (let [cljsbuild-transform-rules (get-in project
                                          [:cljsbuild :dalap :transform-rules]
                                          -mzero-transform-rule)]
    ;;
    (doseq [build builds]
      (let [user-transform-rules
            (-parse-user-transform-rules build
                                         cljsbuild-transform-rules)
            paths-to-transform (get-in build [:dalap :paths])]
        ;;
        (doseq [[input-path output-path] paths-to-transform]
          (let [output-path (:output
                             (-normalize-input-path-dalap-options output-path))
                watcher-id [(:id build) input-path]]
            (-dalap-compile-file
             build input-path output-path user-transform-rules)
            (when watch?
              (wt/watcher
               [input-path]
               (wt/file-filter (constantly true))
               (wt/rate 50)
               (wt/on-change
                (-mk-dalap-file-compiler
                 build input-path output-path user-transform-rules))))))))))

;; using ritalin
;;
;; TODO: Watch project file to redo the watcher-map
;; (def watcher-map (atom {}))
;;
;; (defn -dalap-compile-file [build input-path output-path user-transform-rules]
;;   (fs/mkdirs output-path)
;;   (println (str "[build: " (:id build) "]")
;;            "transforming" input-path "to" output-path)
;;   (spit output-path
;;         (transform-to-cljs-file input-path
;;                                 (get user-transform-rules
;;                                      [input-path output-path]))))

;; (defn -mk-dalap-file-compiler
;;   [build input-path output-path user-transform-rules]
;;   (fn [watcher-id_ root-path_ modified-file-path_])
;;     (-dalap-compile-file build input-path output-path user-transform-rules))

;; (defn dalap-compile [project builds watch?]
;;   (let [cljsbuild-transform-rules (get-in project
;;                                           [:cljsbuild :dalap :transform-rules]
;;                                           -mzero-transform-rule)]
;;     ;;
;;     (doseq [build builds]
;;       (let [user-transform-rules
;;             (-parse-user-transform-rules build
;;                                          cljsbuild-transform-rules)
;;             paths-to-transform (get-in build [:dalap :paths])]
;;         ;;
;;         (doseq [[input-path output-path] paths-to-transform]
;;           (let [output-path (:output
;;                              (-normalize-input-path-dalap-options output-path))
;;                 watcher-id [(:id build) input-path]]
;;            (-dalap-compile-file
;;                build input-path output-path user-transform-rules)
;;            (when watch?
;;              (swap! watcher-map
;;                      ritalin/add-watch
;;                      watcher-id [:modified] input-path
;;                      (-mk-dalap-file-compiler
;;                       build input-path output-path user-transform-rules)))))))))

(defn- run-dalap-transformation
  [run-compiler-fn project {:keys [builds] :as options} given-build-ids watch?]
  (let [builds (if (empty? given-build-ids)
                 builds
                 (filter #(some #{{:id %}} given-build-ids) builds))]
    (dalap-compile project builds watch?)
    (run-compiler-fn project options given-build-ids watch?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn activate []
  (robert.hooke/add-hook #'leiningen.cljsbuild/run-compiler
                         #'run-dalap-transformation))
