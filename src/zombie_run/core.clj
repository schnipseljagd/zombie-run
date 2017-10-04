(ns zombie-run.core
  (:require [clj-time.core :as t]
            [clj-time.spec :as time-spec]
            [clojure.set :refer [rename-keys]]
            [clojure.spec.alpha :as s]))

(s/def ::position (s/tuple int? int?))

(s/def ::weapon-type #{:dagger :musket :zombie-fist})
(s/def ::range int?)
(s/def ::attack int?)
(s/def ::recharge-delay number?)
(s/def ::last-attack ::time-spec/date-time)
(s/def ::weapon (s/keys :req-un [::weapon-type
                                 ::range
                                 ::attack
                                 ::recharge-delay
                                 ::last-attack]))

(s/def ::type #{:zombie :player})
(s/def ::direction #{:left :right :up :down :up-left :up-right :down-left :down-right})
(s/def ::health int?)

(s/def ::character (s/keys :req-un [::type
                                    ::direction
                                    ::health
                                    ::weapon]))

(s/def ::world-size ::position)
(s/def ::terrain (s/map-of ::position ::character))
(s/def ::player-pos ::position)

(s/def ::game (s/keys :req-un [::world-size
                               ::terrain
                               ::player-pos]))

(s/def ::player-action (conj (s/describe ::direction) :fire))

(defn world-center [[x y]]
  [(int (/ x 2)) (int (/ y 2))])

(defn world-left-upper-corner [[_ y]]
  [0 y])

(defn world-right-upper-corner [[x _]]
  [x 0])

(defn in-world? [[world-x world-y] [x y]]
  (and (>= x 0)
       (>= y 0)
       (< x world-x)
       (< y world-y)))

(defn get-position
  ([pos action] (get-position pos action 1))
  ([[x y] action steps]
   (case action
     :right [(+ x steps) y]
     :left [(- x steps) y]
     :up [x (- y steps)]
     :up-left [(- x steps) (- y steps)]
     :up-right [(+ x steps) (- y steps)]
     :down [x (+ y steps)]
     :down-left [(- x steps) (+ y steps)]
     :down-right [(+ x steps) (+ y steps)]
     [x y])))

(defn distance [[a-x a-y] [b-x b-y]]
  (let [x-dist (Math/pow (- a-x b-x) 2)
        y-dist (Math/pow (- a-y b-y) 2)
        dist (Math/sqrt (+ x-dist y-dist))]
    (int dist)))

;;
;; terrain
;;
(defn make-terrain [type]
  {:type      type
   :direction :up})

(defn set-terrain-direction [terrain direction]
  (assoc terrain :direction direction))

(defn terrain-has-type? [terrain pos type]
  (= (get-in terrain [pos :type]) type))

(defn terrain-accessible? [terrain pos]
  (not (contains? terrain pos)))

(defn damage-terrain [terrain target attack]
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
                            :recharge-delay 0.2
                            :last-attack    no-last-attack}
              :musket      {:range          4
                            :attack         4
                            :recharge-delay 2
                            :last-attack    no-last-attack}

              ; zombie weapon
              :zombie-fist {:range          1
                            :attack         2
                            :recharge-delay 2
                            :last-attack    no-last-attack}})

(defn make-weapon
  ([type]
   (assert (contains? weapons type) "Weapon doesn't exist.")
   (assoc (get weapons type) :weapon-type type)))

(defn weapon-is-ready? [{last-attack :last-attack recharge-delay :recharge-delay}]
  (let [available (t/plus last-attack (t/seconds recharge-delay))
        now (t/now)]
    (or (t/after? now available)
        (t/equal? now available))))

(defn weapon-range [{range :range}] range)

(defn in-weapon-range? [terrain attacker-pos target-pos]
  (= (weapon-range (get-in terrain [attacker-pos :weapon]))
     (distance attacker-pos target-pos)))

(defn weapon-attack [{attack :attack}] attack)

(defn attack-target [terrain attacker-pos target-pos target-type]
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

(defn player-health [{terrain :terrain pos :player-pos}]
  (get-in terrain [pos :health]))

(defn player-position [{pos :player-pos terrain :terrain}]
  (when (contains? terrain pos)
    pos))

(defn player-direction [{terrain :terrain pos :player-pos}]
  (get-in terrain [pos :direction]))

(defn configure-player-weapon [{pos :player-pos :as game} overrides]
  (update-in game [:terrain pos :weapon] merge overrides))

(defn set-player
  ([game position] (set-player game position :up))
  ([game position direction]
   (let [old-pos (:player-pos game)]
     (-> game
         (assoc :player-pos position)
         (update :terrain (fn [terrain]
                            (if old-pos
                              (-> (rename-keys terrain {old-pos position})
                                  (update position set-terrain-direction direction))
                              (assoc terrain position (-> (make-terrain :player)
                                                          (assoc :health player-default-health)
                                                          (assoc :weapon (make-weapon :dagger))
                                                          (set-terrain-direction direction))))))))))

(defn move-player [{world-size :world-size current-pos :player-pos terrain :terrain :as game} action]
  (let [new-pos (get-position current-pos action)
        new-pos (if (and (in-world? world-size new-pos)
                         (terrain-accessible? terrain new-pos))
                  new-pos
                  current-pos)]
    (set-player game new-pos action)))

(defn player-attack [{terrain :terrain player-pos :player-pos :as game}]
  (reduce (fn [game counter]
            (let [target-pos (get-position player-pos (player-direction game) counter)]
              (update game :terrain attack-target player-pos target-pos :zombie)))
          game
          (range 1 (inc (weapon-range (get-in terrain [player-pos :weapon]))))))

(defn run-player-action [game player-action]
  (if (= :fire player-action)
    (player-attack game)
    (move-player game player-action)))

;;
;; zombie
;;
(def zombie-default-health 10)

(defn configure-zombie-weapon [game pos overrides]
  (update-in game [:terrain pos :weapon] merge overrides))

(defn zombie-health [game position]
  (:health (get-in game [:terrain position])))

(defn is-zombie? [terrain]
  (= (:type terrain) :zombie))

(defn zombie-positions [game]
  (sort (reduce-kv #(if (is-zombie? %3) (conj %1 %2) %1)
                   []
                   (:terrain game))))

(defn calculate-zombie-action [[x y] [player-x player-y]]
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

(defn move-zombie [terrain current-pos action]
  (let [new-pos (get-position current-pos action)]
    (if (terrain-accessible? terrain new-pos)
      (clojure.set/rename-keys terrain {current-pos new-pos})
      terrain)))

(defn run-zombie-action [{terrain    :terrain
                          player-pos :player-pos
                          :as        game}
                         current-pos]
  (if (in-weapon-range? terrain current-pos player-pos)
    (update game :terrain attack-target current-pos player-pos :player)
    (let [action (calculate-zombie-action current-pos player-pos)]
      (update game :terrain move-zombie current-pos action))))

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

(defn set-zombies [game zombie-positions]
  (reduce set-zombie game zombie-positions))

(defn make-game [{world-size :world-size player-pos :player-pos zombies :zombies}]
  (-> {:world-size world-size
       :terrain    {}}
      (set-zombies (or zombies
                       [(world-left-upper-corner world-size)
                        (world-right-upper-corner world-size)]))
      (set-player (or player-pos (world-center world-size)))))

(s/fdef make-game
        :ret ::game)

(s/fdef run-player-action
        :args (s/cat :game ::game
                     :player-action ::player-action)
        :ret ::game)

(s/fdef run-zombie-actions
        :args (s/cat :game ::game)
        :ret ::game)
