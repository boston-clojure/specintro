(ns specintro.guide
  "examples from clojure spec guide at http://clojure.org/guides/spec"
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as stest])
  (:import java.util.Date))

;;; --------------
;;; defining specs
;;; --------------

;;; spec basic functions
;;; s/conform is used to conform to a spec if valid  else return error info
;;; s/valid is used to test if a form matches a spec

;;; specs with predicates: returns the value if it conforms to
;;; a spec and a special keyword :clojure.spec/invalid if it doesn't
(s/conform even? 1000)
(s/conform even? 999)

;; test predicates: the returned value is a boolean
(s/valid? #(> % 5) 10)
(s/valid? #(> % 5) 0)

;; test set membership
(s/valid? #{:club :diamond :heart :spade} :club)
(s/valid? #{:club :diamond :heart :spade} 42)

;; register specs in fully qualified namespaces
(s/def ::date inst?)
(s/def ::suit #{:club :diamond :heart :spade})

;; use registered spec
(s/valid? ::date (Date.))
(s/conform ::suit :club)

;;; composing predicates
;; composing predicates with and
(s/def ::big-even (s/and int? even? #(> % 1000)))
(s/valid? ::big-even :foo)
(s/valid? ::big-even 10)
(s/valid? ::big-even 100000)
;; composing predicates with or
(s/def ::name-or-id (s/or :name string?
                          :id   int?))
(s/valid? ::name-or-id "abc")
(s/valid? ::name-or-id 100)
(s/valid? ::name-or-id :foo)

;; s/explain is used to report why a value does not conform to a spec:
;; explain prints the reason to the console, returns nil
;; explain-str returns the explanation as a string
;; Return/print a string "Success!" if the value conforms to the spec
(s/explain ::suit 42)
(s/explain-str ::suit 42)
(s/explain-str ::name-or-id :foo)
(s/explain ::suit :club)
(s/explain-str ::suit :club)

;; map specs
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))
(s/def ::acctid int?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email ::email-type)
(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                        :opt [::phone]))

;; validate namespaced key-maps
(s/valid? ::person
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon@example.com"})
(s/valid? ::person
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon"})

;; validate non-namespaced key-maps
(s/def :unq/person
  (s/keys :req-un [::first-name ::last-name ::email]
          :opt-un [::phone]))
(s/conform :unq/person
           {:first-name "Elon"
            :last-name "Musk"
            :email "elon@example.com"})

;; with records
(defrecord Person [first-name last-name email phone])
(s/conform :unq/person
           (->Person "Elon" "Musk" "elon@example.com" nil))

;; specs with parts
(s/def :animal/kind string?)
(s/def :animal/says string?)
(s/def :animal/common (s/keys :req [:animal/kind :animal/says]))
(s/def :dog/tail? boolean?)
(s/def :dog/breed string?)
(s/def :animal/dog (s/merge :animal/common
                            (s/keys :req [:dog/tail? :dog/breed])))
(s/valid? :animal/dog
          {:animal/kind "dog"
           :animal/says "woof"
           :dog/tail? true
           :dog/breed "retriever"})

;; specs for collections
;; homogenous collections
(s/conform (s/coll-of keyword?) [:a :b :c])
(s/conform (s/coll-of number?) [1 2 3])
(s/conform (s/coll-of number? :distinct true) [1 2 3])
(s/conform (s/map-of keyword? string?) {:a "A" :b "B"})

;; specs for tuples
(s/def ::point (s/tuple double? double? double?))
(s/conform ::point [1.5 2.5 -0.5])

;;; specs for sequences
;; sequences using cat and wildcards
(s/def ::ingredient (s/cat :quantity number? :unit keyword?))
(s/conform ::ingredient [2 :teaspoon])
(s/def ::seq-of-keywords (s/* keyword?))
(s/conform ::seq-of-keywords [:a :b :c])
(s/def ::odds-then-maybe-even (s/cat :odds (s/+ odd?)
                                     :even (s/? even?)))
(s/conform ::odds-then-maybe-even [1 3 5 100])
(s/def ::opts (s/* (s/cat :opt keyword? :val boolean?)))
(s/conform ::opts [:silent? false :verbose true])

;; sequences with alt and wildcards
(s/def ::config (s/*
                 (s/cat :prop string?
                        :val  (s/alt :s string? :b boolean?))))
(s/conform ::config ["-server" "foo" "-verbose" true "-user" "joe"])
;; spec descriptions
(s/describe ::config)

;; nested sequences
(s/def ::nested
  (s/cat :names-kw #{:names}
         :names (s/spec (s/* string?))
         :nums-kw #{:nums}
         :nums (s/spec (s/* number?))))
(s/conform ::nested [:names ["a" "b"] :nums [1 2 3]])

;; function specs
(defn ranged-rand
  [start end]
  (+ start (long (rand (- end start)))))

(s/fdef ranged-rand
        :args (s/and (s/cat :start int? :end int?)
                     #(< (:start %) (:end %)))
        :ret int?
        :fn (s/and #(>= (:ret %) (-> % :args :start))
                   #(< (:ret %) (-> % :args :end))))

;; ---------------
;; Using Specs
;; ---------------

;; using specs as function pre/post conditions validation
(defn person-name
  [person]
  {:pre [(s/valid? :unq/person person)]
   :post [(s/valid? string? %)]}
  (str (:first-name person) " " (:last-name person)))
;; (person-name 42) ;; fails spec will not compile file
(person-name {:first-name "elon", :last-name "musk", :email "elon@musk.com"})
(person-name (->Person "elon" "musk" "elon@musk.com" nil))

;; instrumentation during development
(stest/instrument `ranged-rand)
;; (ranged-rand 8 5) ;; fails spec and file will not compile
(stest/unstrument `ranged-rand)

;; using specs to destructure inputs
(defn- set-config [prop val]
  ;; dummy fn
  (println "set" prop val))

(defn configure [input]
  (let [parsed (s/conform ::config input)]
    (if (= parsed ::s/invalid)
      (throw (ex-info "Invalid input" (s/explain-data ::config input)))
      (for [{prop :prop [_ val] :val} parsed]
        (set-config (subs prop 1) val)))))

(configure ["-server" "foo" "-verbose" true "-user" "joe"])

;; using specs for function docs
;; (clojure.repl/doc ranged-rand)

;; complete example - card-game
(def suit? #{:club :diamond :heart :spade})
(def rank? (into #{:jack :queen :king :ace} (range 2 11)))
(def deck (for [suit suit? rank rank?] [rank suit]))
(s/def ::card (s/tuple rank? suit?))
(s/def ::hand (s/* ::card))
(s/def ::name string?)
(s/def ::score int?)
(s/def ::player (s/keys :req [::name ::score ::hand]))
(s/def ::players (s/* ::player))
(s/def ::deck (s/* ::card))
(s/def ::game (s/keys :req [::players ::deck]))
;; validate
(s/valid? ::player
          {::name "Kenny Rogers"
           ::score 100
           ::hand []})
(s/explain ::game
           {::deck deck
            ::players [{::name "Kenny Rogers"
                        ::score 100
                        ::hand [[2 :banana]]}]})

;; ---------------
;; Testing
;; ---------------

;; generators with gen
;; generate and sample from generators
(gen/generate (s/gen int?))
(gen/sample (s/gen string?))
(gen/sample (s/gen ::card))
(gen/sample (s/gen (s/cat :k keyword? :ns (s/+ number?))))
(gen/generate (s/gen ::player))

;; exercise specs with samples and generators
(s/exercise (s/cat :k keyword? :ns (s/+ number?)) 5)
(s/exercise-fn `ranged-rand)

;; generators with predicate specs
(defn divisible-by [n] #(zero? (mod % n)))
(gen/sample (s/gen (s/and int?
                          #(> % 0)
                          (divisible-by 3))))

;; custom spec generators
(s/def ::kws (s/with-gen (s/and keyword? #(= (namespace %) "my.domain"))
               #(s/gen #{:my.domain/name :my.domain/occupation :my.domain/id})))
(s/valid? ::kws :my.domain/name)
(s/exercise ::kws 5)

;; generative testing with check
(stest/check `ranged-rand)
(s/exercise-fn `ranged-rand 10)
