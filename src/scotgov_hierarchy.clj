;; # Explore a hierarchy of Scottish government agencies

;; One of the [CTC24](https://codethecity.org/what-we-do/hack-weekends/ctc24-open-in-practice/) 
;; projects modelled the whole of government in Scotland on Wikidata,
;; culminating in [this SPARQL query](https://w.wiki/4TpN) to show the results.

;; This namespace explores that SPARLQ query and **...TODO**

;; Load Jack Rusher's query abstraction over Wikidata.
(ns scotgov-hierarchy
  (:require [mundaneum.query :as wd]
            [backtick :as b]
            [tablecloth.api :as tc]
            [nextjournal.clerk :as clerk]))


(def queries 
  
  {:exec-agency         '[[?item (wdt :instance-of) (entity "executive agency in the Scottish government")]]
   
   :non-dept-publ-body  '[[?item (wdt :instance-of) (entity "non-departmental public body")
                           _ (wdt :applies-to-jurisdiction) (entity "Scotland")]
                          :minus [[?item (wdt :instance-of) (entity "General lighthouse authority")]]]
   
   :commission          '[[?item (wdt :instance-of) (entity "commission" :office-held-by-head-of-the-organization (entity "commissioner"))
                           _ (wdt :applies-to-jurisdiction) (entity "Scotland")]]
   
   :commissioner        '[[?item (wdt :instance-of) (entity "commissioner")
                           _ (wdt :applies-to-jurisdiction) (entity "Scotland")]]
   
   :publ-corp           '[[?item (wdt :instance-of) (entity "state-owned enterprise")
                           _ (wdt :owned-by) (entity "Scottish Government")]]
   
   :parole-board        '[[?item (wdt :instance-of) (entity "parole board")
                           _ (wdt :applies-to-jurisdiction) (entity "Scotland")]]
   
   :tribunal            '[[?item (wdt :instance-of) (entity "tribunal")
                           _ (wdt :applies-to-jurisdiction) (entity "Scotland")]
                          :minus [[?item (wdt :applies-to-jurisdiction) (entity "England and Wales")]]]
   
   :non-minstr-gov-dept '[[?item (wdt :instance-of) (entity "non-ministerial government department")]
                          :union [[?item (wdt :applies-to-jurisdiction) (entity "Scotland")]
                                  [?item (wdt :located-in-the-administrative-territorial-entity) (entity "Scotland")]]
                          :filter (?item != (entity "Forestry Commission" :instance-of (entity "non-ministerial government department")))]
   
   :gov-agency          '[[?item (wdt :instance-of) (entity "government agency")]
                          :union [[?item (wdt :applies-to-jurisdiction) (entity "Scotland")]
                                  [?item (wdt :located-in-the-administrative-territorial-entity) (entity "Scotland")]]]
   
   :queen-printer       '[[?item (wdt :instance-of) (entity "Queen's Printer")]]
   
   :council             '[[?item (wdt :instance-of) (entity "Scottish unitary authority council")]]
   
   :nhs-board           '[[?item (wdt :instance-of) (entity "NHS board")]]
   
   :health-partnership  '[[?item (wdt :instance-of) (entity "Health and Social Care Partnership")]]

   :court               '[[?item (wdt :part-of) (entity "Courts of Scotland")]]})


(def DS (-> (for [[id where-clause] queries]
              (->
               (b/template
                [:select distinct ?item ?itemLabel
                 :where ~(conj where-clause
                               '[:minus [[?item (wdt :dissolved-abolished-or-demolished) ?_]]])
                 :limit 300])
               wd/query
               tc/dataset
               (tc/add-column :from-query id)))
            (->> (apply tc/concat))))

;; expect 244
(tc/shape DS)

;; de-dup
(def DS' (-> DS
             (tc/fold-by [:item :itemLabel])))

;; those arising from > 1 query
(-> DS'
    (tc/select-rows #(-> % :from-query count (> 1))))

;; expect 238
(tc/shape DS')

;; display as a table
(->> (tc/rows DS' :as-maps)
     (sort-by :itemLabel)
     clerk/table)









