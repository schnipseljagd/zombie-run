(ns zombie-run.game-test
  (:require [zombie-run.game :refer [make-game
                                     run-player-action
                                     run-zombie-actions
                                     action->coords
                                     player-position
                                     set-player
                                     zombie-health
                                     configure-player-weapon
                                     configure-zombie-weapon
                                     zombie-positions
                                     player-health]]
            [zombie-run.weapon :as weapon :refer [make-weapon]]
            [clojure.spec.test.alpha :as stest]

    #?(:cljs [cljs.test :refer-macros [is are deftest testing]]
       :clj
            [clojure.test :refer :all])))

(stest/instrument `zombie-run.game)

(defn example-game []
  (make-game {:player-pos [2 3]
              :world-size [5 5]
              :zombies    [[1 2] [0 2]]}))

(defn player-fire-times [game n]
  (last (take (inc n) (iterate #(run-player-action % :fire) game))))

(defn step-times [game n]
  (last (take (inc n) (iterate run-zombie-actions game))))

(defn player-move [game action]
  (-> game
      (run-player-action action)
      (run-zombie-actions)
      (player-position)))

(deftest test-action->coords-only-allows-directions
  (is (thrown? #?(:clj  AssertionError
                  :cljs js/Error)
               (action->coords :foo))))

(deftest test-action->coords-returns-an-action-for-movement
  (is (= [0 1] (action->coords :down))))

(deftest test-player-move
  (are [pos action] (= pos (-> (example-game)
                               (player-move action)))
                    [3 3] :right
                    [1 3] :left
                    [2 2] :up
                    [2 4] :down))

(deftest test-player-does-not-move-out-of-the-world
  (are [pos action] (= pos (-> (example-game)
                               (set-player [0 4])
                               (player-move action)))
                    [0 4] :left
                    [0 4] :down))

(deftest test-player-cannot-move-on-if-a-terrain-is-blocked
  (is (= [0 1] (-> (example-game)
                   (set-player [0 1])
                   (player-move :down)))))

(deftest test-player-attack-does-not-hit-the-zombie
  (let [game (-> (example-game)
                 (set-player [4 4])
                 (run-player-action :fire))]
    (is (= 10 (zombie-health game [1 2])))))

(deftest test-player-attack-hits-a-zombie
  (let [game (-> (example-game)
                 (set-player [2 3] :up-left)
                 (run-player-action :fire))]
    (is (= 9 (zombie-health game [1 2])))))

(deftest test-player-attack-kills-a-zombie
  (let [game (-> (example-game)
                 (set-player [2 3] :up-left)
                 (configure-player-weapon {::weapon/recharge-delay 0})
                 (player-fire-times 10))]
    (is (nil? (zombie-health game [1 2])))))

(deftest test-zombie-dies-even-if-the-damage-bigger-than-the-existing-health
  (let [game (-> (example-game)
                 (set-player [2 3] :up-left)
                 (configure-player-weapon (make-weapon :musket))
                 (configure-player-weapon {::weapon/recharge-delay 0})
                 (player-fire-times 10))]
    (is (nil? (zombie-health game [1 2])))))

(deftest test-player-weapon-has-a-recharge-delay
  (let [game (-> (example-game)
                 (set-player [2 3] :up-left)
                 (configure-player-weapon {::weapon/recharge-delay 10000})
                 (player-fire-times 10))]
    (is (= 9 (zombie-health game [1 2])))))

(deftest test-player-weapon-recharges-also-if-it-does-not-hit
  (let [game (-> (example-game)
                 (set-player [3 4] :up-left)
                 (configure-player-weapon {::weapon/recharge-delay 10000})
                 (run-player-action :fire)
                 (run-player-action :up-left)
                 (player-fire-times 10))]
    (is (= 10 (zombie-health game [1 2])))))

(deftest test-if-player-carries-a-musket-attack-damage-is-higher
  (let [game (-> (example-game)
                 (set-player [2 3] :up-left)
                 (configure-player-weapon (make-weapon :musket))
                 (run-player-action :fire))]
    (is (= 6 (zombie-health game [1 2])))))

(deftest test-if-player-carries-a-musket-attack-range-is-higher
  (let [game (-> (make-game {:player-pos       [4 4]
                             :world-size       [5 5]
                             :zombies          [[1 1]]
                             :player-direction :up-left})
                 (configure-player-weapon (make-weapon :musket))
                 (run-player-action :fire))]
    (is (= 6 (zombie-health game [1 1])))))


(deftest test-zombies-move-towards-player
  (let [game (-> (example-game)
                 (set-player [3 4])
                 (run-zombie-actions))]
    (is (= [[1 3] [2 3]] (zombie-positions game)))))

(deftest test-zombies-cannot-move-on-if-a-terrain-is-blocked
  (let [game (-> (example-game)
                 (run-zombie-actions))]
    (is (= [[1 2] [1 3]] (zombie-positions game)))))

(deftest test-if-zombies-move-the-player-does-not
  (let [game (-> (example-game)
                 (run-zombie-actions))]
    (is (= [2 3] (player-position game)))))

(deftest test-zombies-do-not-accidentally-occupy-the-same-terrain
  (let [game (-> (make-game {:player-pos [13 17]
                             :zombies    [[10 17] [10 18]]
                             :world-size [30 30]})
                 (run-zombie-actions))]
    (is (= [[10 18] [11 17]] (zombie-positions game)))))

(deftest test-zombie-cannot-attack-the-player
  (let [game (-> (example-game)
                 (set-player [3 4])
                 (run-zombie-actions))]
    (is (= 10 (player-health game)))))

(deftest test-one-zombie-attacks-the-player
  (let [game (-> (example-game)
                 (run-zombie-actions))]
    (is (= 8 (player-health game)))))

(deftest test-zombies-kill-the-player
  (let [game (-> (example-game)
                 (configure-zombie-weapon [1 2] {::weapon/recharge-delay 0})
                 (configure-zombie-weapon [0 2] {::weapon/recharge-delay 0})
                 (step-times 6))]
    (is (nil? (player-health game)))
    (is (nil? (player-position game)))))

(deftest test-zombie-weapons-have-a-recharge-delay
  (let [game (-> (example-game)
                 (run-zombie-actions)
                 (run-zombie-actions))]
    (is (= 6 (player-health game)))))
