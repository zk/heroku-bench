(ns heroku-bench.dev
  (require [heroku-bench.core :as core]
           [nsfw.server :as server]))

(defonce s (server/make (var core/entry-handler) :port 3000))
(server/start s)
