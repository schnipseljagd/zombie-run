(ns zombie-run.weapon
  (:require [clj-time.core :as t]
            [zombie-run.world :refer [measure-distance]]))

(def no-last-attack (t/date-time 1970))

(def weapons {; player weapons
              :dagger      {:range          1
                            :attack         1
                            :recharge-delay 200
                            :last-attack    no-last-attack}
              :musket      {:range          100
                            :attack         4
                            :recharge-delay 2000
                            :last-attack    no-last-attack}

              ; zombie weapon
              :zombie-fist {:range          1
                            :attack         2
                            :recharge-delay 2000
                            :last-attack    no-last-attack}})

(defn make-weapon
  ([type]
   (assert (contains? weapons type) "Weapon doesn't exist.")
   (assoc (get weapons type) :weapon-type type)))

(defn weapon-is-ready? [{last-attack :last-attack recharge-delay :recharge-delay}]
  (let [available (t/plus last-attack (t/millis recharge-delay))
        now (t/now)]
    (or (t/after? now available)
        (t/equal? now available))))

(defn weapon-range [{range :range}] range)

(defn in-weapon-range? [terrain attacker-pos target-pos]
  (= (weapon-range (get-in terrain [attacker-pos :weapon]))
     (measure-distance attacker-pos target-pos)))

(defn weapon-attack [{attack :attack}] attack)

(defn weapon-reset-recharge [weapon]
  (assoc weapon :last-attack (t/now)))
