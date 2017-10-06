(ns zombie-run.game
  (:require [clj-time.core :as t]
            [clojure.spec.alpha :as s]
            [zombie-run.world :as world]
            [zombie-run.weapon :as weapon]
            [zombie-run.terrain :as terrain]))

; terrain variants for zombies and player
(defmethod terrain/terrain-type :zombie [_]
  (s/keys :req [::terrain/type
                ::terrain/direction
                ::terrain/health
                ::weapon/weapon]))
(defmethod terrain/terrain-type :player [_]
  (s/keys :req [::terrain/type
                ::terrain/direction
                ::terrain/health
                ::weapon/weapon]))


; game
(s/def ::world-size ::terrain/position)
(s/def ::game (s/keys :req [::world-size
                            ::terrain/terrain-map]))

; make-game
(s/def ::player-pos ::terrain/position)
(s/def ::player-direction ::terrain/direction)
(s/def ::zombies (s/coll-of ::terrain/position :distinct true))

; run-player-action
(s/def ::player-action (conj (s/describe ::terrain/direction) :fire))

;;
;; player
;;
(def player-default-health 10)

(defn player-position [{terrain ::terrain/terrain-map}]
  (first (terrain/get-positions terrain :player)))

(defn player-health [{terrain ::terrain/terrain-map :as game}]
  (terrain/get-property terrain (player-position game) ::terrain/health))

(defn configure-player-weapon [game overrides]
  (update-in game [::terrain/terrain-map (player-position game) ::weapon/weapon] merge overrides))

(defn set-player
  ([game new-pos] (set-player game new-pos :up))
  ([game new-pos direction]
   (-> game
       (update ::terrain/terrain-map dissoc (player-position game))
       (update ::terrain/terrain-map terrain/make-terrain
               new-pos
               {::terrain/type      :player
                ::terrain/health    player-default-health
                ::weapon/weapon     (weapon/make-weapon :dagger)
                ::terrain/direction direction}))))

(defn- player-attack [{terrain ::terrain/terrain-map world-size ::world-size :as game} player-pos]
  (let [player-direction (terrain/get-property terrain player-pos ::terrain/direction)
        player-weapon (terrain/get-property terrain player-pos ::weapon/weapon)
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
                                (range 1 (inc (weapon/weapon-range player-weapon))))]
      (let [[weapon damage] (weapon/fire player-weapon)]
        (assoc game ::terrain/terrain-map (-> terrain
                                              (terrain/set-property player-pos ::weapon/weapon weapon)
                                              (terrain/damage target-pos damage))))
      (update game ::terrain/terrain-map
              terrain/update-property
              player-pos
              ::weapon/weapon
              weapon/reset-recharge))))
;;
;; zombie
;;
(def zombie-default-health 10)

(defn configure-zombie-weapon [game pos overrides]
  (update-in game [::terrain/terrain-map pos ::weapon/weapon] merge overrides))

(defn zombie-health [{terrain ::terrain/terrain-map} position]
  (terrain/get-property terrain position ::terrain/health))

(defn zombie-positions [{terrain ::terrain/terrain-map}]
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
  (update game ::terrain/terrain-map
          terrain/make-terrain
          position
          {::terrain/type      :zombie
           ::terrain/health    zombie-default-health
           ::weapon/weapon     (weapon/make-weapon :zombie-fist)
           ::terrain/direction :up}))

(defn- set-zombies [game positions]
  (reduce set-zombie game positions))

(defn- run-zombie-action [{terrain    ::terrain/terrain-map
                           world-size ::world-size
                           :as        game}
                          current-pos]
  (if-let [player-pos (player-position game)]
    (if (weapon/in-range? terrain current-pos player-pos)
      (let [[weapon damage] (weapon/fire
                              (terrain/get-property terrain current-pos ::weapon/weapon))]
        (assoc game ::terrain/terrain-map (-> terrain
                                              (terrain/set-property current-pos ::weapon/weapon weapon)
                                              (terrain/damage player-pos damage))))
      (let [action (calculate-zombie-action current-pos player-pos)]
        (update game ::terrain/terrain-map terrain/move
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
  (-> {::world-size          world-size
       ::terrain/terrain-map (terrain/init-map)}
      (set-zombies (or zombies
                       [(world/world-left-upper-corner world-size)
                        (world/world-right-upper-corner world-size)]))
      (set-player (or player-pos (world/world-center world-size))
                  (or player-direcion :up))))

(defn run-player-action [{world-size ::world-size :as game} player-action]
  (if-let [player-position (player-position game)]
    (if (= :fire player-action)
      (player-attack game player-position)
      (update game ::terrain/terrain-map terrain/move
              player-position
              player-action
              world-size))
    game))

(defn run-zombie-actions [game]
  (let [game (reduce-kv (fn [game pos {type ::terrain/type}]
                          (if (= type :zombie)
                            (run-zombie-action game pos)
                            game))
                        game
                        (::terrain/terrain-map game))]
    game))

;;
;; function specs
;;
(s/fdef make-game
        :args (s/cat :x (s/keys :req-un [::world-size]
                                :opt-un [::player-pos
                                         ::player-direction
                                         ::zombies]))
        :ret ::game)

(s/fdef run-player-action
        :args (s/cat :game ::game
                     :player-action ::player-action)
        :ret ::game)

(s/fdef run-zombie-actions
        :args (s/cat :game ::game)
        :ret ::game)
