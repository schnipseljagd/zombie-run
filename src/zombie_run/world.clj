(ns zombie-run.world)

(defn world-center [[x y]]
  [(int (/ x 2)) (int (/ y 2))])

(defn world-left-upper-corner [[_ y]]
  [0 y])

(defn world-right-upper-corner [[x _]]
  [x 0])

(defn measure-distance [[a-x a-y] [b-x b-y]]
  (let [x-dist (Math/pow (- a-x b-x) 2)
        y-dist (Math/pow (- a-y b-y) 2)
        dist (Math/sqrt (+ x-dist y-dist))]
    (int dist)))

(defn- in-world? [[world-x world-y] [x y]]
  (and (>= x 0)
       (>= y 0)
       (< x world-x)
       (< y world-y)))

(defn step-into-direction
  ([world current-pos direction] (step-into-direction world current-pos direction 1))
  ([world current-pos direction steps]
   (let [new-pos (->> [steps steps]
                      (map * direction)
                      (map + current-pos)
                      (apply vector))]
     (if (in-world? world new-pos)
       new-pos
       current-pos))))
