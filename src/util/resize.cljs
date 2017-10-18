(ns util.resize)

(defn resize-to-max-size [[orig-width orig-height] [max-width max-height]]
  (let [orig-ratio (/ orig-width orig-height)
        new-ratio (/ max-width max-height)
        new-size (if (> orig-ratio new-ratio)
                   [max-width (/ max-width orig-ratio)]
                   [(* max-height orig-ratio) max-height])]
    (map int new-size)))
