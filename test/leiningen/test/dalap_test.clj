(ns leiningen.test.dalap-test
  (:require [clojure.test :refer :all]
            [fs.core :as fs]
            [dalap.leiningen.configuration
             :refer [read-user-configuration]]
            [leiningen.dalap
             :refer [dalap-compile]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn assert-file-present [path]
  (is (fs/exists? path)
      (str "`" path "' should exists in the filesystem")))

(defmacro gen-rule-file [project dalap-rules-form]
  `(do
     (spit ~(:dalap-rules project) (pr-str '~dalap-rules-form))
     (dalap-compile
      '~project
      (read-user-configuration '~project)
      false)
     (fs/delete ~(:dalap-rules project))))

(deftest reads-source-file
  (is (thrown? Exception
               (gen-rule-file
                ;; project
                {:dalap-rules "test/fixtures/dalap_rules.clj"}
                {["test/fixtures/non_existing.clj" "test/fixtures/out.cljs"]
                 []}))))

(deftest generates-destination-file
  (gen-rule-file
   ;; project
   {:dalap-rules "test/fixtures/dalap_rules.clj"}
   {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
    []})
  (assert-file-present "test/fixtures/out.cljs")
  (fs/delete "test/fixtures/file.cljs"))

(deftest transformations-being-executed
  (gen-rule-file
   ;; project
   {:dalap-rules "test/fixtures/dalap_rules.clj"}
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

(deftest dalap-scope-is-in-configuration-file
  (is (thrown?
       Exception
       (gen-rule-file
        ;; project
        {:dalap-rules "test/fixtures/dalap_rules.clj"}
        {["test/fixtures/file.clj" "test/fixtures/out.cljs"]
         [(dalap/when #(= (count (str %)) 2))
          (dalap/transform
           (fn [form w]
             (when (#{'ns 'is} form)
               (throw (Exception. (str "form `" form "' received"))))))]}))
      "an exception should have been thrown"))