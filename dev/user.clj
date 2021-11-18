(ns user
  (:require [nextjournal.clerk :as clerk]))

(comment
  ;; start without file watcher, open browser when started
  (clerk/serve! {:browse? true})

  ;; start with file watcher
  (clerk/serve! {:watch-paths ["src" #_"other_src"]})

  ;; start with file watcher and show filter function to enable notebook pinning
  (clerk/serve! {:watch-paths ["src" #_"other_src"] :show-filter-fn #(clojure.string/starts-with? % "literate_programming")})

  ;; open clerk
  (browse/browse-url (str "http://localhost:" port))

  ;; or call `clerk/show!` explicitly
  (clerk/show! "src/exploring_business_waste.clj")

  (clerk/clear-cache!)

  ;; produce a static app
  (clerk/build-static-app! {:paths (mapv #(str "src/" % ".clj")
                                         '[exploring_business_waste])})

  )
