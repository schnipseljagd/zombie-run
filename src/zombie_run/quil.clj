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

(defn width* [n]
  (* 5 n))

(defn display-health-indicator [[x y] current-health default-health]
  (let [number-of-blood (- default-health current-health)]
    (doseq [_ (take number-of-blood (range))]
      (->> (q/rect (+ (width* x) (rand-int 20))
                   (+ (width* y) (rand-int 20))
                   5 5 5)
           (q/with-fill [153 0 0])
           (q/with-stroke [153 0 0])))))

(defn display-player [[x y]]
  (->> (q/rect (width* x) (width* y) 20 20 5)
       (q/with-fill [204 255 255])
       (q/with-stroke [0 51 102])))

(defn display-zombie [[x y]]
  (->> (q/rect (width* x) (width* y) 20 20 5)
       (q/with-fill [204 102 0])
       (q/with-stroke [255 0 0])))

(defn display-game [game]
  (-> (q/load-image "resources/gridpaperlightbluepattern.png")
      (q/background-image))                                 ; background image

  (q/fill 0 0 0)                                            ; default cell body color
  (q/text-size 20)                                          ; default text size

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
  (when-let [pos (z/player-position game)]
    (display-player pos)
    (display-health-indicator pos
                              (z/player-health game)
                              z/player-default-health))

  ; display zombies
  (doseq [pos (z/zombie-positions game)]
    (display-zombie pos)
    (display-health-indicator pos
                              (z/zombie-health game pos)
                              z/zombie-default-health)))

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
                 :draw #(display-game @game)
                 :setup #(q/frame-rate 20)
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
