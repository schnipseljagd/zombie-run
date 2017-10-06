(ns zombie-run.game
  (:require [clj-time.core :as t]
            [clj-time.spec :as time-spec]
            [clojure.spec.alpha :as s]
            [zombie-run.world :as world]
            [zombie-run.weapon :as weapon]
            [zombie-run.terrain :as terrain]))

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
;; player
;;
(def player-default-health 10)

(defn player-position [{terrain :terrain}]
  (first (terrain/get-positions terrain :player)))

(defn player-health [{terrain :terrain :as game}]
  (terrain/get-property terrain (player-position game) :health))

(defn configure-player-weapon [game overrides]
  (update-in game [:terrain (player-position game) :weapon] merge overrides))

(defn set-player
  ([game new-pos] (set-player game new-pos :up))
  ([game new-pos direction]
   (-> game
       (update :terrain dissoc (player-position game))
       (update :terrain terrain/make-terrain
               new-pos
               {:type      :player
                :health    player-default-health
                :weapon    (weapon/make-weapon :dagger)
                :direction direction}))))

(defn- player-attack [{terrain :terrain world-size :world-size :as game} player-pos]
  (let [player-direction (terrain/get-property terrain player-pos :direction)
        player-weapon (terrain/get-property terrain player-pos :weapon)
        find-target (fn [_ counter]
                      (let [target-pos (world/get-position world-size
                                                           player-pos
                                                           player-direction
                                                           counter)]
                        (when (terrain/has-type? terrain target-pos :zombie)
                          (reduced target-pos))))
        weapon range]
    (if-let [target-pos (reduce find-target
                                nil
                                (range 1 (inc (weapon/range player-weapon))))]
      (let [[weapon damage] (weapon/fire player-weapon)]
        (assoc game :terrain (-> terrain
                                 (terrain/set-property player-pos :weapon weapon)
                                 (terrain/damage target-pos damage))))
      (update game :terrain
              terrain/update-property
              player-pos
              :weapon
              weapon/reset-recharge))))
;;
;; zombie
;;
(def zombie-default-health 10)

(defn configure-zombie-weapon [game pos overrides]
  (update-in game [:terrain pos :weapon] merge overrides))

(defn zombie-health [{terrain :terrain} position]
  (terrain/get-property terrain position :health))

(defn zombie-positions [{terrain :terrain}]
  (sort (terrain/get-positions terrain :zombie)))

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
  (update game :terrain
          terrain/make-terrain
          position
          {:type      :zombie
           :health    zombie-default-health
           :weapon    (weapon/make-weapon :zombie-fist)
           :direction :up}))

(defn- set-zombies [game positions]
  (reduce set-zombie game positions))

(defn- run-zombie-action [{terrain    :terrain
                           world-size :world-size
                           :as        game}
                          current-pos]
  (if-let [player-pos (player-position game)]
    (if (weapon/in-range? terrain current-pos player-pos)
      (let [[weapon damage] (weapon/fire
                              (terrain/get-property terrain current-pos :weapon))]
        (assoc game :terrain (-> terrain
                                 (terrain/set-property current-pos :weapon weapon)
                                 (terrain/damage player-pos damage))))
      (let [action (calculate-zombie-action current-pos player-pos)]
        (update game :terrain terrain/move
                current-pos
                action
                world-size)))
    game))

;;
;; game
;;
(defn make-game [{world-size      :world-size
                  player-pos      :player-pos
                  player-direcion :player-direction
                  zombies         :zombies}]
  (-> {:world-size world-size
       :terrain    (terrain/init-map)}
      (set-zombies (or zombies
                       [(world/world-left-upper-corner world-size)
                        (world/world-right-upper-corner world-size)]))
      (set-player (or player-pos (world/world-center world-size))
                  (or player-direcion :up))))

(defn run-player-action [{world-size :world-size :as game} player-action]
  (if-let [player-position (player-position game)]
    (if (= :fire player-action)
      (player-attack game player-position)
      (update game :terrain terrain/move
              player-position
              player-action
              world-size))
    game))

(defn run-zombie-actions [game]
  (let [game (reduce-kv (fn [game pos {type :type}]
                          (if (= type :zombie)
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
