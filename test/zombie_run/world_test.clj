(ns zombie-run.world-test
  (:require [clojure.test :refer :all]
            [zombie-run.world :refer :all]))

(def example-world [5 5])

(deftest new-position-in-given-direction
  (is (= [4 3] (get-position example-world [4 4] :up))))

(deftest does-not-return-positions-outside-the-world
  (are [new current direction] (= new (get-position example-world
                                                    current
                                                    direction))
                     [4 4] [4 4] :down-right
                     [4 4] [4 4] :down
                     [4 4] [4 4] :right
                     [0 0] [0 0] :up-left
                     [0 0] [0 0] :up
                     [0 0] [0 0] :left))
