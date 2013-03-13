(ns dalap.leiningen.configuration
  (:require [leiningen.core.eval :refer [eval-in-project]]))

(defn read-user-configuration
  ([project]
     (read-user-configuration
      project
      (get project :dalap-rules "./dalap_rules.clj")))
  ([project path]
     (eval-in-project
      ;; project instance
      (-> project
          (update-in [:dependencies]
                     conj
                     ['com.birdseye-sw/lein-dalap "0.1.0"])
          (assoc :eval-in :classloader))
      ;; body
      `(eval (read-string (slurp ~path)))
      ;; requires for given body
      '(require '[dalap.rules :as dalap ;;:refer [when transfrom]
                  ]
                '[dalap.leiningen.rules
                  :refer [has-meta? drop-form clj-form-only?]]))))
