(ns zombie-run.terrain
  (:require [zombie-run.world :refer [get-position]]
            [clojure.set :refer [rename-keys]]))

(defn init-map [] {})

(defn make-terrain [terrain position properties]
  (assoc terrain position properties))

(defn set-property [terrain position property value]
  (assoc-in terrain [position property] value))

(defn update-property [terrain position property fn]
  (update-in terrain [position property] fn))

(defn has-type? [terrain pos type]
  (= (get-in terrain [pos :zombie-run.game/type]) type))

(defn- accessible? [terrain pos]
  (not (contains? terrain pos)))

(defn move [terrain current-pos action world-size]
  (let [new-pos (get-position world-size current-pos action)]
    (if (accessible? terrain new-pos)
      (-> terrain
          (rename-keys {current-pos new-pos})
          (set-property new-pos :zombie-run.game/direction action))
      terrain)))

(defn damage [terrain target attack]
  (if-let [health (get-in terrain [target :zombie-run.game/health])]
    (let [new-health (- health attack)]
      (if (>= 0 new-health)
        (dissoc terrain target)
        (assoc-in terrain [target :zombie-run.game/health] new-health)))
    (throw (ex-info "Terrain cannot be damaged."
                    {:pos target}))))

(defn get-property [terrain position property]
  (get-in terrain [position property]))

(defn get-positions [terrain type]
  (reduce-kv #(if (= type (:zombie-run.game/type %3)) (conj %1 %2) %1)
             []
             terrain))
