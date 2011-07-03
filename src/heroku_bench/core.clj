(ns heroku-bench.core
  (:use [net.cgrand.moustache :only (app)]
        nsfw.render
        hiccup.core
        [hiccup.page-helpers :only (doctype)]
        (ring.middleware file
                         file-info
                         resource
                         params
                         nested-params
                         keyword-params))
  (:require [nsfw.server :as server]))

(def snapshots
  (atom []))

(def logs
  (atom []))

(defn reset []
  (reset! snapshots [])
  (reset! logs []))

(def membean (java.lang.management.ManagementFactory/getMemoryMXBean))

(defn mem-usage []
  (-> (.getHeapMemoryUsage membean)
      bean
      (dissoc :class)))

(defn gen-snapshot []
  (let [threads (Thread/getAllStackTraces)
        count (count (keys threads))]
    {:threads {:count count}
     :mem (mem-usage)
     :timestamp (System/currentTimeMillis)}))

(defn spawn-thread []
  (future (while true
            (Thread/sleep 100000000)))
  (swap! snapshots (fn [coll]
                     (take 10 (cons (gen-snapshot) coll)))))

(defn render-snapshot [snapshot]
  (let [timestamp (:timestamp snapshot)]
    (html
     [:tr
      [:td timestamp]
      [:td (str (dissoc snapshot :timestamp))]])))

(defn main [& body]
  (html
   (doctype :html5)
   [:html
    [:head
     [:title "heroku-bench"]]
    [:body
     body]]))


(defn index-tpl []
  (main
   [:h1 "Heroku Bench"]
   [:a {:href "/"} "home"]
   [:p
    "JVM benchmarking proggy for Heroku.  Project at "
    [:a {:href "http://github.com/zkim/heroku-bench"}
     "http://github.com/zkim/heroku-bench"]
    "."]
   [:ul
    [:li [:a {:href "/threads"} "threads"]]
    #_[:li [:a {:href "/mem"} "memory"]]]))

(defn threads-tpl [snapshots]
  (main
   [:h1 "Heroku Bench"]
   [:h2 "Threads"]
   [:p "Hit reload till something happens."]
   [:table
    [:tr
     [:td [:em "Current"]]]
    (render-snapshot (first snapshots))
    [:tr [:td [:em "History"]]]
    (map render-snapshot (rest snapshots))]))

(defn threads [req]
  (doseq [x (range 1)]
    (spawn-thread))
  (render threads-tpl @snapshots))

(defn index [req]
  (render index-tpl))

(def routes
  (app
   [""] index
   ["threads"] threads))

(defn wrap-exceptions [h]
  (fn [r]
    (try
      (h r)
      (catch Throwable e
        (println "--- Last snapshot ---")
        (println (first @snapshots))
        (println (.getMessage e))
        (println "---------------------")
        (println)))))

(def entry-handler
  (-> routes
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      (wrap-file "resources/public")
      wrap-file-info
      wrap-exceptions))
