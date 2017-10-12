(ns zombie-run.core
  (:require [reagent.core :as reagent]
            [zombie-run.view :as view]
            [goog.events :as events]))

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

(defn handle-keydown [e]
  (when-let [action (key-code->action (.-key e))]
    (case action
      :restart (prn "new game...")
      (prn (str "player action: " action)))))

(defonce game (atom nil))

(defn start []
  (events/removeAll js/document "keydown")
  (events/listen js/document "keydown" handle-keydown)
  (reset! game (js/setInterval #(identity "ping") 100)))

(defn stop []
  (when [@game]
    (js/clearInterval @game)
    (reset! game nil)))

(defn reset []
  (stop)
  (start))

; register reagent rendering
(reagent/render [view/world]
                (js/document.getElementById "app"))

; register event handling and game loop
(reset)
