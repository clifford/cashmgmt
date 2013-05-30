(defproject cashmgmt "1.0.0-SNAPSHOT"
  :description "cash management"
  :plugins [[lein-tg "0.0.1"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.8.3848"]
                 [incanter/incanter-charts "1.3.0"]
                 [incanter/incanter-pdf "1.3.0"]
                 [day-of-datomic "1.0.0-SNAPSHOT"]
                 [org.clojure/math.combinatorics "0.0.4"]
                 [org.clojure/test.generative "0.1.4"]
                 [com.datomic/simulant "0.1.4"]
                 [clj-time "0.5.1"]]
  :profiles {:dev {:resource-paths ["resources"]}}
)
