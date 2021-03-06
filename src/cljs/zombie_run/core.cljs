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

(defn current-window-size []
  [(.-innerWidth js/window)
   (.-innerHeight js/window)])

(defn reset-game [state]
  (reset! state (-> (game/make-game {:world-size (view/->world-size
                                                   (current-window-size))})
                    (game/configure-player-weapon (zombie-run.weapon/make-weapon :musket)))))

(defn run-player-action [state action]
  (swap! state #(game/run-player-action % action)))

(defn run-zombie-actions [state]
  (swap! state game/run-zombie-actions))

(defn handle-keydown [game-state e]
  (when-let [action (key-code->action (.-key e))]
    (case action
      :restart (reset-game game-state)
      (run-player-action game-state action))))

(defonce interval (atom nil))

(defn start []
  (let [game-state (reagent/atom nil)]
    ; set initial game state
    (reset-game game-state)

    ; render reagent views
    (reagent/render [(fn [_] [view/world @game-state])]
                    (js/document.getElementById "app"))

    ; handle key events
    (events/removeAll js/document "keydown")
    (events/listen js/document "keydown" #(handle-keydown game-state %))

    ; on window resize restart game with new world size
    (events/removeAll js/document "resize")
    (events/listen js/window "resize" (fn [_] (reset-game game-state)))

    ; start game loop
    (reset! interval (js/setInterval #(run-zombie-actions game-state) 100))))

(defn stop []
  (when [@interval]
    ; stop game loop
    (js/clearInterval @interval)
    (reset! interval nil)))

(defn reset []
  (stop)
  (start))

(reset)
