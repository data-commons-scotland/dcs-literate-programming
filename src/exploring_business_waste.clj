;; # Exploring Scotland's business waste data

;; This is a _data-analytics-y_ (but basic!) exploration 
;; of the data that describes the waste generated by businesses in Scotland.
;;
;; We're going to extract information about the waste generated by Scotland's business sectors,
;; to provide a comparison between them.
;;
;; We use the Clojure programming language,
;; but the steps taken would have be very similar if we'd have used R or Python.

;; Load the helper libraries.
(ns exploring-business-waste
  (:require [tablecloth.api :as tc]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

#_(clerk/serve! {:watch-paths ["src"]})
#_(nextjournal.clerk/show! "src/exploring_business_waste.clj")
#_(clerk/clear-cache!)
#_(-> @v/!viewers :root (get 10) :fetch-opts :n)
#_(clerk/build-static-app! {:paths (mapv #(str "src/" % ".clj") '[exploring_business_waste])})

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
(defn ->graph [[material ds-material]]
  (let [material' (if (> (count material) 65)
                    (str (subs material 0 65) "...")
                    material)
        ds-sectors (-> ds-material
                       (tc/order-by :year)
                       (tc/group-by :business-sector)
                       tc/groups->map)
        plotlines  (->> ds-sectors
                        (map ->plotline)
                        vec)]
    (v/plotly {:data   plotlines
               :layout (assoc layout :title material')})))

;; (In the 'Display the graphs' step below, we want to display a list of 34 graphs - one for each `material`.
;; And we're using the [Clerk](https://github.com/nextjournal/clerk) 
;; [literate programming](https://en.wikipedia.org/wiki/Literate_programming) tool to display things on this webpage.
;; But Clerk elides lists after the 20th element, so we need to tweak its eliding parameter `n`.) 
(swap! v/!viewers update-in [:root 10 :fetch-opts] #(assoc % :n (-> DS-augmented :material distinct count)))
  
;; Display the graphs.
;;
;; _Browsing hints: Hover-over/click-on graph points to pop-up their details, and
;; (re-)click on legend entries to hide/show their plotlines._
(let [ds-materials (-> DS-augmented
                       (tc/group-by :material)
                       tc/groups->map)]
  (->> ds-materials
       (map ->graph)))

;; ## What information has been surfaced?
;;
;; NOTE: This section is still *work-in-progress*.
;;
;; Say somethings about what we can learn from the graphs
;; * see relative amounts, and trends
;; * spot erratic plotlines (some obvious, some obvious only after hiding the top plotlines)
;; * Longannet
;;
;; Say something about the tech
;; * The source code behind this page is at 
;;    [https://raw.githubusercontent.com/data-commons-scotland/dcs-literate-programming/master/src/exploring_business_waste.clj](https://raw.githubusercontent.com/data-commons-scotland/dcs-literate-programming/master/src/exploring_business_waste.clj)
;;

