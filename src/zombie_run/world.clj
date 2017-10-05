(ns zombie-run.world)

(defn world-center [[x y]]
  [(int (/ x 2)) (int (/ y 2))])

(defn world-left-upper-corner [[_ y]]
  [0 y])

(defn world-right-upper-corner [[x _]]
  [x 0])

(defn in-world? [[world-x world-y] [x y]]
  (and (>= x 0)
       (>= y 0)
       (< x world-x)
       (< y world-y)))

(defn measure-distance [[a-x a-y] [b-x b-y]]
  (let [x-dist (Math/pow (- a-x b-x) 2)
        y-dist (Math/pow (- a-y b-y) 2)
        dist (Math/sqrt (+ x-dist y-dist))]
    (int dist)))

(defn- calc-position [[x y] action steps]
  (case action
    :right [(+ x steps) y]
    :left [(- x steps) y]
    :up [x (- y steps)]
    :up-left [(- x steps) (- y steps)]
    :up-right [(+ x steps) (- y steps)]
    :down [x (+ y steps)]
    :down-left [(- x steps) (+ y steps)]
    :down-right [(+ x steps) (+ y steps)]
    [x y]))

(defn get-position
  ([world current-pos action] (get-position world current-pos action 1))
  ([world current-pos action steps]
   (let [new-pos (calc-position current-pos action steps)]
     (if (in-world? world new-pos)
       new-pos
       current-pos))))
