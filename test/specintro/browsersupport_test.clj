(ns specintro.browsersupport-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [specintro.browsersupport :refer :all]
            [clojure.spec.test :as stest]))

;;; use specs for generative testing
(def spec-passed? (comp :result :clojure.spec.test.check/ret first stest/check))

#_(deftest browser-support-level-test
  (is (true? (spec-passed? `browser-support-level))))
