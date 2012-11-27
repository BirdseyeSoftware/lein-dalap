(ns dalap.leiningen.configuration
  (:require [leiningen.core.eval :refer [eval-in-project]]))

(defn read-user-configuration
  ([project] (read-user-configuration project "./dalap_rules.clj"))
  ([project path]
     (eval-in-project
      ;; project instance
      (-> project
          (update-in [:dependencies]
                 conj
                 ['lein-dalap-cljsbuild "0.1.0-SNAPSHOT"])
          (assoc :eval-in :classloader))
      ;; body
      `(eval (read-string (slurp ~path)))
      ;; requires for given body
      '(require '[dalap.rules
                  :refer [when transform]]
                '[dalap.leiningen.rules
                  :refer [has-meta? drop-form clj-form-only?]]))))