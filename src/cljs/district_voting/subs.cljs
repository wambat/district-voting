(ns district-voting.subs
  (:require
    [cljs-time.core :as t]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [goog.string :as gstring]
    [goog.string.format]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]
    [re-frame.subs :as sbs]
    [district-voting.constants :as constants]
    [print.foo :refer [look]]
    [district0x.utils :as u])
  (:require-macros [reagent.ratom :refer [reaction]]))

(reg-sub
  :votings
  (fn [db]
    (:votings db)))

(reg-sub
  :contract-address
  (fn [db [_ contract-key]]
    (get-in db [:smart-contracts contract-key :address])))

(reg-sub
  :voting-loading?
  (fn [db [_ ] [voting-key]]
    (get-in db [:votings voting-key :loading?])))

(reg-sub
  :voting-time-remaining
  (fn [db [_ voting-key]]
    (let [time-remaining (u/time-remaining (:now db) (get-in db [:votings voting-key :end-time]))]
      (if (some neg? (vals time-remaining))
        (medley/map-vals (constantly 0) time-remaining)
        time-remaining))))
(reg-sub
 :voting-form
 (fn [db _ [project]]
   (get-in db [:voting-forms project :default])))

(reg-sub
  :voting/voters-dnt-total
  :<- [:district0x/balances]
  :<- [:votings]
  (fn [[balances votings] [_] [voting-key]]
    (->> (vals (get-in votings [voting-key :voting/candidates]))
      (reduce #(set/union %1 (:candidate/voters %2)) #{})
      (select-keys balances)
      vals
      (map :dnt)
      (reduce + 0))))

(reg-sub
  :voting/candidates-voters-dnt-total
  :<- [:district0x/balances]
  :<- [:votings]
  (fn [[balances votings] [_] [voting-key]]
    (medley/map-vals (fn [{:keys [:candidate/voters]}]
                       (->> voters
                         (select-keys balances)
                         vals
                         (map :dnt)
                         (reduce + 0)))
                     (get-in votings [voting-key :voting/candidates]))))

(reg-sub
  :voting/active-address-voted?
  :<- [:district0x/active-address]
  :<- [:votings]
  (fn [[active-address votings] [_ voting-key candidate-index]]
    (contains? (get-in votings [voting-key :voting/candidates candidate-index :candidate/voters])
               active-address)))

(reg-sub
 :sorted-list
 (fn [_ [_ sort-options] [lst sort-order]]
   (let [sorted (sort-by (get-in sort-options
                                 [sort-order :cmp-fn]) lst)]
     (if (get-in sort-options [sort-order :reverse?])
       (reverse sorted)
       sorted))))

(reg-sub
 :limited-list
 (fn [_ _ [lst limit]]
   (if (pos? limit)
     (take limit lst)
     lst)))

(defn count-reactions [reactions]
  (let [r (dissoc reactions :url :total_count)
        ops [[#{:+1} +]]]
    (reduce (fn [acc [sign cnt]]
              (let [op (some (fn [[signs op]]
                               (if-let [o (contains? signs sign)]
                                 op
                                 #(identity %1))) ops)]
                (op acc cnt))) 0 r)))

(reg-sub
 :proposals/list
 (fn [db [_] [project]]
   (look project)
   (look project)
   (look db)
   (get-in db [:votings project :voting/proposals])))

(reg-sub
 :proposals/list-open-with-votes-and-reactions
 (fn [_ [project]]
   (look project)
   {:lst  (sbs/subscribe [:proposals/list] [(reaction project)])
    :votes (sbs/subscribe [:voting/candidates-voters-dnt-total] [(reaction project)])})
 (fn [{:keys [lst votes]} _]
   (doall (map (fn [p]
                 (-> p
                     (assoc :dnt-votes (get votes (:number p)))
                     (update :reactions count-reactions)))
               (filter (fn [p]
                         (= (:state p)
                            "open")) lst)))))

