(ns cashmgmt.acc-dsl
  (:use [datomic.api :only [q db] :as d])
  (:require [datomic.samples.repl :as util])
  (:import [java.lang Exception]))

;; xyz -- operational -- service accs (fees, commission, ...)
;;                    -- trade accs
;;                    -- control accs
;;     -- client/liability accs 

(def conn (util/scratch-conn))
(def schema [{:db/id #db/id [:db.part/db],
              :db/ident :account/reference,
              :db/valueType :db.type/string,
              :db/cardinality :db.cardinality/one,
              :db/index true,
              :db/unique :db.unique/identity
              :db/fulltext true,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :account/tag,
              :db/valueType :db.type/ref,
              :db/cardinality :db.cardinality/one,
              :db/index true,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :account/instrument,
              :db/valueType :db.type/ref,
              :db/cardinality :db.cardinality/one,
              :db/index true,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :account/balance,
              :db/valueType :db.type/bigdec,
              :db/cardinality :db.cardinality/one,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :account/min-balance,
              :db/valueType :db.type/bigdec,
              :db/cardinality :db.cardinality/one,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :account/max-balance,
              :db/valueType :db.type/bigdec,
              :db/cardinality :db.cardinality/one,
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db],
              :db/ident :balance-setter
              :db/fn #db/fn {:lang :clojure
                             :params [db id amount]
                             :code (let [e (d/entity db id)
                                         min-bal (:account/min-balance e 0)
                                         max-bal (:account/max-balance e 0)
                                         forecast-bal (+ (:account/balance e 0) amount)]
                                     (if (and (>= forecast-bal min-bal) (<= forecast-bal max-bal))
                                       [[:db/add id :account/balance forecast-bal]]
                                       (throw (Exception. (str "illegal balance: " forecast-bal)))))}}
             [:db/add #db/id [:db.part/user] :db/ident :account.tag/avail]
             [:db/add #db/id [:db.part/user] :db/ident :account.tag/reserved]

             [:db/add #db/id [:db.part/user] :db/ident :account.instrument/p1]
             ])
@(d/transact conn schema)

(d/q '[:find ?b :in $ ?r :where [?e :account/reference ?r] [?e :account/balance ?b]] (db conn) "a123")

(let [txid (d/tempid :db.part/tx)]
  (d/transact conn [[:balance-setter txid 11M]]))

(let [txid (d/tempid :db.part/tx)]
  (d/transact conn [
                    { :db/id txid
                      :account/reference "a123"
                      :account/tag :account.tag/avail
                      :account/instrument :account.instrument/p1
                      :account/balance 10M
                      :account/min-balance -2M
                      :account/max-balance 15M}
                    ]))


(def acc-a {:primary "xyz corp"
            })