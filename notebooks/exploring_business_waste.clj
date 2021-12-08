;; # Exploring Scotland's business waste data

;; We are going to take the CSV data file
;; that describes the waste generated by businesses in Scotland during the last decade, 
;; and walk through how to extract information from it
;; to provide a comparison between business sectors.
;;
;; _This walk-through is pitched at the data analyst beginner level.
;; We use the Clojure programming language,
;; but the steps taken would have be very similar if we'd have used R or Python._

;; Firstly, load a couple of helper libraries.
(ns exploring-business-waste
  (:require [tablecloth.api :as tc]
            [nextjournal.clerk.viewer :as v]))

;; ## Load the data

;; The URL of Scotland's business waste data (including business sectors) in CSV format
;; (from the "[Easier open data about waste in Scotland](https://github.com/data-commons-scotland/dcs-easier-open-data)" repository).
(def url "https://raw.githubusercontent.com/data-commons-scotland/dcs-easier-open-data/v1.5-beta/data/business-waste-by-sector.csv")

;; Read the data into a column-oriented data structure
;; (that is similar to an R Data Frame or a Python Pandas DataFrame).
;;
;; _Browsing hint: Click on the underlined backets within following the outputs, to expand what is shown._
(def DS (tc/dataset url {:key-fn keyword}))

;; Count the rows and columns.
(tc/shape DS)

;; Get a further description of the data.
(tc/info DS)

;; ...The `n-missing` and `n-valid` values indicate that there are no _holes_ in the data.

;; ## Augment the data

;; Calculate _totals_: fabricate an `All wastes` row for each `business-sector` & `year` pair.

;; Define a helper function...
;; * that given data about a `business-sector` and `year`,
;; * will calculate the _total_ `tonnes`, and
;; * will fabricate an `All wastes` row to store it in.
(defn ->total [[row-template ds-sector-and-year]]
  (assoc row-template
         :material "All wastes"
         :tonnes (apply + (ds-sector-and-year :tonnes))))

;; Calculate _totals_, and store them in a new dataset.
(def DS-totals 
  (let [ds-sectors-and-years (-> DS
                                 (tc/group-by [:business-sector :year])
                                 tc/groups->map)]
    (->> ds-sectors-and-years
         (map ->total)
         tc/dataset)))

;; Concatenate the original data and _totals_ data.
(def DS-augmented (tc/concat DS DS-totals))

;; ## Display the data to glean some information

;; For each waste material, plot a graph that will surface information about
;; the amounts of the waste material produced year-on-year by each business sector.
;; This should help us compare waste between business sectors, and identify trends. 

;; Specify the layout basics for the graphs. 
(def layout {:xaxis        {:automargin true
                            :title      {:text     "year"
                                         :standoff 0.1}}
             :yaxis        {:title      "tonnes"
                            :automargin true}
             :legend       {:orientation "h"
                            :font        {:size 10}
                            :xanchor     "bottom"
                            :y           -0.2}
             :plot_bgcolor "#fff1e5"
             :paper_bgcolor "floralwhite"
             })

;; Define a helper function...
;; * that given data about a `business-sector`,
;; * will create a plotline that depicts its tonnes of waste per year. 
(defn ->plotline [[sector ds-sector]]
  {:name          sector
   :x             (-> ds-sector :year vec)
   :y             (-> ds-sector :tonnes vec)
   :hovertemplate (str "<b>" sector "</b> (%{x})<br>"
                       "%{yaxis.title.text}: %{y}<extra></extra>")})

;; Define a helper function...
;; * that given data about a `material`,
;; * will plot a graph that contains plotlines ...one for each business-sector. 
(defn ->graph [i [material ds-material]]
  (let [material' (if (> (count material) 61)
                    (str (subs material 0 61) "...")
                    material)
        ds-sectors (-> ds-material
                       (tc/order-by :year)
                       (tc/group-by :business-sector)
                       tc/groups->map)
        plotlines  (->> ds-sectors
                        (map ->plotline)
                        vec)]
    (v/plotly {:data   plotlines
               :layout (assoc layout :title (str (inc i) " - " material'))})))
 
;; Display the graphs.
;;
;; _Browsing hints: Hover-over/click-on graph points to pop-up their details, and
;; (re-)click on legend entries to hide/show their plotlines._
(let [ds-materials (-> DS-augmented
                       (tc/group-by :material)
                       tc/groups->map)]
  (->> ds-materials
       (map-indexed ->graph)))

;; ## What information has been surfaced?

;; The graphs provide at-a-glance-information about the relative _performances_ 
;; of the business sectors in terms of amounts of waste generated and trends.
;; And, they make it easier to spot _anomalies_...
;;
;; * The signature of Longannet's closure: 
;;   Longannet was a coal fuelled power station that closed on 24th March 2016.
;;   It was a significant generator of waste so its signature can be spotted 
;;   at the `All wastes` level of graph 1 (hide its `Commerce` plot line to better see its
;;   `Power industry` plotline nose dive between 2015 and 2017).
;;   And graph 23 singles out `Combustion wastes`, where it is even easier to spot.
;;   (Furthermore, Longannet features in [this map-oriented data analysis](https://github.com/data-commons-scotland/dcs-shorts/tree/master/longannet-found-in-the-data)).
;;   
;; * There is a x10 spike in graph 29's `Acid, Alkaline or saline waste` due to
;;   `Mining and quarrying` in the year 2014. _Why?_ 
;;   [This 13th July 2014 article](https://www.heraldscotland.com/news/13169742.revealed-national-crisis-opencast-mine-warning/)
;;   in The Herald newspaper, reports on the threat from contaminated water in Scotland's 
;;   abandoned coal mines, and that Scottish Coal went bust in 2013.
;;   Perhaps this waste was accounted for only after Scottish Coal's demise, hence the spike in 2014(?).
;;
;; * More spikes: Actually, there is a lot of _spike-y_ data in the graphs.
;;   Some graphs have obvious spikes, 
;;   such as those for `Sludges and liquid wastes from waste treatment` in graph 34.
;;   In other graphs, spikes are noticable only after hiding the plotlines of
;;   the higher waste generating sectors. For example, on graph 19 `Rubber wastes`,
;;   double click in the legend on `Manufacture of food and beverage products` 
;;   to bring this to the fore. It is zero for all years apart from 2017. _Why?_
;;   And the same question for the other spikes!

;; ## Source code

;; The source code for this page is at:
;; [https://raw.githubusercontent.com/data-commons-scotland/dcs-literate-programming/master/notebooks/exploring_business_waste.clj](https://raw.githubusercontent.com/data-commons-scotland/dcs-literate-programming/master/notebooks/exploring_business_waste.clj)

