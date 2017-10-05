(ns zombie-run.core
  (:require [clj-time.core :as t]
            [clj-time.spec :as time-spec]
            [clojure.set :refer [rename-keys]]
            [clojure.spec.alpha :as s]
            [zombie-run.world :as world]
            [zombie-run.weapon :as weapon]))

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
(defn- make-terrain [type]
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
      (if (>= 0 new-health)
        (dissoc terrain target)
        (assoc-in terrain [target :health] new-health)))
    (throw (ex-info "Terrain cannot be damaged."
                    {:terrain terrain :pos target}))))

(defn- attack-target [terrain attacker-pos target-pos target-type]
  (if (weapon/weapon-is-ready? (get-in terrain [attacker-pos :weapon]))
    (let [terrain (update-in terrain
                             [attacker-pos :weapon]
                             weapon/weapon-reset-recharge)]
      (if (terrain-has-type? terrain target-pos target-type)
        (damage-terrain terrain
                        target-pos
                        (weapon/weapon-attack (get-in terrain [attacker-pos :weapon])))
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
                                        (assoc :weapon (weapon/make-weapon :dagger))
                                        (set-terrain-direction direction))))))

(defn- player-attack [{terrain :terrain world-size :world-size :as game} player-pos]
  (reduce (fn [game counter]
            (let [target-pos (world/get-position world-size
                                                 player-pos
                                                 (get-in terrain [player-pos :direction])
                                                 counter)]
              (update game :terrain attack-target player-pos target-pos :zombie)))
          game
          (range 1 (inc (weapon/weapon-range (get-in terrain [player-pos :weapon]))))))

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

(defn- set-zombie [game position]
  (assoc-in game [:terrain position] (-> (make-terrain :zombie)
                                         (assoc :health zombie-default-health)
                                         (assoc :weapon (weapon/make-weapon :zombie-fist)))))

(defn- set-zombies [game positions]
  (reduce set-zombie game positions))

(defn- run-zombie-action [{terrain    :terrain
                           world-size :world-size
                           :as        game}
                          current-pos]
  (if-let [player-pos (player-position game)]
    (if (weapon/in-weapon-range? terrain current-pos player-pos)
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
(defn make-game [{world-size :world-size player-pos :player-pos zombies :zombies}]
  (-> {:world-size world-size
       :terrain    {}}
      (set-zombies (or zombies
                       [(world/world-left-upper-corner world-size)
                        (world/world-right-upper-corner world-size)]))
      (set-player (or player-pos (world/world-center world-size)))))

(defn run-player-action [{world-size :world-size :as game} player-action]
  (if-let [player-position (player-position game)]
    (if (= :fire player-action)
      (player-attack game player-position)
      (update game :terrain move-terrain
              player-position
              player-action
              world-size))
    game))

(defn run-zombie-actions [game]
  (let [game (reduce-kv (fn [game pos terrain]
                          (if (is-zombie? terrain)
                            (run-zombie-action game pos)
                            game))
                        game
                        (:terrain game))]
    game))

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
