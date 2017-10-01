(ns zole.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx]]
            [zole.ws :as ws]
            [zole.db :as db]
            [goog.string :as gstring]
            [goog.string.format]))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (let [{:keys [previous-panel]} db]
     (-> db
         (assoc :previous-panel (:active-panel db))
         (assoc :active-panel (if (= active-panel :play-page)
                                (if (= previous-panel :play-page)
                                  active-panel
                                  (or previous-panel :home-page))
                                active-panel))))))

(reg-event-fx
 :user-login
 (fn [{:keys [db]} [_ username]]
   {:db         (assoc db :username username)
    :ws-connect []}))

(reg-event-fx
 :ws-connected
 (fn [{:keys [db]} [_ ws-chan]]
   {:db (assoc db :ws-chan ws-chan)
    :ws-send [ws-chan ["login" (:username db)]]}))

(reg-event-fx
 :user-logout
 (fn [{:keys [db]} _]
   {:ws-send  [(:ws-chan db) ["logout"]]}))

(reg-event-fx
 :ok-tables
 (fn [{:keys [db]} _]
   {:dispatch [:set-active-panel :tables-page]}))

(reg-event-db
 :open-tables
 (fn [db [_ [tables]]]
   (assoc db :tables (->> tables
                          (map (fn [[table players]] {:name    table
                                                     :players (apply str (interpose \, players))}))
                          (sort-by :name)))))

(reg-event-fx
 :leave-table
 (fn [{:keys [db]} _]
   {:ws-send [(:ws-chan db) ["leave"]]}))

(reg-event-fx
 :play-lielais
 (fn [{:keys [db]} _]
   {:ws-send [(:ws-chan db) ["lielais"]]}))

(reg-event-fx
 :play-pass
 (fn [{:keys [db]} _]
   {:ws-send [(:ws-chan db) ["pass"]]}))

(reg-event-fx
 :play-zole
 (fn [{:keys [db]} _]
   {:ws-send [(:ws-chan db) ["zole"]]}))

(reg-event-fx
 :create-table
 (fn [{:keys [db]} [_ prefix]]
   {:dispatch [:join-table (str prefix "-table-" (str (random-uuid)))]}))

(reg-event-fx
 :join-table
 (fn [{:keys [db]} [_ name]]
   {:db      (assoc db :tablename name)
    :ws-send [(:ws-chan db) ["join" name]]}))

(reg-event-fx
 :ok-login
 (fn [{:keys [db]} _]
   {:db       (assoc db :logged-in? true)
    :ws-send  [(:ws-chan db) ["tables"]]}))

(reg-event-fx
 :play-last-game
 (fn [{:keys [db]} _]
   {:ws-send [(:ws-chan db) ["last_game"]]}))

(reg-event-fx
 :players
 (fn [{:keys [db]} [_ [players]]]
   {:db       (-> db
                  (assoc :players players)
                  (assoc :previous-panel :play-page))
    :dispatch [:set-active-panel :play-page]}))

(reg-event-fx
 :go-to-tables
 (fn [{:keys [db]} _]
   {:db       (-> db
                  (select-keys [:username :logged-in? :active-panel :ws-chan])
                  (assoc :joined? false))
    :ws-send [(:ws-chan db) ["tables"]]}))

(reg-event-fx
 :play-card
 (fn [{:keys [db]} [_ card]]
   (let [{:keys [saved prompt ws-chan]} db
         p                              (if (coll? prompt) (first prompt) prompt)]
     (case p
       "play" {:ws-send [ws-chan ["play" card]]}
       "save" (if saved
                {:ws-send [ws-chan ["save" [saved card]]]}
                {:db (assoc db :saved card)})
       {}))))

(reg-event-db
 :cards
 (fn [db [_ [cards]]]
   (assoc db :cards cards)))

(reg-event-db
 :prompt
 (fn [db [_ [prompt]]]
   (assoc db :prompt prompt)))

(reg-event-db
 :plays
 (fn [db [_ [player [card & more]]]]
   (-> db
       (assoc-in [:plays player] card)
       (dissoc :prompt))))

(reg-event-db
 :ok-lielais
 (fn [db _]
   (dissoc db :prompt)))

(reg-event-db
 :ok-save
 (fn [db _]
   (-> db
       (dissoc :saved)
       (dissoc :prompt))))

(reg-event-db
 :ok-zole
 (fn [db _]
   (dissoc db :prompt)))

(reg-event-db
 :ok-pass
 (fn [db _]
   (dissoc db :prompt)))

(reg-event-db
 :ok-last-game
 (fn [db _]
   (assoc db :last-game? true)))

(reg-event-db
 :last-game
 (fn [db [_ [player]]]
   (-> db
       (assoc :last-game? true)
       (assoc :last-game-player player))))

(reg-event-db
 :disconnected
 (fn [db [_ [player]]]
   (assoc db :disconnected-player player)))

(reg-event-db
 :wins
 (fn [db [_ [player _cards]]]
   (-> db
       (assoc :winner-player player)
       (assoc :plays {}))))

(reg-event-db
 :game-type
 (fn [db [_ [game-type]]]
   (assoc db :game-type game-type)))

(reg-event-db
 :table-closed
 (fn [db _]
   (assoc db :prompt :table-closed)))

(reg-event-db
 :end-of-game
 (fn [db [_ [num tricks points score total]]]
   (let [tricks-map    (into {} tricks)
         points-tricks (map (fn [[player pts]] [player (str pts " (" (tricks-map player) ")")]) points)
         update-fn     (fn [arg] (partial cons (assoc (into {} arg) "num" num)))]
     (-> db
         (update-in [:points] (update-fn points-tricks))
         (update-in [:score] (update-fn score))
         (assoc :total (into {} total))
         (dissoc :game-type)
         (dissoc :winner-player)))))

(reg-event-db
 :error
 (fn [db [ _ [message & more]]]
   (cond-> db
     (= message "already_registered") (assoc :alert (gstring/format "Username '%s' is already taken." (:username db))))))

(reg-event-db
 :ok-logout
 (fn [db _]
   db/default-db))

(reg-event-db
 :ok-join
 (fn [db _]
   (assoc db :joined? true)))

(reg-event-db
 :ok-play
 (fn [db _] db))

(reg-event-db
 :tables
 (fn [db _] db))

(reg-event-db
 :ok-joined
 (fn [db _]
   (assoc db :joined? true)))

(reg-event-db
 :ok-leave
 (fn [db _]
   (assoc db :joined? false)))

(reg-event-db
 :ws-disconnected
 (fn [db _]
   (assoc db/default-db :alert "disconnected")))

(reg-event-db
 :ws-error
 (fn [db [_ error]]
   (assoc db/default-db :alert (str error))))

(reg-fx
 :ws-connect
 (fn [_]
   (ws/connect!)))

(reg-fx
 :ws-send
 (fn [[ws-chan message]]
   (ws/send! ws-chan message)))
