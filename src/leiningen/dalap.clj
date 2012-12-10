(ns leiningen.dalap
  ;; TODO: make sure there is no race condition
  (:require
   [leiningen.core.main :refer [abort]]
   [fs.core :as fs]
   [watchtower.core :as wt]
   [robert.hooke]
   [leiningen.cljsbuild]
   [dalap.leiningen
    [configuration :refer [read-user-configuration]]
    [transform :refer [transform-to-cljs-file]]]))

;; FILE-SPEC UTILS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- -get-build-id [file-spec]
 (cond
    (= (count file-spec) 2) nil
    (= (count file-spec) 3) (first file-spec)
    :else (throw (Exception. (str "Invalid file-spec " file-spec)))) )

(defn- -get-input-path [file-spec]
  (cond
    (= (count file-spec) 2) (first file-spec)
    (= (count file-spec) 3) (second file-spec)
    :else (throw (Exception. (str "Invalid file-spec " file-spec)))))

(defn- -get-output-path [file-spec]
  (cond
    (= (count file-spec) 2) (second file-spec)
    (= (count file-spec) 3) (nth file-spec 2)
    :else (throw (Exception. (str "Invalid file-spec: " file-spec)))))

;; COMPILE SINGLE FILE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -dalap-compile-file
 ([input-path output-path rules]
    (-dalap-compile-file input-path output-path nil))
  ([input-path output-path rules build-id]
     (if (fs/directory? output-path)
       (fs/mkdirs output-path)
       (fs/mkdirs (fs/parent output-path)))
     ;; (println (str "[build: " (or build-id "none") "]")
     ;;          "Transforming" input-path "to" output-path)
     (println "")
     (println "dalap transforming" input-path "=>" output-path)
     (spit output-path
           (transform-to-cljs-file input-path
                                   rules))))

(defn -mk-dalap-file-compiler
  ([input-path output-path rules-map & [build-id]]
     (fn file-compiler [& args]
       (try
         (-dalap-compile-file input-path output-path rules-map build-id)
         (catch Exception e
           (.printStackTrace e))))))

;; COMPILE ALL FILES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -dalap-compile
  [project rules-map watch? & [build-id]]
  (doseq [[file-spec rules] rules-map]
    (let [input-path (-get-input-path file-spec)
          output-path (-get-output-path file-spec)
          rule-build-id (-get-build-id file-spec)]
      ;; either you are a transpile with both a given build-id
      ;; and a specified one in the rules file, or you are
      ;; one without any build-id at all
      (when (or (and (nil? build-id)
                     (nil? rule-build-id))
                (= rule-build-id build-id))
        (-dalap-compile-file input-path
                             output-path
                             rules
                             rule-build-id)
        (when watch?
          (wt/watcher [input-path]
                      (wt/file-filter (constantly true))
                      (wt/rate 50)
                      (wt/on-change
                       (-mk-dalap-file-compiler input-path output-path rules))))))))

(defn dalap-compile [project rules-map watch? & [builds]]
  ;; always compile files that don't belong to any build
  (-dalap-compile project rules-map watch?)
  ;; then compile files that belong to an specific build
  (doseq [build-id builds]
    (-dalap-compile project rules-map watch? build-id)))

;; PLUGIN TASKS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- once
  "Transpile clojure files to clojurescript"
  [project rules-map]
  (dalap-compile project rules-map false))

(defn- auto
  "Automatically re-transpile when files are modified."
  [project rules-map]
  (dalap-compile project rules-map true)
  (loop [] (Thread/sleep 100) (recur)))

(defn- clean
  "Remove lein-dalap generated files."
  [project rules-map]
  (doseq [[file-spec _] rules-map]
    (fs/delete (-get-output-path file-spec))))

(defn dalap
  "Run dalap cljs transpiler plugin"
  {:help-arglists '([once auto clean])
   :subtasks '[#'once #'auto #'clean]}
  ([project]
     (println "Specify a subtask (once, auto, clean)"))
  ([project subtask & args]
     (let [rules-map (read-user-configuration project)]
       (case subtask
         "once" (once project rules-map)
         "auto" (auto project rules-map)
         "clean" (clean project rules-map)
         (do
           (println "Subtask" (str \" subtask \") "not found")
           (abort 1))))))

;; HOOKS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -cljsbuild-compiler-hook
  [run-compiler-fn project options build-ids watch?]
  (dalap-compile project
                 (read-user-configuration project)
                 watch?
                 build-ids)
  (run-compiler-fn project options build-ids watch?))

(defn activate []
  (robert.hooke/add-hook #'leiningen.cljsbuild/run-compiler
                         #'leiningen.dalap/-cljsbuild-compiler-hook))
