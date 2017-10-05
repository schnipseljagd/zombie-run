(ns zombie-run.core
  (:require [clj-time.core :as t]
            [clj-time.spec :as time-spec]
            [clojure.set :refer [rename-keys]]
            [clojure.spec.alpha :as s]
            [zombie-run.world :as world]))

;;
;; schema
;;
; weapon
(s/def ::weapon-type #{:dagger :musket :zombie-fist})
(s/def ::range (s/int-in 1 1000))
(s/def ::attack pos-int?)
(s/def ::recharge-delay (s/int-in 0 (* 1000 1000)))
(s/def ::last-attack ::time-spec/date-time)
(s/def ::weapon (s/keys :req-un [::weapon-type
                                 ::range
                                 ::attack
                                 ::recharge-delay
                                 ::last-attack]))

; character (zombie, player)
(s/def ::type #{:zombie :player})
(s/def ::direction #{:left :right :up :down :up-left :up-right :down-left :down-right})
(s/def ::health int?)
(s/def ::character (s/keys :req-un [::type
                                    ::direction
                                    ::health
                                    ::weapon]))

; game
(s/def ::position (s/tuple (s/int-in 0 500) (s/int-in 0 500)))
(s/def ::world-size ::position)
(s/def ::terrain (s/map-of ::position ::character
                           :min-count 1))
(s/def ::game (s/keys :req-un [::world-size
                               ::terrain]))

; make-game
(s/def ::player-pos ::position)
(s/def ::zombies (s/coll-of ::position :distinct true))

; run-player-action
(s/def ::player-action (conj (s/describe ::direction) :fire))

;;
;; terrain
;;
(defn make-terrain [type]
  {:type      type
   :direction :up})

(defn- set-terrain-direction [terrain direction]
  (assoc terrain :direction direction))

(defn- terrain-has-type? [terrain pos type]
  (= (get-in terrain [pos :type]) type))

(defn- terrain-accessible? [terrain pos]
  (not (contains? terrain pos)))

(defn- move-terrain [terrain current-pos action world-size]
  (let [new-pos (world/get-position world-size current-pos action)]
    (if (terrain-accessible? terrain new-pos)
      (-> terrain
          (clojure.set/rename-keys {current-pos new-pos})
          (update new-pos set-terrain-direction action))
      terrain)))

(defn- damage-terrain [terrain target attack]
  (if-let [health (get-in terrain [target :health])]
    (let [new-health (- health attack)]
      (if (zero? new-health)
        (dissoc terrain target)
        (assoc-in terrain [target :health] new-health)))
    (throw (ex-info "Terrain cannot be damaged."
                    {:terrain terrain :pos target}))))

;;
;; weapon
;;
(def no-last-attack (t/date-time 1970))

(def weapons {; player weapons
              :dagger      {:range          1
                            :attack         1
                            :recharge-delay 200
                            :last-attack    no-last-attack}
              :musket      {:range          4
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

(defn- weapon-is-ready? [{last-attack :last-attack recharge-delay :recharge-delay}]
  (let [available (t/plus last-attack (t/millis recharge-delay))
        now (t/now)]
    (or (t/after? now available)
        (t/equal? now available))))

(defn- weapon-range [{range :range}] range)

(defn- in-weapon-range? [terrain attacker-pos target-pos]
  (= (weapon-range (get-in terrain [attacker-pos :weapon]))
     (world/measure-distance attacker-pos target-pos)))

(defn- weapon-attack [{attack :attack}] attack)

(defn- attack-target [terrain attacker-pos target-pos target-type]
  (if (weapon-is-ready? (get-in terrain [attacker-pos :weapon]))
    (let [terrain (assoc-in terrain [attacker-pos :weapon :last-attack] (t/now))]
      (if (terrain-has-type? terrain target-pos target-type)
        (damage-terrain terrain target-pos (weapon-attack (get-in terrain [attacker-pos :weapon])))
        terrain))
    terrain))

;;
;; player
;;
(def player-default-health 10)

(defn- is-player? [terrain]
  (= (:type terrain) :player))

(defn player-position [game]
  (first (reduce-kv #(if (is-player? %3) (conj %1 %2) %1)
                    []
                    (:terrain game))))

(defn player-health [{terrain :terrain :as game}]
  (get-in terrain [(player-position game) :health]))

(defn configure-player-weapon [game overrides]
  (update-in game [:terrain (player-position game) :weapon] merge overrides))

(defn set-player
  ([game new-pos] (set-player game new-pos :up))
  ([game new-pos direction]
   (-> game
       (update :terrain dissoc (player-position game))
       (assoc-in [:terrain new-pos] (-> (make-terrain :player)
                                        (assoc :health player-default-health)
                                        (assoc :weapon (make-weapon :dagger))
                                        (set-terrain-direction direction))))))

(defn- player-attack [{terrain :terrain world-size :world-size :as game} player-pos]
  (reduce (fn [game counter]
            (let [target-pos (world/get-position world-size
                                                 player-pos
                                                 (get-in terrain [player-pos :direction])
                                                 counter)]
              (update game :terrain attack-target player-pos target-pos :zombie)))
          game
          (range 1 (inc (weapon-range (get-in terrain [player-pos :weapon]))))))

(defn run-player-action [{world-size :world-size :as game} player-action]
  (if-let [player-position (player-position game)]
    (if (= :fire player-action)
      (player-attack game player-position)
      (update game :terrain move-terrain
              player-position
              player-action
              world-size))
    game))

;;
;; zombie
;;
(def zombie-default-health 10)

(defn configure-zombie-weapon [game pos overrides]
  (update-in game [:terrain pos :weapon] merge overrides))

(defn zombie-health [game position]
  (:health (get-in game [:terrain position])))

(defn- is-zombie? [terrain]
  (= (:type terrain) :zombie))

(defn zombie-positions [game]
  (sort (reduce-kv #(if (is-zombie? %3) (conj %1 %2) %1)
                   []
                   (:terrain game))))

(defn- calculate-zombie-action [[x y] [player-x player-y]]
  (let [x-dist (- x player-x)
        y-dist (- y player-y)]
    (cond (and (> x-dist 0) (> y-dist 0)) :up-left
          (and (> x-dist 0) (= y-dist 0)) :left
          (and (> x-dist 0) (< y-dist 0)) :down-left
          (and (< x-dist 0) (> y-dist 0)) :up-right
          (and (< x-dist 0) (= y-dist 0)) :right
          (and (< x-dist 0) (< y-dist 0)) :down-right
          (and (= x-dist 0) (> y-dist 0)) :up
          (and (= x-dist 0) (< y-dist 0)) :down)))

(defn run-zombie-action [{terrain    :terrain
                          world-size :world-size
                          :as        game}
                         current-pos]
  (if-let [player-pos (player-position game)]
    (if (in-weapon-range? terrain current-pos player-pos)
      (update game :terrain attack-target current-pos player-pos :player)
      (let [action (calculate-zombie-action current-pos player-pos)]
        (update game :terrain move-terrain
                current-pos
                action
                world-size)))
    game))

;;
;; game
;;
(defn run-zombie-actions [game]
  (let [game (reduce-kv (fn [game pos terrain]
                          (if (is-zombie? terrain)
                            (run-zombie-action game pos)
                            game))
                        game
                        (:terrain game))]
    game))



(defn set-zombie [game position]
  (assoc-in game [:terrain position] (-> (make-terrain :zombie)
                                         (assoc :health zombie-default-health)
                                         (assoc :weapon (make-weapon :zombie-fist)))))

(defn set-zombies [game positions]
  (reduce set-zombie game positions))

(defn make-game [{world-size :world-size player-pos :player-pos zombies :zombies}]
  (-> {:world-size world-size
       :terrain    {}}
      (set-zombies (or zombies
                       [(world/world-left-upper-corner world-size)
                        (world/world-right-upper-corner world-size)]))
      (set-player (or player-pos (world/world-center world-size)))))

;;
;; function specs
;;
(s/fdef make-game
        :args (s/cat :x (s/keys :req-un [::world-size]
                                :opt-un [::player-pos
                                         ::zombies]))
        :ret ::game)

(s/fdef run-player-action
        :args (s/cat :game ::game
                     :player-action ::player-action)
        :ret ::game)

(s/fdef run-zombie-actions
        :args (s/cat :game ::game)
        :ret ::game)
