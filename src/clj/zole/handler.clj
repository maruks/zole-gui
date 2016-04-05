(ns zole.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [zole.middleware :refer [wrap-middleware]]
            [environ.core :refer [env]]))

(def mount-target
  [:div#app
   [:div#preloaded-images
    [:img {:src "/images/10_of_clubs.svg"}]
    [:img {:src "/images/10_of_diamonds.svg"}]
    [:img {:src "/images/10_of_hearts.svg"}]
    [:img {:src "/images/10_of_spades.svg"}]
    [:img {:src "/images/7_of_diamonds.svg"}]
    [:img {:src "/images/8_of_diamonds.svg"}]
    [:img {:src "/images/9_of_clubs.svg"}]
    [:img {:src "/images/9_of_diamonds.svg"}]
    [:img {:src "/images/9_of_hearts.svg"}]
    [:img {:src "/images/9_of_spades.svg"}]
    [:img {:src "/images/ace_of_clubs.svg"}]
    [:img {:src "/images/ace_of_diamonds.svg"}]
    [:img {:src "/images/ace_of_hearts.svg"}]
    [:img {:src "/images/ace_of_spades.svg"}]
    [:img {:src "/images/jack_of_clubs.svg"}]
    [:img {:src "/images/jack_of_diamonds.svg"}]
    [:img {:src "/images/jack_of_hearts.svg"}]
    [:img {:src "/images/jack_of_spades.svg"}]
    [:img {:src "/images/king_of_clubs.svg"}]
    [:img {:src "/images/king_of_diamonds.svg"}]
    [:img {:src "/images/king_of_hearts.svg"}]
    [:img {:src "/images/king_of_spades.svg"}]
    [:img {:src "/images/queen_of_clubs.svg"}]
    [:img {:src "/images/queen_of_diamonds.svg"}]
    [:img {:src "/images/queen_of_hearts.svg"}]
    [:img {:src "/images/queen_of_spades.svg"}]]
   [:h3 "Loading Zole..."]])

(def loading-page
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    (include-css "/css/zole.css")
    (include-css (if (= "dev" (env :env)) "/css/bootstrap.css" "/css/bootstrap.min.css"))]
   [:body
    mount-target
    (include-js "/js/app.js")]))


(defroutes routes
  (GET "/" [] loading-page)
  (GET "/about" [] loading-page)

  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))
