(ns dalap.leiningen.transform
  (:require [clojure.string :as str]
            [dalap.walk]
            [dalap.rules]
            [dalap.leiningen.rules :refer
             [cljs-default-rules -mappend]])
  (:import [clojure.lang LineNumberingPushbackReader]))

(defn visit-clj-form [form w]
  "A modified version of clojure.walk with the ability to drop forms"
  (letfn [(filter-map [f form] (remove #(= % :dalap/drop-form)
                                       (map f form)))]
    (cond
      (list? form) (apply list (filter-map w form))
      (instance? clojure.lang.IMapEntry form) (vec (filter-map w form))
      (seq? form) (doall (filter-map w form))
      (coll? form) (into (empty form) (filter-map w form))
      :else form)))

(defn clj-forms-to-cljs-forms
  "Transform a seq of clojure forms into clojurescript forms"
  ([forms]
     (clj-forms-to-cljs-forms forms cljs-default-rules))
  ([forms rules]
     (dalap.walk/walk forms
                      ((dalap.rules/-gen-rules-decorator rules) visit-clj-form))))


(defn read-forms-from-file
  ;; stolen from:
  ;; https://github.com/jonase/kibit/blob/master/src/kibit/check.clj
  "Gen a lazy sequence of top level forms from a LineNumberingPushbackReader"
  [^LineNumberingPushbackReader r]
  (lazy-seq
   (let [form
         (try
           (read r false ::eof)
           (catch Exception e
             (throw (Exception.
                     (str "Dalap's reader crashed"
                          (.getMessage e)) e))))]
     (when-not (= form ::eof)
       (cons form (read-forms-from-file r))))))

(defn read-clj-forms-from-input [input]
  (read-forms-from-file
   (LineNumberingPushbackReader.
    ;; IMPORTANT:
    ;; this piece of code replaces _top level_ #_(:cljs)
    ;; forms with a (do) so that forms inside it get executed
    ;; only in clojurescript
    ;; e.g:
    ;; #_(:cljs (println "hello world")) => (do (println "hello world"))
    (java.io.StringReader.
     (str/replace (slurp input) #"\#_\(\s*:cljs" "(:dalap-cljs-only")))))

(defn cljs-generated-file-notice [clj-file-path]
  (str ";; This file was generated with lein-dalap from\n;;\n;; "
       clj-file-path " @ " (java.util.Date.)
       "\n;;\n"))

(defn transform-to-cljs-file
  [clj-file-path rules]
  (str (cljs-generated-file-notice clj-file-path)
       (str/join "\n" (clj-forms-to-cljs-forms
                       (read-clj-forms-from-input clj-file-path)
                       (-mappend cljs-default-rules
                                 rules)))))
