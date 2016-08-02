(ns specintro.core-test
  "tests for core functions"
  (:require [clojure.test :refer :all]
            [specintro.core :refer :all]))

(deftest foo-test
  (testing "foo has a bar suffix"
    (is (not= nil
              (re-find #"-bar$" (foo "ed"))))))
