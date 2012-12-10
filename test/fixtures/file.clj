(ns fixtures.file
  (:require [clojure.test :refer [deftest is]])
  (:import [clojure.lang PersistentVector IFn]))

(defprotocol CustomProtocol
  (my-fn [this]))

(defrecord CustomType []
  IFn
  (invoke [this] nil))

(extend-protocol CustomProtocol
  PersistentVector
  (my-fn [this] nil)

  String
  (my-fn [this] nil))

(deftest hello-world
  (is (= 1 1)))