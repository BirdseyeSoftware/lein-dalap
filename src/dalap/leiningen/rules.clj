(ns dalap.leiningen.rules
  (:require [dalap.rules]))

;; Selectors ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn has-meta? [meta-tag]
  (fn -has-meta [form]
    (contains? (meta form) meta-tag)))

(defn clj-form-only? [form]
  (and (list? form)
       (#{'defmacro 'comment} (first form))))

(defn require-macro? [form]
  (and (seq? form)
       (= (first form) :require)
       ((has-meta? :cljs-macro) form)))

(defn cljs-form-only? [form]
  (and (list? form)
       (or (= (first form) :dalap-cljs-only)
           (= (first form) :dalap-cljs-only-splat))))

;; Transformers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn replace-with-meta [meta-tag]
  (fn -replace-with-meta [form w_]
    (let [replacement ((comp meta-tag meta) form)]
        (if (and (seq? replacement)
                 (= (first replacement) 'quote))
          (second replacement)
          replacement))))

(def drop-form (constantly :dalap/drop-form))

(defn change-to-require-macro [form w_]
  (concat [:require-macro] (rest form)))

(defn replace-with-cljs [form w_]
  (cond
    (= (first form) :dalap-cljs-only) (w_ (cons 'do (rest form)))
    (= (first form) :dalap-cljs-only-splat) (w_ (second form))
    :else (w_ form)))

;; Constant values ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce clj-forms-to-drop #{'defmacro 'comment})

(defonce cljs-core-rules
  [;;
   ;; replace forms with the one specified in the ^{:cljs}
   (dalap.rules/when (has-meta? :cljs))
   (dalap.rules/transform (replace-with-meta :cljs))
   ;;
   ;;
   (dalap.rules/when cljs-form-only?)
   (dalap.rules/transform replace-with-cljs)
   ;;
   ;; drop all forms tagged with ^{:clj} (clojure only)
   (dalap.rules/when (has-meta? :clj))
   (dalap.rules/transform drop-form)
   ;;
   ;; drop forms that only work on clojure (macro & comments)
   (dalap.rules/when clj-form-only?)
   (dalap.rules/transform drop-form)
   ;;
   ;; ^:cljs-macro replaces :require to :require-macros
   (dalap.rules/when require-macro?)
   (dalap.rules/transform change-to-require-macro)
   ])

(defonce -cljs-java-non-prefix-types-rules
  ['Object 'default
   'String 'string
   'Long 'number
   'Integer 'number
   'Float 'number
   'Double 'number
   'Boolean 'boolean
   'Exception 'js/Error
  ])

(defonce cljs-java-non-prefix-types-rules
  (mapcat
   (fn [[k _]]
     [k
      (dalap.rules/transform
       (fn [form w]
         (println
          (str (:log-prefix w)
               "WARNING: Found possible Java class `"
               form
               "'. Leaving it as is.\n"
               "         You'll need to add the package name to `"
               form
               "' for automatic translation to Javascript.\n"
               "         (e.g java.lang." form ")"))
         (println "")
         form))])
   (partition 2 -cljs-java-non-prefix-types-rules)))

(defonce cljs-java-prefix-types-rules
  (mapcat
   (fn [[k v]]
     [(symbol (format "java.lang.%s" (name k))) v])
   (partition 2 -cljs-java-non-prefix-types-rules)))

(defonce -cljs-clj-non-prefix-types-rules
  ['Atom 'cljs.core.Atom
   'Named 'cljs.core.Named
   'PersistentVector 'cljs.core.PersistentVector
  ])

(defonce cljs-clj-non-prefix-types-rules
  (mapcat
   (fn [[k _]]
     [k
      (dalap.rules/transform
       (fn [form w]
         (println
          (str (:log-prefix w)
               "WARNING: Found possible Clojure type `"
               form
               "'. Leaving it as is.\n"
               "         You'll need to add the package prefix for `"
               form
               "' for automatic translation to Clojurescript.\n"
               "         (e.g clojure.lang." form ")"))
         (println "")
         form))])
   (partition 2 -cljs-clj-non-prefix-types-rules)))

(defonce cljs-clj-prefix-types-rules
  (mapcat
   (fn [[k v]]
     [(symbol (format "clojure.lang.%s" (name k))) v])
   (partition 2 -cljs-clj-non-prefix-types-rules)))

(defonce cljs-default-rules
  (concat cljs-core-rules
          cljs-java-non-prefix-types-rules
          cljs-java-prefix-types-rules
          cljs-clj-non-prefix-types-rules
          cljs-clj-prefix-types-rules))

;; Monoid instance of rules ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; `rules` can either be a vector or a function
;;
(def -mzero [])
(defn -mappend
  ([tr1 tr2]
     (cond
       ;; give precedence to the rules that are specified
       ;; at the end (e.g stack them)
       (and (sequential? tr1) (fn? tr2))  (tr2 tr1)
       (and (sequential? tr1) (sequential? tr2)) (concat tr2 tr1)
       (and (fn? tr1) (fn? tr2))   (comp tr2 tr1)
       (and (fn? tr1) (sequential? tr2))  (tr1 tr2)
       :else
       (throw (Exception.
               (str "-mappend invalid argument types"
                    (type tr1) " => " (str tr1) "\n"
                    (type tr2) " => " (str tr2))))))
  ([tr1 tr2 & rest]
     (reduce -mappend
             (-mappend tr1 tr2)
             rest)))
