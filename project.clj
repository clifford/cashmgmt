(defproject cashmgmt "1.0.0-SNAPSHOT"
  :description "cash management"
  :plugins [[lein-tg "0.0.1"]]
  :dependencies [[org.clojure/clojure "1.5.0-RC4"]
                 [org.clojure/test.generative "0.3.0"]
                 [com.datomic/datomic-free "0.8.3784"]
                 [incanter/incanter-charts "1.3.0"]
                 [incanter/incanter-pdf "1.3.0"]
                 [day-of-datomic "1.0.0-SNAPSHOT"]]
  :profiles {:dev {:resource-paths ["resources"]}} )
