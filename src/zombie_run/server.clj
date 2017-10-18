(ns zombie-run.server
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources not-found]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defroutes routes
           (GET "/" _
             {:status  200
              :headers {"Content-Type" "text/html; charset=utf-8"}
              :body    (io/input-stream (io/resource "public/index.html"))})
           (resources "/"))

(def http-handler
  (-> routes
      (wrap-defaults api-defaults)
      wrap-gzip))

(defn -main []
  (run-jetty http-handler {:port 8080 :join? false}))
