(ns cljsbuild-dalap.transform-rules)

(defonce ^:private clj-forms-to-drop #{'defmacro 'comment})

(defonce ^:private cljs-core-transform-rules
  [;;
   ;; replace forms with the one specified in the ^{:cljs}
   (fn [form w] (contains? (meta form) :cljs))
   (fn [form w_] ((comp :cljs meta) form))
   ;;
   ;; drop all forms tagged with ^{:clj} (clojure only)
   (fn [form w] (contains? (meta form) :clj))
   (constantly ::drop)
   ;;
   ;; drop forms that only work on clojure (macro & comments)
   (fn [form w] (and (list? form) (clj-forms-to-drop (first form))))
   (constantly ::drop)
  ])

(defonce ^:private cljs-java-non-prefix-types-transform-rules
  ['Object 'default
   'String 'string
   'Long 'number
   'Integer 'number
   'Float 'number
   'Double 'number
   'Boolean 'boolean
   'Exception 'js/Error
  ])

(defonce ^:private cljs-java-prefix-types-transform-rules
  (mapcat
   (fn [[k v]]
     [(symbol (format "java.lang.%s" (name k))) v])
   (partition 2 cljs-java-non-prefix-types-transform-rules)))

(defonce ^:private cljs-clj-non-prefix-types-transform-rules
  ['Atom 'cljs.core.Atom
   'PersistentVector 'cljs.core.PersistentVector
  ])

(defonce ^:private cljs-clj-prefix-types-transform-rules
  (mapcat
   (fn [[k v]]
     [(symbol (format "clojure.lang.%s" (name k))) v])
   (partition 2 cljs-clj-non-prefix-types-transform-rules)))

(defonce cljs-default-transform-rules
  (concat cljs-core-transform-rules
          cljs-java-non-prefix-types-transform-rules
          cljs-java-prefix-types-transform-rules
          cljs-clj-non-prefix-types-transform-rules
          cljs-clj-prefix-types-transform-rules))
