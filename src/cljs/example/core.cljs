(ns example.core
  (:require [reagent.core :as reagent]))

(defn simple-component []
  [:h1 "foo"])

(reagent/render [simple-component]
  (js/document.getElementById "app"))
