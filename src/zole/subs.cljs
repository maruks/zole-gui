(ns zole.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 :play-page
 (fn [db _]
   (select-keys db [:cards :players :username :plays :game-type :points :score :total :prompt :last-game? :last-game-player :disconnected-player :winner-player :saved])))

(re-frame/reg-sub
 :nav-bar
 (fn [db _]
   (select-keys db [:logged-in? :username :active-panel])))

(re-frame/reg-sub
 :tables-page
 (fn [db _]
   (select-keys db [:joined? :tables])))

(re-frame/reg-sub
 :home-page
 (fn [db _]
   (select-keys db [:alert])))
