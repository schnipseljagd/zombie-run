(ns zombie-run.core
  (:require [reagent.core :as reagent]
            [zombie-run.view :as view]
            [goog.events :as events]
            [zombie-run.game :as game]))

(def key-code->action
  {"Enter"      :restart
   ;; arrows
   "ArrowRight" :right
   "ArrowLeft"  :left
   "ArrowUp"    :up
   "ArrowDown"  :down
   ;; wasd
   "d"          :right
   "a"          :left
   "w"          :up
   "s"          :down
   "e"          :up-right
   "q"          :up-left
   "z"          :down-left
   "x"          :down-right
   ;; vim
   "l"          :right
   "h"          :left
   "j"          :down
   "k"          :up
   ;; fire
   "f"          :fire})


(defn reset-game [state]
  (prn "resetting game state...")
  (reset! state (-> (game/make-game {:world-size [300 150]})
                    (game/configure-player-weapon (zombie-run.weapon/make-weapon :musket)))))

(defn run-player-action [state action]
  (prn (str "player action: " action "..."))
  (swap! state #(game/run-player-action % action)))

(defn run-zombie-actions [state]
  ;(prn (str "run zombie actions..."))
  (swap! state game/run-zombie-actions))

(defn handle-keydown [game-state e]
  (when-let [action (key-code->action (.-key e))]
    (case action
      :restart (reset-game game-state)
      (run-player-action game-state action))))

(defonce interval (atom nil))

(defonce game-state (reagent/atom nil))

(defn start []
  (reset-game game-state)

  (events/removeAll js/document "keydown")
  (events/listen js/document "keydown" #(handle-keydown game-state %))

  (reset! interval (js/setInterval #(run-zombie-actions game-state) 100)))

(defn stop []
  (when [@interval]
    (js/clearInterval @interval)
    (reset! interval nil)))

(defn reset []
  (stop)
  (start))

; enable printing to console for debugging
(enable-console-print!)

; register reagent rendering
(reagent/render [(fn [_] [view/world @game-state])]
                (js/document.getElementById "app"))

; register event handling and game loop
(reset)
