(ns example.core
  (:require [reagent.core :as reagent]))

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

(defn display-overlay-text [type]
  (case type
    :survival (make-overlay-text "You survived!")
    :death (make-overlay-text "You are dead!")
    nil))

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

(defn simple-component []
  [:div
   (into
     [:svg.world
      {:view-box "0 0 100 100"}]
     (mapcat identity
             [[(make-health-text 5)
               (make-fire-tip-text)
               (display-overlay-text :none)]
              (into [(make-zombie [2 3])]
                    (make-blood [2 3] 10 1))
              (into [(make-zombie [90 20])]
                    (make-blood [90 20] 10 9))
              (into [(make-player [15 15])]
                    (make-blood [15 15] 10 5))]))])



(reagent/render [simple-component]
                (js/document.getElementById "app"))
