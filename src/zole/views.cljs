(ns zole.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [clojure.string :refer [blank?]]))

(defn nav-bar []
  (let [state (subscribe [:nav-bar])]
    (fn []
      (let [{:keys [logged-in? username active-panel]} @state]
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
            [:li (when (#{:home-page :tables-page :play-page} active-panel) {:class "active"})
             [:a {:href "#/play"} "Play"]]
            [:li (when (= active-panel :about-page) {:class "active"})
             [:a {:href "#/about"} "About"]]]
           (when logged-in?
             [:p.navbar-text.navbar-right (str "Signed in as " username)])]]]))))

(defn home-page []
  (let [state    (subscribe [:home-page])
        username (r/atom "")]
    (fn []
      (let [{:keys [alert]} @state]
        [:div.container
         [:div.row
          [:div.col-md-6
           [:h2 "Welcome to zole"]]]
         [:div.row
          [:div.col-md-6
           [:form
            [:div.form-group
             [:label {:for "username"} "Username"]
             [:input#username.form-control {:placeholder "Username" :on-change #(reset! username (-> % .-target .-value))}]]
            [:div.form-group
             [:button.btn.btn-default (cond-> {:type "button" :on-click #(dispatch [:user-login @username])} (blank? @username) (assoc :disabled "disabled")) "Login"]]
            [:div.form-group
             [:div.alert.alert-danger {:class (when-not alert "hidden")} alert]]]]]]))))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-4
     [:h2 "About zole"]
     [:span "This is open source implementation of card game "] [:a {:target "_blank" :href "https://en.wikipedia.org/wiki/Zole"} "zole."]]]])

;; tables page

(defn tables-page []
  (let [state (subscribe [:tables-page])]
    (fn []
      (let [{:keys [joined? tables]} @state]
        [:div.container
         [:div.row
          [:div.col-md-6
           [:div.panel.panel-default
            [:div.panel-heading [:h3 "Tables"]]
            [:div.panel-body
             [:form
              [:div.form-group
               (if joined?
                 [:div
                  [:span "You have joined a table"]
                  [:button.btn.btn-default {:type "button" :on-click #(dispatch [:leave-table])} "Leave"]]
                 [:div
                  [:button.btn.btn-default {:type "button" :on-click #(dispatch [:create-table "pvb"])} "Play against computer"]
                  [:button.btn.btn-default {:type "button" :on-click #(dispatch [:create-table "ffa"])} "Create table"]
                  [:button.btn.btn-default {:type "button" :on-click #(dispatch [:user-logout])} "Logout"]])]]
             (when (seq tables)
               [:table.table.table-hover
                [:thead
                 [:tr [:td "Players"] (when-not joined? [:td "Join"])]]
                [:tbody
                 (for [{:keys [name players]} tables]
                   [:tr {:key name}
                    [:td players]
                    (when-not joined?
                      [:td [:button.btn.btn-primary.btn-xs {:type "button" :on-click #(dispatch [:join-table name])} "Join"]])])]])]]]]]))))

;; play page

(defn show-cards [cards]
  [:div.col-md-10.cards-div
   (for [[r s] cards]
     ^{:key (str r s)} [:div.card-div [:img.card {:src (str "images/" r "_of_" s ".png") :on-click #(dispatch [:play-card [r s]])}]])])

(defn show-player [player [game-type player-name] last-game-player disconnected-player winner-player]
  [:div
   [:p {:class (if (= player winner-player) "thick bg-success" "thick")} player]
   [:p.bg-info
    (case game-type
      "galds" game-type
      "zole" (if (= player-name player) game-type "mazais")
      "lielais" (if (= player-name player) game-type "mazais")
      "")]
   (when (= player last-game-player)
     [:p.bg-warning "last game"])
   (when (= player disconnected-player)
     [:p.bg-danger.text-uppercase "disconnected"])])

(defn score-table [players score total username]
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

(defn points-table [players points username]
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

(defn prompt-panel [prompt last-game? saved]
  (let [p (if (coll? prompt) (first prompt) prompt)]
    [:div.col-md-6.align-center
     (condp = p
       "choose" [:p (str "#" (second prompt))
                 [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(dispatch [:play-lielais])} "Lielais"]
                 [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(dispatch [:play-zole])} "Zole"]
                 [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(dispatch [:play-pass])} "Pass"]]
       "save" [:div {:class "alert alert-info"} (if saved "Choose second card to save!" "Choose two cards to save!")]
       "play" [:div {:class "alert alert-info"} "Your turn to play!"
               (when-not last-game?
                 [:button.btn.btn-primary {:type "button" :on-click #(dispatch [:play-last-game])} "Last game"])]
       :table-closed [:div {:class "alert alert-info"} "Table closed"
                      [:p
                       [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(dispatch [:go-to-tables])} "Tables"]
                       [:button.btn.btn-primary.btn-lg {:type "button" :on-click #(dispatch [:user-logout])} "Logout"]]]
       [:p])]))

(defn play-page []
  (let [state (subscribe [:play-page])]
    (fn []
      (let [{:keys [cards players username plays game-type points score total prompt last-game? last-game-player disconnected-player winner-player saved]} @state
            [first-player second-player] players]

        [:div.container-fluid
         [:div.row.top-buffer.table-row
          [:div.col-md-2.align-center
           [show-player first-player game-type last-game-player disconnected-player winner-player]]
          [:div.col-md-1]
          [:div.col-md-2.align-center (when-let [card (get plays first-player)]
                                        [:img.card {:src (str "images/" (first card) "_of_" (second card) ".png")}])]
          [:div.col-md-2.align-center (when-let [card (get plays username)]
                                        [:img.card {:src (str "images/" (first card) "_of_" (second card) ".png")}])]
          [:div.col-md-2.align-center (when-let [card (get plays second-player)]
                                        [:img.card {:src (str "images/" (first card) "_of_" (second card) ".png")}])]
          [:div.col-md-1]
          [:div.col-md-2.align-center
           [show-player second-player game-type last-game-player disconnected-player winner-player]]]
         [:div.row.top-buffer
          [:div.col-md-1]
          [show-cards cards]
          [:div.col-md-1]]
         [:div.row.top-buffer.prompt-row
          [:div.col-md-3]
          [prompt-panel prompt last-game? saved]
          [:div.col-md-3]]
         [:div.row.top-buffer
          [:div.col-md-6
           [score-table players score total username]]
          [:div.col-md-6
           [points-table players points username]]]]))))

;; main panel

(def panels {:home-page   #'home-page
             :tables-page #'tables-page
             :play-page   #'play-page
             :about-page  #'about-page})

(defn main-panel []
  (let [active-panel (subscribe [:active-panel])]
    (fn []
      [:div
       [nav-bar]
       [(panels @active-panel)]])))
