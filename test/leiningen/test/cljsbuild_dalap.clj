(ns leiningen.test.cljsbuild-dalap
  (:require
     [clojure.test :refer :all]
     [cljsbuild-dalap.transform-rules
      :refer [cljs-default-transform-rules]]
     [leiningen.cljsbuild-dalap
      :refer [-parse-user-transform-rules]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn assert-transform-rules
  [build-map expected-rules]
  (let [result (-parse-user-transform-rules build-map)]
    (is (= result expected-rules)
        (str "Transformation rules don't match expected result\n"
             "\t Expected: " (pr-str expected-rules) "\n"
             "\t Got: " (pr-str result) "\n"))))


(deftest parse-user-transform-rules
  (testing "concats top-level transform rules vector with default ones"
    (assert-transform-rules
     ;; input
     {:dalap
      {:transform-rules ['java.lang.Object 'MyObject]
       :paths {"a" "b"}}}
     ;; output
     {["a" "b"] (concat ['java.lang.Object 'MyObject]
                  cljs-default-transform-rules)}))

  (testing "concats input-path-level transform rules vector with default ones"
    (assert-transform-rules
     ;; input
     {:dalap
      {:paths {"a" {:output "b"
                    :transform-rules ['java.lang.Object 'MyObject]}}}}
     ;; output
     {["a" "b"] (concat ['java.lang.Object 'MyObject]
                  cljs-default-transform-rules)}))

  (testing "concats different transform rules vectors giving
            preference to input-path level ones"
    (assert-transform-rules
     ;;input
     {:dalap
      {:transform-rules ['Object 'TopLevel]
       :paths {"a" {:output "b"
                    :transform-rules ['Object 'InputPathLevel]}}}}
     ;; output
     {["a" "b"] (concat ['Object 'InputPathLevel]
                  ['Object 'TopLevel]
                  cljs-default-transform-rules)}))

  (testing "calls top-level transform-rules function and
            modifies current value of transform-rules vector"
    (assert-transform-rules
     ;; input
     {:dalap
      {:transform-rules
       (fn [transform-rules]
         (is (= transform-rules cljs-default-transform-rules)
             (str "top-level `transform fn` should receive"
                  "`cljs-default-transform-rules`"))
         ;; ^ the top-level transform rules is always
         ;; going to receive cljs-default-transform-rules
         []
         ;; ^ this is going to be the result value of every
         ;; input path
         )
       :paths {"a" "b"}}}
     ;; output
     {["a" "b"] []}
     ;; ^ the transform rules from the path are going to be
     ;; the ones returned by the top level transform-rules function
     ))

  (testing "top-level `transform-rules fn` and inpit-path-level
            `transform-rules fn` are going to be composed"
    (assert-transform-rules
     ;;input
     {:dalap
      {:transform-rules
       (fn [transform-rules]
         (is (= transform-rules cljs-default-transform-rules)
             (str "top-level `transform fn` should receive"
                  "`cljs-default-transform-rules`"))
         ;; ^ the top-level transform rules is always
         ;; going to receive cljs-default-transform-rules
         []
         ;; ^ this is going to be the result value of every
         ;; input path
         )
       :paths
       {"a"
        {:output "b"
         :transform-rules
         (fn [transform-rules-from-top-level]
           (is (= transform-rules-from-top-level []))
           ['Object 'InnerObject])}}}}
     ;; output
     {["a" "b"] ['Object 'InnerObject]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
