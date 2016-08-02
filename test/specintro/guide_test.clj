(ns specintro.guide-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [specintro.guide :refer :all]
            [clojure.spec.test :as stest]))


;;; use specs for generative testing
(def spec-passed? (comp :result :clojure.spec.test.check/ret first stest/check))

(deftest ranged-rand-test
  (is (true? (spec-passed? `ranged-rand))))

