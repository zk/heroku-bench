(ns heroku-bench.dev
  (require [heroku-bench.core :as core]
           [nsfw.server :as server]))

(defonce s (server/make (var core/entry-handler)))
(server/start s :port 3000 :max-threads 1000 :min-threads 2)
