;; # Comparing Scotland's household waste against its business waste

;; We are going to take the two CSV data files that describe the waste generated by 
;; households and by businesses in Scotland during the last decade,
;; and walk through how to:
;; 1. load their data, 
;; 2. aggregate to yearly totals,
;; 3. apportion yearly percentages, and
;; 4. plot the results in a graph for comparison.

;; Firstly, load a couple of helper libraries.
(ns household-vs-business-waste
  (:require [tablecloth.api :as tc]
            [nextjournal.clerk.viewer :as v]))

;; ## 1. Load the data

;; URL to the context containing the data files 
;; (within the "[Easier open data about waste in Scotland](https://github.com/data-commons-scotland/dcs-easier-open-data)" repository).
(def ctx-url "https://raw.githubusercontent.com/data-commons-scotland/dcs-easier-open-data/v1.6-beta/data/")

;; Read each data file into a column-oriented data structure
;; (similar to an R Data Frame or a Python Pandas DataFrame).
;;
;; _Browsing hint: Click on the underlined backets within following the outputs, to expand what is shown._
(def household (tc/dataset (str ctx-url "household-waste.csv") {:key-fn keyword}))
(def business (tc/dataset (str ctx-url "business-waste-by-sector.csv") {:key-fn keyword}))

;; ## 2. Aggregate to yearly totals

;; Define a helper function that aggregates to yearly totals, the given dataset.
(defn ->totals [data]
  (let [groups (-> data
                   (tc/group-by :year)
                   tc/groups->map)
        totals (->> groups
                    (map (fn [[year group]]
                           {:year   year
                            :tonnes (apply + (group :tonnes))}))
                    tc/dataset)]
    (tc/order-by totals :year)))

;; Apply the helper function to each dataset.
(def household-totals (->totals household))
(def business-totals (->totals business))

;; ## 3. Apportion yearly percentages

;; Combine the datasets so that percenatges can be calculate and apportioned.
(def all-percentages
  (-> (tc/full-join household-totals business-totals :year)
      (tc/map-columns :year 
                      [:year :right.year]
                      #(or %1 %2))
      (tc/map-columns :combined-tonnes 
                      [:tonnes :right.tonnes] 
                      #(when (and %1 %2) (+ %1 %2)))
      (tc/map-columns :household-percentage
                      [:tonnes :combined-tonnes] 
                      #(when (and %1 %2) (* (/ %1 %2) 100)))
      (tc/map-columns :business-percentage
                      [:right.tonnes :combined-tonnes]
                      #(when (and %1 %2) (* (/ %1 %2) 100))))) 

;; Segregate and simpify.
(def household-percentages (-> all-percentages
                               (tc/select-columns #{:year :household-percentage})
                               (tc/rename-columns {:household-percentage :percentage})))
(def business-percentages (-> all-percentages
                              (tc/select-columns #{:year :business-percentage})
                              (tc/rename-columns {:business-percentage :percentage})))

;; ## 4. Plot in a graph for comparison

;; Define a helper function that builds a plotline, from the data.
(defn ->plotline [name colour totals percentages]
  {:name          name
   :x             (-> totals :year vec)
   :y             (-> totals :tonnes vec)
   :line          {:color colour :width 3}
   :customdata    (->> percentages 
                       :percentage 
                       (map #(if (nil? %) "n/a" (format "%.1f%%" (double %)))) 
                       vec)
   :hovertemplate (str "<b>" name "</b> (%{x})<br>"
                       "%{yaxis.title.text}: %{y}<br>"
                       "portion of total: %{customdata}<extra></extra>")})

;; Display the graph.
(v/plotly 
 {:data   [(->plotline "household waste" "#C9A9A6" household-totals household-percentages)
           (->plotline "business waste" "#5b5a57" business-totals business-percentages)]
           :layout {:title "Household-vs-business wastes in Scotland"
                    :height 400 :margin {:l 110 :b 40}
                    :xaxis {:title "year" :showgrid false :dtick 1}
                    :yaxis {:title "tonnes" :tickformat "," :rangemode "tozero"}
                    :legend {:traceorder "reversed"}
                    :plot_bgcolor "#fff1e5" :paper_bgcolor "floralwhite"}})

;; ## Concluding remarks

;; The graph provides an at-a-glance-comparison between Scotland's households and businesses
;; in respect of the yearly amounts waste that they have generated during the last decade.
;;
;; The _household:business_ ratio has been very approximately _2:3_,
;; but waste from businesses has been reducing noticably over the decade.
;;
;; (Figures for business waste in 2019, haven't yet been released.)

;; ## Source code

;; The source code for this page is at:
;; [https://raw.githubusercontent.com/data-commons-scotland/dcs-literate-programming/master/src/household_vs_business_waste.clj](https://raw.githubusercontent.com/data-commons-scotland/dcs-literate-programming/master/src/household_vs_business_waste.clj)