(ns rules.core
  (:use    [datomic.api :only [db q] :as d])
  (:import [datomic Peer Util] ;; Imports only used in data loading
           [java.io FileReader]))

;(use '[datomic.api :only [q ab] :as d])
(def uri "datomic:mem//rules")

(def conn (d/connect uri))

(def schema-tx (read-string (slurp "/Users/dev/src/clj/rules/rules.dtm")))

@(d/transact conn schema-tx)

(def data-tx (read-string (slurp "/Users/dev/src/clj/rules/rule-data.dtm")))

@(d/transact conn data-tx)