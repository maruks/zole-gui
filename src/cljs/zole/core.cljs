(ns zole.core
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [clojure.string :refer [blank?]]
            [goog.string :as gstring]
            [goog.string.format]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! >! put! close! chan timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [zole.env :refer [cljs-env]]))

(defonce app-state (atom {}))

(defn nav-bar [state]
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
        [:li (when (#{:home-page :tables-page :play-page} current-page) {:class "active"})
         [:a {:href "/play"} "Play"]]
        [:li (when (= current-page :about-page) {:class "active"})
         [:a {:href "/about"} "About"]]]
       (when (:loggedin @state)
         [:p.navbar-text.navbar-right (str "Signed in as " (:username @state))])]]]))

(declare connect!)

(defn do-join-table [state name]
  (go (>! (:ws-chan @state) ["join" name])))

(defn create-table [state prefix]
  (let [name (str prefix "-table-" (str (random-uuid)))]
    (swap! state assoc :tablename name)
    (do-join-table state name)))

(defn join-table [state name]
  (swap! state assoc :tablename name)
  (do-join-table state name))

(defn leave-table [state]
  (let [ws-chan (:ws-chan @state)]
    (go (>! ws-chan ["leave"]))))

(defn user-login [state]
  (connect! state (fn [s ws-chan]
                    (go (>! ws-chan ["login" (:username @state)])))))

;; -------------------------
;; Views

