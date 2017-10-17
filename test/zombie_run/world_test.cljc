(ns zombie-run.world-test
  (:require [zombie-run.world :refer [step-into-direction]]

    #?(:cljs [cljs.test :as t :refer-macros [is are deftest]]
       :clj
            [clojure.test :as t :refer :all])))

(def example-world [5 5])

(deftest new-position-in-given-direction
  (is (= [4 3] (step-into-direction example-world [4 4] [0 -1]))))

(deftest does-not-return-positions-outside-the-world
  (are [new current direction] (= new (step-into-direction example-world
                                                           current
                                                           direction))
                               [4 4] [4 4] [1 1]
                               [4 4] [4 4] [0 1]
                               [4 4] [4 4] [1 0]
                               [0 0] [0 0] [-1 -1]
                               [0 0] [0 0] [0 -1]
                               [0 0] [0 0] [-1 0]))

(deftest takes-the-amount-of-steps-to-go
  (is (= [1 1] (step-into-direction example-world [4 4] [-1 -1] 3))))
