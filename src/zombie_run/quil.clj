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
  ; display text
  (if-let [player-health (z/player-health game)]
    (->> (q/text (format "Health: %s" player-health)
                 (width* 5)
                 (width* 5)))

    (let [[center-x center-y] (z/world-center (:world-size game))]
      (q/text "You are dead!"
              (width* center-x)
              (width* center-y))))

  ; display player
  (when-let [[x y] (z/player-position game)]
    (->> (q/rect (width* x) (width* y) 20 20 5)
         (q/with-fill [204 255 255])
         (q/with-stroke [0 51 102])))

  ; display zombies
  (doseq [[x y] (z/zombie-positions game)]
    (->> (q/rect (width* x) (width* y) 20 20 5)
         (q/with-fill [204 102 0])
         (q/with-stroke [255 0 0]))))

(defn start-interval [fn interval]
  (doto (Thread. #(try
                    (while (not (.isInterrupted (Thread/currentThread)))
                      (Thread/sleep interval)
                      (fn))
                    (catch InterruptedException _)))
    (.setDaemon true)
    (.start)))

(defn stop-interval [ticker]
  (.interrupt ticker))

(defn run-zombieland []
  (let [[world-x world-y] [300 150]
        default-game (z/make-game {:world-size [world-x world-y]})
        game (atom default-game)
        ticker (start-interval #(swap! game z/run-zombie-actions) 100)]
    (q/defsketch zombie-run
                 :size [(width* world-x) (width* world-y)]
                 :title "zombie run"
                 :draw (fn []
                         (-> (q/load-image "resources/gridpaperlightbluepattern.png")
                             (q/background-image))
                         (q/fill 0 0 0)                     ; cell body color
                         (q/no-stroke)                      ; cell border color
                         (q/text-size 20)

                         (display-game @game))
                 :setup (fn []
                          (q/frame-rate 20))                ; Set FPS
                 :key-pressed (fn []
                                (if (= \newline (q/raw-key))
                                  (reset! game default-game)
                                  (when-let [action (get valid-keys (q/key-as-keyword))]
                                    (swap! game #(z/run-player-action % action)))))
                 :on-close #(stop-interval ticker))))

(comment
  (run-zombieland))

(defn -main []
  (run-zombieland))
