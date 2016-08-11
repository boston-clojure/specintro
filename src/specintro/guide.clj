(ns specintro.guide
  "examples from clojure spec guide at http://clojure.org/guides/spec
   and additional examples"
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
;; explain-data retruns the result as a hashmap
;; If the value conforms to the spec, explain and explain-str return/print a string "Success!"
;; explain-data returns nil
(s/explain ::suit 42)
(s/explain-str ::suit 42)
(s/explain-data ::suit 42)
(s/explain-str ::name-or-id :foo)
(s/explain-data ::name-or-id :foo)
(s/explain ::suit :club)
(s/explain-str ::suit :club)
(s/explain-data ::suit :club)

;; map specs
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))
(s/def ::acctid int?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email ::email-type)
(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                        :opt [::phone]))

;; validate namespaced key-maps: keys come with a validation
;; function
(s/valid? ::person
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon@example.com"})

(::first-name
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon@example.com"})

(s/valid? ::person
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon"})

(s/explain-str ::person
          {::first-name "Elon"
           ::last-name "Musk"
           ::email "elon"})

(s/explain-data ::person
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
(s/conform (s/coll-of number?) {1 2 3 4}); elements of a map are k-v pairs
(s/conform (s/coll-of number? :distinct true) [1 2 3])
(s/conform (s/coll-of keyword? :kind vector? :max-count 3) [:a :b])

;; use s/map-of for maps
(s/conform (s/map-of keyword? string?) {:a "A" :b "B"})


;; specs for tuples
(s/def ::point (s/tuple double? double? double?))
(s/conform ::point [1.5 2.5 -0.5])
(s/def ::odd-even (s/tuple odd? even? odd? even?))
(s/conform ::odd-even [1 2 3 4])
(s/conform ::odd-even [0 1 2 3])

;;; specs for sequences
;; sequences using cat and wildcards
;; The conformed sequence is returned as a collection of matches;
;; its format depends on the operators used.

;; s/cat must have names for elements, returns a conformed result as a map
;; This is important for combining predicates
(s/def ::ingredient (s/cat :quantity number? :unit keyword?))
(s/conform ::ingredient [2 :teaspoon])

;; sequences may be matched to "regular expressions"
;; s/* - 0 or more occurrences,
;; s/+ - 1 or more occurrences
;; s/? - 0 or 1 occurrences
;; 0 or more occurrences of a keyword
(s/def ::seq-of-keywords (s/* keyword?))
(s/conform ::seq-of-keywords [:a :b :c])
(s/conform ::seq-of-keywords [])
(s/conform (s/+ keyword?) [])
(s/conform (s/? keyword?) [:hi])
(s/conform (s/? keyword?) [])
(s/conform (s/? keyword?) [:hi :bye])

;; combining regular expressions:
(s/def ::odds-then-maybe-even (s/cat :odds (s/+ odd?)
                                     :even (s/? even?)))
(s/conform ::odds-then-maybe-even [1 3 5 100])
(s/def ::opts (s/* (s/cat :opt keyword? :val boolean?)))
(s/conform ::opts [:silent? false :verbose? true])

;; ----------------
;; Compound specs
;; ----------------


;; s/and passes the conformed value to the next predicate
;; In the case of s/cat, the conformed value is the hashmap with
;; the keys for matched parts of the sequence.
;; The predicate is then applied to the resulting hashmap.
(s/def ::combined-spec (s/and (s/cat :vec vector? :num number?)
                              #(< (count (:vec %)) (:num %))))


(s/conform ::combined-spec [[7 8] 3])
(s/explain-str ::combined-spec [[] :hi])
(s/explain-str ::combined-spec [[7 8 9] 2])


;; sequences with alt and wildcards
(s/def ::config (s/*
                  (s/cat :prop string?
                         :val  (s/alt :s string? :b boolean?))))

(s/conform ::config ["-server" "foo" "-verbose" true "-user" "joe"])
;; spec descriptions
(s/describe ::config)

;; nested sequences
;; s/spec is used for nesting
(s/def ::nested
  (s/cat :names-kw #{:names}
         :names (s/spec (s/* string?))
         :nums-kw #{:nums}
         :nums (s/spec (s/* number?))))
(s/conform ::nested [:names ["a" "b"] :nums [1 2 3]])

;; ----------------------------------------------
;; Function specs, instrumentation, and testing
;; ----------------------------------------------

;; function specs
(defn ranged-rand
  [start end]
  (+ start (long (rand (- end start)))))

;; the matched values accumulate into a hashmap.
;; :args is the conformed value of the :args spec,
;; :ret is a conformed value of the return.
;; They spec in :fn is applied to the accumulated hashmap
(s/fdef ranged-rand
        :args (s/and (s/cat :start int? :end int?)
                     #(< (:start %) (:end %)))
        :ret int?
        :fn (s/and #(>= (:ret %) (-> % :args :start))
                   #(< (:ret %) (-> % :args :end))))

;; Instrumentation checks arguments:

;; instrumentation during development:
(stest/instrument `ranged-rand)
;; If a spec fails, it throws a clojure.lang.ExceptionInfo exception.
;; The exception has data attached to it explaining what failed.
;; Examples:
;; (ranged-rand :hi :bye) ;; wrong type of arguments (fails on the first one)
;; (ranged-rand 5 10 0) ;; fails spec with "Extra input"
;; (ranged-rand 5) ;; fails spec with "Insufficient input"
;; (ranged-rand 8 5) ;; fails the condition on the arguments
;; (ranged-rand :hi 5 4) ;; Careful: fails the int? predicate, not the number of arguments!!!

;; Testing checks return values with randomly generated parameters:

(stest/check `ranged-rand)

;; to stop instrumentation:
(stest/unstrument `ranged-rand)

;; Some useful predicates/tricks:

;; using any? as a wildcard in sequences predicates,
;; using s/nilable to specify that a parameter can be nil
;; (otherwise can't pass nil as a vector):
(defn add-front
  [x v]
  (into [x] v))

(s/fdef add-front
        :args (s/cat :elem any? :vec (s/nilable vector?))
        :ret (s/and vector? #(> (count %) 0))
        :fn #(= (first (:ret %)) (:elem (:args %))))

(stest/instrument `add-front)

;(add-front 5 '(1 2 3)) ; throws an exception
(add-front 5 nil)


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

;; Advanced topic: s/&
;; s/& is used when you have a sequence that is matched using a regular expression,
;; but you also would like to impose additional constraints on the result.
;; For instance, you want a sequence of ints, but you also require
;; that the sequence has the same number of odd and even numbers.
;; s/& takes a regular expression and predicates and checks that
;; the value matched by the regular expression also satisfies all
;; of the predicates.
(s/def ::same-evens-odds (s/& (s/* int?) #(= (count (filter odd? %)) (count (filter even? %)))))

(s/conform ::same-evens-odds [2 3])
(s/explain-str ::same-evens-odds [2 :hi 3])
(s/explain-str ::same-evens-odds [2 3 4])
