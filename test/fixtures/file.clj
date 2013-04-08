(ns fixtures.file
  (:require [clojure.test :refer [deftest is]]
            #_(:cljs [cljs.only :refer [somefn]]))
  ^:cljs-macro (:require [watchtower.core :refer [watcher]])
  ^:clj (:import [clojure.lang PersistentVector IFn]))


#_(:cljs-do
   (.log js/console "hello world"))

(defprotocol CustomProtocol
  (my-fn [this]))

(defrecord CustomType []
  clojure.lang.IFn
  (invoke [this] nil))

(extend-protocol CustomProtocol
  clojure.lang.PersistentVector
  (my-fn [this] nil)

  java.lang.String
  (my-fn [this] nil))

(deftest hello-world
  (is (= 1 1)))