(defn home-page [app-state]
  (let [state                           (:home-page @app-state)
        {:keys [connected error alert]} state
        alert-hidden                    (when-not alert "hidden")]
    [:div.container
     [:div.row
      [:div.col-md-6
       [:h2 "Welcome to zole"]]]
     [:div.row
      [:div.col-md-6
       [:form
        [:div.form-group
         [:label {:for "username"} "Username"]
         [:input#username.form-control {:placeholder "Username" :on-change #(swap! app-state assoc :username (-> % .-target .-value))}]]
        [:div.form-group
         [:button.btn.btn-default (cond-> {:type "button" :on-click #(user-login app-state)} (blank? (:username @app-state)) (assoc :disabled "disabled")) "Login"]]
        [:div.form-group
         [:div.alert.alert-danger {:class alert-hidden} alert]]]]]]))

(defn about-page [_]
  [:div.container
   [:div.row
    [:div.col-md-4
     [:h2 "About zole"]
     [:span "This is open source implementation of card game "] [:a {:target "_blank" :href "https://en.wikipedia.org/wiki/Zole"} "zole."]]]])

(defn back-to-tables [state ws-chan]
  (swap! state merge {:tables-page {}
                      :play-page {}
                      :joined false})
  (go (>! ws-chan ["tables"])))

(defn logout [ws-chan]
  (go (>! ws-chan ["logout"])))

(defn prompt-panel [state page ws-chan]
  (let [prompt (:prompt page)
        p      (if (coll? prompt) (first prompt) prompt)]
    [:div.col-md-6.align-center
     (condp = p
       "choose" [:p (str "#" (second prompt))
                 [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(go (>! ws-chan ["lielais"]))} "Lielais"]
                 [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(go (>! ws-chan ["zole"]))} "Zole"]
                 [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(go (>! ws-chan ["pass"]))} "Pass"]]
       "save" [:div {:class "alert alert-info"} (if (:saved page) "Choose second card to save!" "Choose two cards to save!")]
       "play" [:div {:class "alert alert-info"} "Your turn to play!"
               (when-not (:last-game page)
                 [:button.btn.btn-primary {:type "button" :on-click #(go (>! ws-chan ["last_game"]))} "Last game"])]
       :table-closed [:div {:class "alert alert-info"} "Table closed"
                      [:p
                       [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(back-to-tables state ws-chan)} "Tables"]
                       [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(logout ws-chan)} "Logout"]]]
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
       ^{:key (str r s)} [:div.card-div [:img.card {:src (str "/images/" r "_of_" s ".png") :on-click #(playfn r s)}]])]))

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

(defn tables-page [app-state]
  (let [{:keys [joined] :as state} @app-state]
    [:div.container
     [:div.row
      [:div.col-md-6
       [:div.panel.panel-default
        [:div.panel-heading [:h3 "Tables"]]
        [:div.panel-body
         [:form
          [:div.form-group
           (if joined
             [:div
              [:span "You have joined a table"]
              [:button.btn.btn-default {:type "button" :on-click #(leave-table app-state)} "Leave"]]
             [:div
              [:button.btn.btn-default {:type "button" :on-click #(create-table app-state "pvb")} "Play against computer"]
              [:button.btn.btn-default {:type "button" :on-click #(create-table app-state "ffa")} "Create table"]])]]
         (when (seq (-> state :tables-page :tables))
           [:table.table.table-hover
            [:thead
             [:tr [:td "Players"] (when-not joined [:td "Join"])]]
            [:tbody
             (for [table (-> state :tables-page :tables)]
               [:tr {:key (:name table)}
                [:td (:players table)]
                (when-not joined
                  [:td [:button.btn.btn-primary.btn-xs {:type "button" :on-click #(join-table app-state (:name table))} "Join"]])])]])]]]]]))

(defn play-page [state]
  (let [page                         (:play-page @state)
        ws-chan                      (:ws-chan @state)
        crds                         (:cards page)
        players                      (:players page)
        [first-player second-player] players
        username                     (:username @state)]
    [:div.container-fluid
     [:div.row.top-buffer.table-row
      [:div.col-md-2.align-center [:p.thick first-player] [:p (game-type first-player (:game-type page))]]
      [:div.col-md-1]
      [:div.col-md-2.align-center (when-let [card (some-> page :plays (get first-player))]
                                    [:img.card {:src (str "/images/" (first card) "_of_" (second card) ".png")}])]
      [:div.col-md-2.align-center (when-let [card (some-> page :plays (get username))]
                                    [:img.card {:src (str "/images/" (first card) "_of_" (second card) ".png")}])]
      [:div.col-md-2.align-center (when-let [card (some-> page :plays (get second-player))]
                                    [:img.card {:src (str "/images/" (first card) "_of_" (second card) ".png")}])]
      [:div.col-md-1]
      [:div.col-md-2.align-center [:p.thick second-player] [:p (game-type second-player (:game-type page))]]]
     [:div.row.top-buffer
      [:div.col-md-1]
      [cards crds state]
      [:div.col-md-1]]
     [:div.row.top-buffer.prompt-row
      [:div.col-md-3]
      [prompt-panel state page ws-chan]
      [:div.col-md-3]]
     [:div.row.top-buffer
      [:div.col-md-6
       [score-table page username]]
      [:div.col-md-6
       [points-table page username]]]]))

(def page {:home-page #'home-page
           :tables-page #'tables-page
           :play-page #'play-page
           :about-page #'about-page})

(def ws-port (cljs-env :ws-port))

(defn table-joined [state]
  (swap! state assoc :joined true))

(defn table-left [state]
  (swap! state assoc :joined false))

(defn start-game [state players]
  (swap! state assoc-in [:play-page :players] players)
  (session/put! :current-page :play-page))

(defn plays [state [_ player [card]]]
  (swap! state assoc-in [:play-page :plays player] card)
  (swap! state assoc-in [:play-page :prompt] nil))

(defn end-of-game [state msg]
  (let [[_ num tricks points score total] msg
        tricks-map                        (into {} tricks)
        points-tricks                     (map (fn [[player pts]] [player (str pts " (" (tricks-map player) ")")]) points)
        update-fn                         (fn [arg] (partial cons (assoc (into {} arg) "num" num)))]
    (swap! state update-in [:play-page :points] (update-fn points-tricks))
    (swap! state update-in [:play-page :score] (update-fn score))
    (swap! state assoc-in [:play-page :total] (into {} total))
    (swap! state assoc-in [:play-page :game-type] nil)))

(defn error-alert [state alert]
    (swap! state assoc :home-page {:error true :alert alert}))

(defn logged-in [state]
  (swap! state assoc :loggedin true)
  (go (>! (:ws-chan @state) ["tables"])))

(defn logged-out [state]
  (reset! state {})
  (session/put! :current-page :home-page))

(defn last-game [state]
  (swap! state assoc-in [:play-page :last-game] true))

(defn handle-message [state [m1 m2 :as msg]]
  (println "message" msg)
  (cond
    (= m1 "ok") (cond
                  (= m2 "login") (logged-in state)
                  (= m2 "logout") (logged-out state)
                  (= m2 "join") (table-joined state)
                  (= m2 "leave") (table-left state)
                  (= m2 "last_game") (last-game state)
                  (or (= m2 "lielais")
                      (= m2 "zole")
                      (= m2 "pass")) (swap! state assoc-in [:play-page :prompt] nil)
                  (= m2 "tables") (session/put! :current-page :tables-page)
                  (= m2 "save") (do (swap! state assoc-in [:play-page :saved] nil)
                                    (swap! state assoc-in [:play-page :prompt] nil)))

    (= m1 "tables") (swap! state assoc-in [:tables-page :tables] (sort-by :name (map (fn [[table players]] {:name table :players (apply str (interpose \, players))}) m2)))
    (= m1 "open_tables") (swap! state assoc-in [:tables-page :tables] (sort-by :name (map (fn [[table players]] {:name table :players (apply str (interpose \, players))}) m2)))
    (= m1 "cards") (swap! state assoc-in [:play-page :cards] m2)
    (= m1 "players") (start-game state m2)
    (= m1 "last_game") (last-game state)
    (= m1 "prompt") (swap! state assoc-in [:play-page :prompt] m2)
    (= m1 "plays") (plays state msg)
    (= m1 "wins") (swap! state assoc-in [:play-page :plays] {})
    (= m1 "game_type") (swap! state assoc-in [:play-page :game-type] (rest msg))
    (= m1 "table_closed") (swap! state assoc-in [:play-page :prompt] :table-closed)
    (= m1 "end_of_game") (end-of-game state msg)

    (= msg ["error" "already_registered" "login"]) (error-alert state (gstring/format "Username '%s' is already taken." (:username @state)))

    :else (println "unknown message" msg)))

(defn handle-error [state err]
  (println "error " err))

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
            (<! (timeout 1100))))
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

(defn ws-url []
  (println js/window.location.hostname)
  (gstring/format "ws://%s:%s/websocket" js/window.location.hostname ws-port))

(defn connect! [state on-connect-fn]
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch (ws-url) {:format :json}))]
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

(secretary/defroute "/play" []
  (let [current-page (session/get :current-page)]
    (when (= current-page :about-page)
      (let [goto-page (or (session/get :previous-page) :home-page)]
        (session/put! :current-page goto-page)))))

(secretary/defroute "/about" []
  (let [current-page (session/get :current-page)]
    (session/put! :previous-page current-page)
    (session/put! :current-page :about-page)))

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
