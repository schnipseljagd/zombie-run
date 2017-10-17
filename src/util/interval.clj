(ns util.interval)

(defn start [fn interval]
  (doto (Thread. #(try
                    (while (not (.isInterrupted (Thread/currentThread)))
                      (Thread/sleep interval)
                      (fn))
                    (catch InterruptedException _)))
    (.setDaemon true)
    (.start)))

(defn stop [^Thread thread]
  (.interrupt thread))
