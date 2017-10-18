(ns zombie-run.view
  (:require [zombie-run.game :as game]
            [clojure.string :refer [join]]))

(def world-size [100 100])

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

(defn make-overlay-text [text]
  [:text.overlay
   {:x                  50
    :y                  25
    :text-anchor        "middle"
    :alignment-baseline "central"}
   text])

(defn display-overlay-text [game-state]
  (cond (nil? (game/player-health game-state)) (make-overlay-text "You are dead!")
        (empty? (game/zombie-positions game-state)) (make-overlay-text "You survived!")
        :else nil))

(defn display-zombies [game-state]
  (mapcat #(into [(make-zombie %)]
                 (make-blood %
                             game/zombie-default-health
                             (game/zombie-health game-state %)))
          (game/zombie-positions game-state)))

(defn make-health-text [health]
  [:text.bar
   {:x                  1
    :y                  2
    :text-anchor        "left"
    :alignment-baseline "central"}
   (str "Health: " health)])

(defn make-fire-tip-text []
  [:text.bar
   {:x                  50
    :y                  2
    :text-anchor        "middle"
    :alignment-baseline "central"}
   [:tspan {:x 50 :y 2} "Move in the direction of a zombie"]
   [:tspan {:x 50 :y 4} "and press <f> to fire!"]])

(defn display-player [game-state]
  (when-let [pos (game/player-position game-state)]
    (into [(make-player pos)]
          (make-blood pos
                      game/player-default-health
                      (game/player-health game-state)))))

(defn display-health-text [game-state]
  (when-let [player-health (game/player-health game-state)]
    (make-health-text player-health)))

(defn world [game-state]
  [:div
   (into
     [:svg.world
      {:view-box (join " " (concat [0 0] world-size))}]
     (concat (display-player game-state)

             (display-zombies game-state)

             [(display-health-text game-state)

              (make-fire-tip-text)

              (display-overlay-text game-state)]))])
