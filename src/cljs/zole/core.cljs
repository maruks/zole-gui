(ns zole.core
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! put! close! chan timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defonce app-state (atom {}))

(defn nav-bar [_]
  (let [current-page (session/get :current-page)]
    [:nav.navbar.navbar-default.navbar-static-top
     [:div.container
      [:div.navbar-header
       [:button.navbar-toggle.collapsed {:type "button" :data-toggle "collapse" :data-target "#navbar" :aria-expanded "false" :aria-controls "navbar"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]
       [:a.navbar-brand "Zole"]]
      [:div#navbar.navbar-collapse.collapse
       [:ul.nav.navbar-nav
        [:li (when (= current-page :home-page) {:class "active"})
         [:a {:href "/"} "Home"]]
        [:li
         [:a {:href "/tables"} "Tables"]]
        [:li (when (= current-page :play-page) {:class "active"})
         [:a {:href "/play"} "Play"]]
        [:li (when (= current-page :about-page) {:class "active"})
         [:a {:href "/about"} "About"]]]]]]))

(declare connect!)

(defn play-against-bots [state]
  (connect! state (fn [s ws-chan]
                    (let [name  (str "player-" (str (random-uuid)))
                          table (str "pvb-table-" (str (random-uuid)))]
                      (swap! s assoc :username name)
                      (swap! s assoc :tablename table)
                      (go (>! ws-chan ["login" name])
                          (>! ws-chan ["join" table]))))))

;; -------------------------
;; Views

(defn home-page [app-state]
  (let [state                           (:home-page @app-state)
        {:keys [connected error alert]} state
        alert-hidden                    (when-not alert "hidden")]
    [:div.container
     [:div.row
      [:div.col-md-6
       [:h2 "Welcome to zole"]
       [:p "Please login to play against other users"]]]
     [:div.row
      [:div.col-md-6
       [:form
        [:div.form-group
         [:label {:for "username"} "Username"]
         [:input#username.form-control {:disabled "disabled" :placeholder "Username"}]]
        [:div.form-group
         [:button.btn.btn-default {:disabled "disabled" :type "button"} "Login"]]
        [:div.form-group
         [:button.btn.btn-default {:type "button" :on-click #(play-against-bots app-state)} "Play against computer"]]
        [:div.form-group
         [:div.alert.alert-danger {:class alert-hidden}
          [:strong "Error "] alert]]]]]]))

(defn about-page [_]
  [:div.container
   [:div.row
    [:div.col-md-4
     [:h2 "About zole"]]]])

(defn prompt-panel [page ws-chan]
  (let [prompt (:prompt page)
        p      (if (coll? prompt) (first prompt) prompt)]
    [:div.col-md-6.align-center
     (condp = p
       "choose" [:p (str "#" (second prompt))
                 [:button.btn.btn-primary.btn-lg.with-padding {:type "button" :on-click #(go (>! ws-chan ["lielais"]))} "Lielais"]
                 [:button.btn.btn-primary.btn-lg.with-padding {:type "button" :on-click #(go (>! ws-chan ["zole"]))} "Zole"]
                 [:button.btn.btn-primary.btn-lg.with-padding {:type "button" :on-click #(go (>! ws-chan ["pass"]))} "Pass"]]
       "save" [:div {:class "alert alert-info"} (if (:saved page) "Choose second card to save!" "Choose two cards to save!")]
       "play" [:div {:class "alert alert-info"} "Your turn to play!"]
       [:p])]))

(defn get-playfn [app-state]
  (let [state   @app-state
        ws-chan (:ws-chan state)
        page    (:play-page state)
        prompt  (:prompt page)
        saved   (:saved page)
        p       (if (coll? prompt) (first prompt) prompt)]
    (cond
      (= p "play") (fn [r s] (go (>! ws-chan ["play" [r s]])))
      (= p "save") (if saved
                     (fn [r s] (go (>! ws-chan ["save" [saved [r s]]])))
                     (fn [r s] (swap! app-state assoc-in [:play-page :saved] [r s])))
      :else (fn [r s] (identity [r s])))))

(defn cards [crds state]
  (let [playfn  (get-playfn state)]
    [:div.col-md-10.cards-div
     (for [[r s] crds]
       ^{:key (str r s)} [:img.card {:src (str "/images/" r "_of_" s ".svg") :height "140" :width "120" :on-click #(playfn r s)}])]))

(defn game-type [player game-type-msg]
  (when-let [t (first game-type-msg)]
    (let [[game-type player-name & r] t]
      (cond
        (= game-type "galds") game-type
        (= game-type "zole") (if (= player-name player) game-type "mazais")
        (= game-type "lielais") (if (= player-name player) game-type "mazais")
        :else ""))))

(defn score-table [{:keys [players score total]} username]
  (when score
    (let [[player1 player2] (remove (partial = username) players)]
      [:table.table.table-condensed
       [:caption "Score"]
       [:thead
        [:tr
         (for [p (list "#" username player1 player2)]
           ^{:key p} [:th p])]]
       [:tbody
        (for [s (cons (assoc total "num" "total") score)]
          ^{:key (s "num")} [:tr
                             [:td (s "num")]
                             [:td (s username)]
                             [:td (s player1)]
                             [:td (s player2)]])]])))

(defn points-table [{:keys [players points]} username]
  (when points
    (let [[player1 player2] (remove (partial = username) players)]
      [:table.table.table-condensed
       [:caption "Points"]
       [:thead
        [:tr
         (for [p (list "#" username player1 player2)]
           ^{:key p} [:th p])]]
       [:tbody
        (for [p points]
          ^{:key (p "num")} [:tr
                             [:td (p "num")]
                             [:td (p username)]
                             [:td (p player1)]
                             [:td (p player2)]])]])))

(defn play-page [state]
  (let [page          (:play-page @state)
        ws-chan       (:ws-chan @state)
        crds          (:cards page)
        players       (:players page)
        first-player  (first players)
        second-player (second players)
        username      (:username @state)]
    [:div.container-fluid
     [:div.row.top-buffer.table-row
      [:div.col-md-2.align-center [:p second-player] [:p (game-type second-player (:game-type page))]]
      [:div.col-md-1]
      [:div.col-md-2.align-center (when-let [card (some-> page :plays (get second-player))]
                                    [:img.card {:src (str "/images/" (first card) "_of_" (second card) ".svg") :height "140" :width "120"}])]
      [:div.col-md-2.align-center (when-let [card (some-> page :plays (get username))]
                                    [:img.card {:src (str "/images/" (first card) "_of_" (second card) ".svg") :height "140" :width "120"}])]
      [:div.col-md-2.align-center (when-let [card (some-> page :plays (get first-player))]
                                    [:img.card {:src (str "/images/" (first card) "_of_" (second card) ".svg") :height "140" :width "120"}])]
      [:div.col-md-1]
      [:div.col-md-2.align-center [:p first-player] [:p (game-type first-player (:game-type page))]]]
     [:div.row.top-buffer
      [:div.col-md-1]
      [cards crds state]
      [:div.col-md-1]]
     [:div.row.top-buffer.prompt-row
      [:div.col-md-3]
      [prompt-panel page ws-chan]
      [:div.col-md-3]]
     [:div.row.top-buffer
      [:div.col-md-6
       [score-table page username]]
      [:div.col-md-6
       [points-table page username]]]]))

(def page {:home-page #'home-page
           :about-page #'about-page
           :play-page #'play-page})

(def ws-url "ws://192.168.1.8:8080/websocket")

(defn table-joined [state]
  (swap! state assoc :joined true)
  (session/put! :current-page :play-page))

(defn plays [state msg]
  (swap! state assoc-in [:play-page :plays (fnext msg)] (first (second (rest msg))))
  (swap! state assoc-in [:play-page :prompt] nil))

(defn end-of-game [state msg]
  (let [[_ num tricks points score total] msg
        update-fn (fn [arg] (partial cons (assoc (into {} arg) "num" num)))]
    (swap! state update-in [:play-page :tricks] (update-fn tricks))
    (swap! state update-in [:play-page :points] (update-fn points))
    (swap! state update-in [:play-page :score] (update-fn score))
    (swap! state assoc-in [:play-page :total] (into {} total))
    (swap! state assoc-in [:play-page :game-type] nil)))

(defn handle-message [state msg]
  (println "message" msg)
  (cond
    (= msg ["ok" "login"]) (swap! state assoc :loggedin true)
    (= msg ["ok" "join"]) (table-joined state)
    (= (first msg) "cards") (swap! state assoc-in [:play-page :cards] (second msg))
    (= (first msg) "players") (swap! state assoc-in [:play-page :players] (second msg))
    (= (first msg) "prompt") (swap! state assoc-in [:play-page :prompt] (second msg))
    (= (first msg) "plays") (plays state msg)
    (= (first msg) "wins") (swap! state assoc-in [:play-page :plays] {})
    (= (first msg) "game_type") (swap! state assoc-in [:play-page :game-type] (rest msg))
    (= (first msg) "end_of_game") (end-of-game state msg)
    :else (println "unknown message" msg)))

(defn handle-error [state err]
  (println "error " err))

(defn error-alert [state alert]
    (swap! state assoc :home-page {:error true :alert alert}))

(defn disconnected [state]
  (println "disconnected")
  (reset! state {})
  (error-alert state "disconnected")
  (session/put! :current-page :home-page))

(defn app-loop [state ws-chan]
  (go-loop []
    (let [{:keys [message error] :as msg} (<! ws-chan)]
      (if-not error
        (when message
          (handle-message state message)
          (when (= (first message) "plays")
            (<! (timeout 1500))))
        (handle-error state error))
      (if msg
        (recur)
        (disconnected state)))))

(defn connected [state ws-chan on-connect-fn]
  (println "connected")
  (swap! state assoc :ws-chan ws-chan)
  (swap! state assoc :home-page {:connected true})
  (app-loop state ws-chan)
  (on-connect-fn state ws-chan))

(defn connection-error [state error]
  (error-alert state (str "Couldn't connect to server " error)))

(defn connect! [state on-connect-fn]
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch ws-url {:format :json}))]
      (if-not error
        (connected state ws-channel on-connect-fn)
        (connection-error state error)))))

(defn current-page [state]
  [:div
   [nav-bar state]
   [(page (session/get :current-page)) state]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page :home-page))

(secretary/defroute "/about" []
  (session/put! :current-page :about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [current-page app-state] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
