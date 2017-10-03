(ns zombie-run.quil
  (:gen-class)
  (:require [quil.core :as q]
            [zombie-run.core :as z])
  (:import (java.awt.event KeyEvent)))

(def valid-keys {;; arrows
                 :right :right
                 :left  :left
                 :up    :up
                 :down  :down
                 ;; wasd
                 :d     :right
                 :a     :left
                 :w     :up
                 :s     :down
                 ;; vim
                 :l     :right
                 :h     :left
                 :j     :down
                 :k     :up

                 ;; fire
                 :f     :fire})

(def width 5)

(defn width* [n]
  (* width n))

(defn display-game [game]
  (q/text (format "Health: %s" (z/player-health game))
          (width* 1)
          (width* 3))
  (when-let [[x y] (z/player-position game)]
    (q/rect (width* x) (width* y) width width 2))

  (doseq [[x y] (z/zombie-positions game)]
    (q/rect (width* x) (width* y) width width 2)))

(defn tick-game-fn [game]
  (fn []
    (Thread/sleep 500)
    (swap! game z/run-zombie-actions)
    (recur)))

(defn run-zombieland [size]
  (let [game (atom (z/make-game {:world-size [size size]}))
        ticker (doto (Thread. (tick-game-fn game))
                 (.setDaemon true)
                 (.start))]
    (q/defsketch zombieland
                 :size [(width* size) (width* size)]
                 :title "world of snake"
                 :draw (fn []
                         (q/background 240)                 ; background color
                         (q/fill 180)                       ; cell body color
                         (q/stroke 220)                     ; cell border color

                         (let [game @game]
                             (display-game game)))
                 :setup (fn []
                          (q/frame-rate 20)                 ; Set FPS
                          (q/background 200))
                 :key-pressed (fn []
                                (if (= \newline (q/raw-key))
                                  (reset! game (z/make-game {:world-size [size size]}))
                                  (when-let [action (get valid-keys (q/key-as-keyword))]
                                    (swap! game (fn [game]
                                                  (z/run-player-action game action))))))
                 :on-close (fn [] (.interrupt ticker)))))

(comment
  (run-zombieland 30))

(defn -main []
  (run-zombieland 30))
