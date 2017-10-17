(ns zombie-run.weapon
  (:require [zombie-run.world :refer [measure-distance]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]

    #?(:clj
            [clj-time.spec])

    #?(:cljs [cljs-time.core :as t]
       :clj
            [clj-time.core :as t])))

#?(:cljs (s/def ::date-time (s/with-gen #(t/date? %)
                                        #(s/gen #{(t/date-time 2001 1 1 00 00 00)
                                                  (t/now)
                                                  (t/date-time 2031 1 1 0 00 00)})))
   :clj  (s/def ::date-time :clj-time.spec/date-time))

(s/def ::type #{:dagger :musket :zombie-fist})
(s/def ::range (s/int-in 1 1000))
(s/def ::attack pos-int?)
(s/def ::recharge-delay (s/int-in 0 (* 1000 1000)))
(s/def ::last-attack ::date-time)
(s/def ::weapon (s/keys :req [::type
                              ::range
                              ::attack
                              ::recharge-delay
                              ::last-attack]))


(def no-last-attack (t/date-time 1970))

(def weapons {; player weapons
              :dagger      {::range          1
                            ::attack         1
                            ::recharge-delay 200
                            ::last-attack    no-last-attack}
              :musket      {::range          100
                            ::attack         4
                            ::recharge-delay 2000
                            ::last-attack    no-last-attack}

              ; zombie weapon
              :zombie-fist {::range          1
                            ::attack         2
                            ::recharge-delay 2000
                            ::last-attack    no-last-attack}})

(defn make-weapon
  ([type]
   (assert (contains? weapons type) "Weapon doesn't exist.")
   (assoc (get weapons type) ::type type)))

(defn- is-ready? [{last-attack ::last-attack recharge-delay ::recharge-delay}]
  (let [available (t/plus last-attack (t/millis recharge-delay))
        now (t/now)]
    (or (t/after? now available)
        (t/equal? now available))))

(defn weapon-range [{range ::range}] range)

(defn in-range? [terrain attacker-pos target-pos]
  (= (weapon-range (get-in terrain [attacker-pos ::weapon]))
     (measure-distance attacker-pos target-pos)))

(defn reset-recharge [weapon]
  (assoc weapon ::last-attack (t/now)))

(defn fire [weapon]
  (if (is-ready? weapon)
    (let [weapon (reset-recharge weapon)]
      [weapon (::attack weapon)])
    [weapon 0]))
