(ns specintro.browsersupport-test
  (:require [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [specintro.browsersupport :refer :all]
            [clojure.spec.test :as stest]))
