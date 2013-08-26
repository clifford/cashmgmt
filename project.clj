(defproject cashmgmt "1.0.0-SNAPSHOT"
  :description "cash management"
  :plugins [[lein-tg "0.0.1"]]

  :profiles {:dev {:resource-paths ["resources"]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]
                                  [day-of-datomic "1.0.0-SNAPSHOT"]
                                  [org.clojure/data.generators "0.1.0"]
                                  [org.clojure/test.generative "0.1.4"]]
                   }}
 )
