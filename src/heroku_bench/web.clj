(ns heroku-bench.web
  (:require [heroku-bench.core :as core]
            [nsfw.server :as server]))

(defn -main []
  (-> (server/make core/entry-handler
                   :port (Integer/parseInt (get (System/getenv) "PORT" "8080"))
                   :max-threads (Integer/parseInt (get (System/getenv) "WEB_MAX_THREADS" "20")))
      server/start))
