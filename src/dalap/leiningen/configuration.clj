(ns dalap.leiningen.configuration
  (:require [leiningen.core.eval :refer [eval-in-project]]
            [fs.core :as fs])
  (:import [java.io FileNotFoundException]))

(defn read-user-configuration
  ([project]
     (read-user-configuration
      project
      (get project :dalap-rules "./dalap_rules.clj")))
  ([project path]
     (when (not (fs/exists? path))
       (throw (FileNotFoundException. "

\u001b[31;1mERROR:\u001b[0m Can't find rules file.

In order to run lein-dalap you must have a `dalap_rules.clj` file at
the root of your project.

For more info, please go to:

http://birdseyesoftware.github.com/lein-dalap.docs/articles/getting_started.html#specifying_which_files_you_want_to_transform_to_cljs

")))
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
      '(require '[dalap.rules :as dalap]
                '[dalap.leiningen.rules
                  :refer [has-meta? drop-form clj-form-only?]]))))
