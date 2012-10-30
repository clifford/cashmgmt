(defproject rules "1.0.0-SNAPSHOT"
  :description "rules engine"
  :dependencies [[org.clojure/clojure "1.5.0-alpha3"]
                 [org.clojure/core.match "0.2.0-alpha11"]
                 [com.datomic/datomic-free "0.8.3488"]
                 [org.clojure/data.json "0.1.1"]
                 [org.clojure/core.logic "0.8.0-beta1"]]
  :source-paths ["src/clj"]
  :dev-dependencies [[midje "1.4.0"]])


