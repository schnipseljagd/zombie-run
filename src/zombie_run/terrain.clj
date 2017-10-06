(ns zombie-run.terrain
  (:require [zombie-run.world :refer [get-position]]
            [clojure.set :refer [rename-keys]]
            [clojure.spec.alpha :as s]))

(s/def ::position (s/tuple (s/int-in 0 500) (s/int-in 0 500)))

(s/def ::type #{:zombie :player})
(s/def ::direction #{:left :right :up :down :up-left :up-right :down-left :down-right})
(s/def ::health int?)
(defmulti terrain-type ::type)

(s/def ::terrain (s/multi-spec terrain-type ::type))
(s/def ::terrain-map (s/map-of ::position ::terrain))


(defn init-map [] {})

(defn make-terrain [terrain position properties]
  (assoc terrain position properties))

(defn remove-terrain [terrain position]
  (dissoc terrain position))

(defn set-property [terrain position property value]
  (assoc-in terrain [position property] value))

(defn update-property
  ([terrain position property func]
   (update-in terrain [position property] func))
  ([terrain position property func & func-args]
   (update-in terrain [position property] #(apply func % func-args))))

(defn has-type? [terrain pos type]
  (= (get-in terrain [pos ::type]) type))

(defn- accessible? [terrain pos]
  (not (contains? terrain pos)))

(defn move [terrain current-pos action world-size]
  (let [new-pos (get-position world-size current-pos action)]
    (if (accessible? terrain new-pos)
      (-> terrain
          (rename-keys {current-pos new-pos})
          (set-property new-pos ::direction action))
      terrain)))

(defn damage [terrain target attack]
  (if-let [health (get-in terrain [target ::health])]
    (let [new-health (- health attack)]
      (if (>= 0 new-health)
        (dissoc terrain target)
        (assoc-in terrain [target ::health] new-health)))
    (throw (ex-info "Terrain cannot be damaged."
                    {:pos target}))))

(defn get-property [terrain position property]
  (get-in terrain [position property]))

(defn get-positions [terrain type]
  (reduce-kv #(if (= type (::type %3)) (conj %1 %2) %1)
             []
             terrain))
