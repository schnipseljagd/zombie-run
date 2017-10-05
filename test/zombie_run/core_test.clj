(ns zombie-run.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as stest]
            [zombie-run.core :refer :all]))

(stest/instrument (stest/enumerate-namespace 'zombie-run.core))

(defn example-game []
  (make-game {:player-pos [2 3]
              :world-size [5 5]
              :zombies    [[1 2] [0 2]]}))

(defn player-fire-times [game n]
  (last (take (inc n) (iterate #(run-player-action % :fire) game))))

(defn step-times [game n]
  (last (take (inc n) (iterate run-zombie-actions game))))

(deftest move-player-test
  (let [player-move (fn [game action] (-> game
                                          (run-player-action action)
                                          (run-zombie-actions)
                                          (player-position)))]
    (testing "player moves"
      (are [pos action] (= pos (-> (example-game)
                                   (player-move action)))
                        [3 3] :right
                        [1 3] :left
                        [2 2] :up
                        [2 4] :down))

    (testing "player doesn't move out of the world"
      (are [pos action] (= pos (-> (example-game)
                                   (set-player [0 4])
                                   (player-move action)))
                        [0 4] :left
                        [0 4] :down))

    (testing "player cannot move on if a terrain is blocked"
      (is (= [0 1] (-> (example-game)
                       (set-player [0 1])
                       (player-move :down)))))))

(deftest player-attack-test
  (testing "player attack doesn't hit the zombie"
    (let [game (-> (example-game)
                   (set-player [4 4])
                   (run-player-action :fire))]
      (is (= 10 (zombie-health game [1 2])))))

  (testing "player attack hits a zombie"
    (let [game (-> (example-game)
                   (set-player [2 3] :up-left)
                   (run-player-action :fire))]
      (is (= 9 (zombie-health game [1 2])))))

  (testing "player attack kills a zombie"
    (let [game (-> (example-game)
                   (set-player [2 3] :up-left)
                   (configure-player-weapon {:recharge-delay 0})
                   (player-fire-times 10))]
      (is (nil? (zombie-health game [1 2])))))

  (testing "player weapon has a recharge delay"
    (let [game (-> (example-game)
                   (set-player [2 3] :up-left)
                   (configure-player-weapon {:recharge-delay 10000})
                   (player-fire-times 10))]
      (is (= 9 (zombie-health game [1 2])))))

  (testing "player weapon recharges also if it doesn't hit"
    (let [game (-> (example-game)
                   (set-player [3 4] :up-left)
                   (configure-player-weapon {:recharge-delay 10000})
                   (run-player-action :fire)
                   (run-player-action :up-left)
                   (player-fire-times 10))]
      (is (= 10 (zombie-health game [1 2])))))

  (testing "player weapons have different attack damage"
    (let [game (-> (example-game)
                   (set-player [2 3] :up-left)
                   (configure-player-weapon (make-weapon :musket))
                   (run-player-action :fire))]
      (is (= 6 (zombie-health game [1 2]))))))


(deftest move-zombies-test
  (testing "zombies move towards player"
    (let [game (-> (example-game)
                   (set-player [3 4])
                   (run-zombie-actions))]
      (is (= [[1 3] [2 3]] (zombie-positions game)))))

  (testing "zombies cannot move on if a terrain is blocked"
    (let [game (-> (example-game)
                   (run-zombie-actions))]
      (is (= [[1 2] [1 3]] (zombie-positions game)))))

  (testing "if zombies move the player doesn't"
    (let [game (-> (example-game)
                   (run-zombie-actions))]
      (is (= [2 3] (player-position game)))))

  (testing "zombies do not accidentally occupy the same terrain (This hit me already)"
    (let [game (-> (make-game {:player-pos [13 17]
                               :zombies    [[10 17] [10 18]]
                               :world-size [30 30]})
                   (run-zombie-actions))]

      (is (= [[10 18] [11 17]] (zombie-positions game))))))

(deftest zombies-attack-player-test
  (testing "no zombie can attack"
    (let [game (-> (example-game)
                   (set-player [3 4])
                   (run-zombie-actions))]
      (is (= 10 (player-health game)))))

  (testing "one zombie attacks"
    (let [game (-> (example-game)
                   (run-zombie-actions))]
      (is (= 8 (player-health game)))))

  (testing "zombies kill the player"
    (let [game (-> (example-game)
                   (configure-zombie-weapon [1 2] {:recharge-delay 0})
                   (configure-zombie-weapon [0 2] {:recharge-delay 0})
                   (step-times 6))]
      (is (nil? (player-health game)))
      (is (nil? (player-position game)))))

  (testing "zombie weapons have a recharge delay as well"
    (let [game (-> (example-game)
                   (run-zombie-actions)
                   (run-zombie-actions))]
      (is (= 6 (player-health game))))))

(deftest check-specs-test
  (doseq [report (stest/check (stest/enumerate-namespace 'zombie-run.core))]
    (let [result (get-in report [:clojure.spec.test.check/ret :result])]
      (when-not (true? result)
        (prn (:failure report)))
      (is (true? result)))))
