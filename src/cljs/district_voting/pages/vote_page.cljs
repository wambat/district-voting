(ns district-voting.pages.vote-page
  (:require
    [district-voting.components.bottom-logo :refer [bottom-logo]]
    [district-voting.components.contract-info :refer [contract-info]]
    [district-voting.components.how-to-instructions :refer [how-to-instructions]]
    [district-voting.components.voting-bar :refer [voting-bar]]
    [district-voting.styles :as styles]
    [district0x.components.misc :as misc :refer [row row-with-cols col center-layout paper page]]
    [district-voting.proposals.subs :as proposal-subs]
    [re-frame.core :refer [subscribe dispatch]]))

(defn link [name url]
  [:a {:href url
       :target :_blank}
   name])

(defmethod page :route.vote/home []
  (let [votes (subscribe [:voting/candidates-voters-dnt-total :next-district])
        votes-total (subscribe [:voting/voters-dnt-total :next-district])
        loading? (subscribe [:voting-loading? :next-district])
        can-submit? (subscribe [:district0x/can-submit-into-blockchain?])
        vote-form (subscribe [:form.next-district/vote])
        proposals (subscribe [::proposal-subs/list "district-proposals"])]
    (fn []
      [paper
       {:style {:min-height 600}
        :loading? (or @loading? (:loading? @vote-form))
        :use-loader? true}
       [:h1 {:style (merge styles/text-center
                           styles/margin-bottom-gutter-less)}
        "What should we build next?"]
       [row
        [:div "district0x makes use of a " [link "district proposal process" "https://github.com/district0x/district-proposals"]
         " to allow the community to determine what districts they would like to see built and deployed to the network next by the district0x team.  To signal for a district you would like to see launched, please complete the following steps:"
         [how-to-instructions]
         [:div "Note: You may only vote for one district per address at a time. No DNT are transferred when signaling, the voting mechanism simply registers your indication to your address. As such, the entire DNT balance stored at that address would be counted towards the vote. Once DNT is transferred to a new address, the district's vote total would be lowered by a corresponding amount. Your vote can be changed at any time by voting again from the same address."]
         [contract-info {:contract-key :next-district
                         :style styles/margin-bottom-gutter-less}]
         ]
        (doall
          (for [{:keys [:number :title :body :html_url]} @proposals]
            [:div
             {:key number
              :style {:margin-top styles/desktop-gutter}}
             [:h2
              {:style styles/margin-bottom-gutter-mini}
              title]
             body
             [:div
              {:style styles/margin-top-gutter-less}
              [:div "ID: " number]
              [:a {:href html_url
                   :target :_blank}
               "Read more"]]
             [voting-bar
              {:votes-total @votes-total
               :votes @votes
               :index number
               :loading? @loading?
               :voting-key :next-district
               :form-key :form.next-district/vote}]]))]
       [bottom-logo]])))
