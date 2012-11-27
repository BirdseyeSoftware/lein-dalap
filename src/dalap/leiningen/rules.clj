(ns dalap.leiningen.rules
  (:require [dalap.rules]))

;; Selectors ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn has-meta? [meta-tag]
  (fn -has-meta [form]
    (contains? (meta form) meta-tag)))

(defn clj-form-only? [form]
  (and (list? form)
       (#{'defmacro 'comment} (first form))))

;; Transformers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn replace-with-meta [meta-tag]
  (fn -replace-with-meta [form w_]
    (let [replacement ((comp meta-tag meta) form)]
        (if (and (seq? replacement)
                 (= (first replacement) 'quote))
          (second replacement)
          replacement))))

(def drop-form (constantly :dalap/drop-form))


;; Constant values ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce clj-forms-to-drop #{'defmacro 'comment})

(defonce cljs-core-rules
  [;;
   ;; replace forms with the one specified in the ^{:cljs}
   (dalap.rules/when (has-meta? :cljs))
   (dalap.rules/transform (replace-with-meta :cljs))
   ;;
   ;; drop all forms tagged with ^{:clj} (clojure only)
   (dalap.rules/when (has-meta? :clj))
   (dalap.rules/transform drop-form)
   ;;
   ;; drop forms that only work on clojure (macro & comments)
   (dalap.rules/when clj-form-only?)
   (dalap.rules/transform drop-form)
   ])

(defonce cljs-java-non-prefix-types-rules
  ['Object 'default
   'String 'string
   'Long 'number
   'Integer 'number
   'Float 'number
   'Double 'number
   'Boolean 'boolean
   'Exception 'js/Error
  ])

(defonce cljs-java-prefix-types-rules
  (mapcat
   (fn [[k v]]
     [(symbol (format "java.lang.%s" (name k))) v])
   (partition 2 cljs-java-non-prefix-types-rules)))

(defonce cljs-clj-non-prefix-types-rules
  ['Atom 'cljs.core.Atom
   'Named 'cljs.core.Named
   'PersistentVector 'cljs.core.PersistentVector
  ])

(defonce cljs-clj-prefix-types-rules
  (mapcat
   (fn [[k v]]
     [(symbol (format "clojure.lang.%s" (name k))) v])
   (partition 2 cljs-clj-non-prefix-types-rules)))

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
               (str "-mappend-rules invalid argument types"
                    (type tr1) " => " (str tr1) "\n"
                    (type tr2) " => " (str tr2))))))
  ([tr1 tr2 & rest]
     (reduce -mappend
             (-mappend tr1 tr2)
             rest)))
