(defproject specintro "0.1.0-SNAPSHOT"
  :description "Intro to clojure spec"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]]
  :profiles {:dev
             {:dependencies [[org.clojure/test.check "0.9.0"]]
              :plugins [[cider/cider-nrepl "0.9.0-SNAPSHOT"]]}})

