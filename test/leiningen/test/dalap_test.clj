(ns leiningen.test.dalap-test
  (:require [clojure.test :refer :all]
            [fs.core :as fs]
            [dalap.leiningen.configuration
             :refer [read-user-configuration]]
            [leiningen.dalap
             :refer [dalap-compile]])
  (:import [java.io FileNotFoundException]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn assert-file-present [path]
  (is (fs/exists? path)
      (str "`" path "' should exists in the filesystem")))

(defmacro run-transformations! [dalap-rules-form]
  `(do
     (spit "test/fixtures/dalap_rules.clj" (pr-str '~dalap-rules-form))
     (dalap-compile
      {}
      (read-user-configuration {:dalap-rules "test/fixtures/dalap_rules.clj"
                                :dependencies []})
      false)
     (fs/delete "test/fixtures/dalap_rules.clj")))

(deftest test-reads-source-file
  (is (thrown? Exception
               (run-transformations!
                {["test/fixtures/non_existing.clj" "test/fixtures/out.cljs"]
                 []}))))

(deftest test-generates-destination-file
  (run-transformations!
   {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
    []})
  (assert-file-present "test/fixtures/out.cljs")
  (fs/delete "test/fixtures/file.cljs"))

(deftest test-transformations-being-executed
  (run-transformations!
   {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
    ['ns 'namespace]})
  (is (= (-> (slurp "test/fixtures/file.clj")
             read-string
             first)
         'ns))
  (is (= (-> (slurp "test/fixtures/out.cljs")
             read-string
             first)
         'namespace))
  (fs/delete "test/fixtures/out.cljs"))

(deftest test-nil-forms-are-not-dropped
  (run-transformations!
   {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
    []})
  (is (re-find #" nil" (slurp "test/fixtures/out.cljs"))))

(deftest test-cljs-macro
  (run-transformations!
   {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
    []})
  (is (= (-> (slurp "test/fixtures/file.clj")
             read-string
             (nth 3)
             first)
         :require))
  (is (= (-> (slurp "test/fixtures/out.cljs")
             read-string
             (nth 3)
             first)
         :require-macro))
  (fs/delete "test/fixtures/out.cljs"))

(deftest test-cljs-splat-ignore-macro
  (run-transformations!
   {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
    []})
  (is (->> (slurp "test/fixtures/file.clj")
          (re-find #":cljs")))
  (is (->> (slurp "test/fixtures/out.cljs")
           (re-find #"\[cljs.only"))))


(deftest test-cljs-do-ignore-macro
  (run-transformations!
   {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
    []})
  (is (->> (slurp "test/fixtures/file.clj")
          (re-find #":cljs-do")))
  (is (->> (slurp "test/fixtures/out.cljs")
           (re-find #"\(do \(\.log"))))

(deftest test-dalap-scope-is-in-configuration-file
  (is (thrown?
       Exception
       (run-transformations!
        {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
         [(dalap/when #(= (count (str %)) 2))
          (dalap/transform
           (fn [form w]
             (when (#{'ns 'is} form)
               (throw (Exception. (str "form `" form "' received"))))))]}))
      "an exception should have been thrown"))

(deftest test-exception-with-msg-is-thrown-when-dalap-rules-not-found
  (is (thrown-with-msg?
        FileNotFoundException
        #"In order to run lein-dalap"
        (read-user-configuration {:dalap-rules "test/fixtures/non_existing_file.clj"}))))
