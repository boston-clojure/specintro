(ns specintro.browsersupport
  "function specs with generative testing"
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as stest]))


;;;; Example of function specification with generative testing.
;;;; A media rich HTML5 web application, features like
;;;; smooth animation, audio hints, etc. require HTML5 specifications 
;;;; that have varying levels of support across the different browsers and platforms
;;;; Given a browser user agent parsed into browser and os versions,
;;;; the function browser-support-level returns the level of support for the web application
;;;; based on a configuration map developed after extensive testing

;;; -------------------
;;; function definition
;;; -------------------
(defrecord BrowserUserAgent [browser os])
(defrecord BrowserConfig [browser-name os-name support-level minimum-allowed-version minimum-fully-supported-version])

(defn browser-support-level
  "returns a key of :unsupported, :allowed, :fullysupported based on input conf and user agent, given a conf list of entries with key=browsername-osname,value=BrowserConfig"
  [all-conf user-agent]
  (let [browser-name (get-in user-agent [:browser :name])
        os-name (get-in user-agent [:os :name])
        conf (get all-conf (keyword (str browser-name "-" os-name)))]
    (cond
      (nil? conf) :unsupported
      (= :unsupported (:support-level conf)) :unsupported
      (= :allowed (:support-level conf)) :allowed
      (>= (get-in user-agent [:browser :browser-version]) (get conf :minimum-fully-supported-version)) :fullysupported
      :else :unsupported)))

;;; -----
;;; specs
;;; -----
;; predicate order is important here! else generators will not work
(s/def ::positive-int (s/and  int?
                              #(>= % 0))) 
(s/def ::os-version ::positive-int)

;; constrain to more recent browser versions
(s/def ::browser-version (s/and ::positive-int
                                #(>= % 25)))

;; can also use a custom generator to define specific test data to generate
(s/def ::browser-version-alternative
  (s/with-gen
    ::positive-int
    (fn [] (s/gen (s/and ::positive-int
                         #(>= % 10)
                         #(< 100))))))

(s/def ::minimum-allowed-version ::browser-version)
(s/def ::minimum-fully-supported-version ::browser-version)
(s/def ::support-level #{:unsupported :allowed :fullysupported})
(s/def ::browser-name #{"Firefox" "Safari" "Chrome" "IE" "Edge"})
(s/def ::os-name #{"Windows" "OSX" "Linux" "ChromeOS" "iOS" "Android"})
(s/def ::browser (s/keys :req-un [::browser-name ::browser-version]))
(s/def ::os (s/keys :req-un [::os-name ::os-version]))
(s/def :unq/browser-user-agent
  (s/keys :req-un [::browser ::os]))
(s/def :unq/browser-config
  (s/keys :req-un [::browser-name ::os-name ::support-level ::minimum-allowed-version ::minimum-fully-supported-version]))

;; use a custom generator for configuration map keys
;; need to define a dependent type such that key = f(value)
;; using generator combinators from s/gen and s/gen wrappers to test.check
(defn- conf-key
  "returns key used for full configuration map given a config entry"
  [conf]
  (let [{:keys [:browser-name :os-name]} conf]
    (keyword (str browser-name "-" os-name))))

(s/def ::map-of-browser-config
  (s/with-gen
    (s/map-of keyword? :unq/browser-config)
    #(gen/fmap (fn [conf-vec]
                 (into {}
                       (for [conf conf-vec]
                         [(conf-key conf) conf])))
               (gen/vector (s/gen :unq/browser-config)))))

;; function spec
(defn- has-config-entry?
  [conf-map conf]
  (contains? conf-map (conf-key conf)))

(s/fdef browser-support-level
        :args (s/cat :conf ::map-of-browser-config
                     :user-agent :unq/browser-user-agent)
        :ret ::support-level
        :fn (s/and
             ;; untested browsers return :unsupported
             #(let [browser-os {:os-name (-> % :args :user-agent :os :os-name)
                                :browser-name (-> % :args :user-agent :browser :browser-name)}]
                (if (not (has-config-entry? (-> % :args :conf) browser-os))
                  (= (:ret %) :unsupported)
                  true))
             ;; tested browsers return :fullysupported if browser version > conf fully-supported-version
             ;; will fail test!
             #_(let [browser-os {:os-name (-> % :args :user-agent :os :os-name)
                                :browser-name (-> % :args :user-agent :browser :browser-name)}
                    br-version (-> % :args :user-agent :browser :browser-version)
                    conf-entry (-> % :args :conf (get (conf-key browser-os)))
                    full-version (:minimum-fully-supported-version conf-entry)]
                (if (and
                     (has-config-entry? (-> % :args :conf) browser-os)
                     (>= br-version full-version))
                  (= (:ret %) :fullysupported)
                  true))))

;;; -----
;;; REPL
;;; -----
(comment
  ;; generate data for data structures used in the function
  (dorun (map println (gen/sample (s/gen :unq/browser-user-agent) 10)))
  (dorun (map println (gen/sample (s/gen :unq/browser-config) 10)))
  (dorun (map clojure.pprint/pprint (gen/sample (s/gen ::map-of-browser-config) 10)))

  ;; sample function inputs and outputs
  (dorun (map clojure.pprint/pprint (s/exercise-fn `browser-support-level)))

  ;; test a named function with generators from specs
  (stest/check `browser-support-level)
  (stest/summarize-results (stest/check `browser-support-level))

  ;; test all functions in namespace
  (-> (stest/enumerate-namespace 'specintro.browsersupport) stest/check)
  
  ;; with failures look for
  ;; :fail for failing input dataset
  ;; :smallest for smallest failing dataset
)







