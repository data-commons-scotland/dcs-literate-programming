(ns user
  (:require [clojure.java.browse :as browse]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.viewer :as v]))

(comment

  ;; start without file watcher, open browser when started
  (clerk/serve! {:browse? true})

  ;; start with file watcher
  (clerk/serve! {:watch-paths ["src" #_"other_src"]})

  ;; start with file watcher and show filter function to enable notebook pinning
  (clerk/serve! {:watch-paths ["src" #_"other_src"]
                 :show-filter-fn #(clojure.string/starts-with? % "literate_programming")})

  ;; open clerk
  (browse/browse-url "http://localhost:7777")

  ;; or call `clerk/show!` explicitly
  (clerk/show! "src/exploring_business_waste.clj")
  (clerk/show! "src/household_vs_business_waste.clj")

  (clerk/clear-cache!)

  ;; Clerk elides lists after the 20th element; show and tweak the eliding parameter :n
  (-> @v/!viewers :root (get 10) :fetch-opts :n)
  (swap! v/!viewers update-in [:root 10 :fetch-opts] #(assoc % :n 35))

  ;; generate a 'static app'
  (clerk/build-static-app! {:paths (mapv #(str "src/" % ".clj") 
                                         '[exploring_business_waste
                                           household_vs_business_waste])})

  )
