(ns zombie-run.view
  (:require [zombie-run.game :as game]
            [clojure.string :refer [join]]
            [util.resize :refer [resize-to-max-size]]))

(defn world-x-center [[world-x world-y] x]
  (let [orig-ratio (/ world-x world-y)]
    (if (< orig-ratio 1)
      (* x orig-ratio)
      x)))

(defn world-y-center [[world-x world-y] y]
  (let [orig-ratio (/ world-x world-y)]
    (if (> orig-ratio 1)
      (/ y orig-ratio)
      y)))

(defn ->world-size [size]
  (resize-to-max-size size [100 100]))

(defn make-zombie [[x y]]
  [:rect.zombie
   {:x            x
    :y            y
    :width        1
    :height       1
    :fill         "rgb(204,102,0)"
    :stroke-width 0.1
    :stroke       "rgb(255,0,0)"}])

(defn make-player [[x y]]
  [:rect.zombie
   {:x            x
    :y            y
    :width        1
    :height       1
    :fill         "rgb(204,255,255)"
    :stroke-width 0.1
    :stroke       "rgb(0,51,102)"}])

(defn make-blood-drop [[x y]]
  [:rect.blood-drop
   {:x      x
    :y      y
    :width  0.3
    :height 0.3
    :fill   "rgb(153,0,0)"}])

(defn make-blood [[x y] default-health current-health]
  (let [number-of-blood (- default-health current-health)]
    (repeatedly number-of-blood
                #(make-blood-drop [(+ x (/ (rand-int 10) 10))
                                   (+ y (/ (rand-int 10) 10))]))))

(defn make-overlay-text [game-state text]
  [:text.overlay
   {:x                  (world-x-center (game/world-size game-state) 50)
    :y                  (world-y-center (game/world-size game-state) 50)
    :text-anchor        "middle"
    :alignment-baseline "central"}
   text])

(defn display-overlay-text [game-state]
  (cond (nil? (game/player-health game-state)) (make-overlay-text game-state
                                                                  "You are dead!")
        (empty? (game/zombie-positions game-state)) (make-overlay-text game-state
                                                                       "You survived!")
        :else nil))

(defn display-zombies [game-state]
  (mapcat #(into [(make-zombie %)]
                 (make-blood %
                             game/zombie-default-health
                             (game/zombie-health game-state %)))
          (game/zombie-positions game-state)))

(defn make-health-text [game-state health]
  [:text.bar
   {:x                  (world-x-center (game/world-size game-state) 1)
    :y                  2
    :text-anchor        "left"
    :alignment-baseline "central"}
   (str "Health: " health)])

(defn make-fire-tip-text [game-state]
  [:text.bar
   {:x                  (world-x-center (game/world-size game-state) 50)
    :y                  2
    :text-anchor        "middle"
    :alignment-baseline "central"}
   [:tspan {:x (world-x-center (game/world-size game-state) 50)
            :y 2}
    "Move in the direction of a zombie"]
   [:tspan {:x (world-x-center (game/world-size game-state) 50)
            :y 4}
    "and press <f> to fire!"]])

(defn display-player [game-state]
  (when-let [pos (game/player-position game-state)]
    (into [(make-player pos)]
          (make-blood pos
                      game/player-default-health
                      (game/player-health game-state)))))

(defn display-health-text [game-state]
  (when-let [player-health (game/player-health game-state)]
    (make-health-text game-state player-health)))

(defn world [game-state]
  [:div
   (into
     [:svg.world
      {:view-box (join " " (concat [0 0] (game/world-size game-state)))}]
     (concat (display-player game-state)

             (display-zombies game-state)

             [(display-health-text game-state)

              (make-fire-tip-text game-state)

              (display-overlay-text game-state)]))])
