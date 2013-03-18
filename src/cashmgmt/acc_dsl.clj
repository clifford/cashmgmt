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

             [:db/add #db/id [:db.part/user] :db/ident :account.tag/avail]
             [:db/add #db/id [:db.part/user] :db/ident :account.tag/reserved]

             [:db/add #db/id [:db.part/user] :db/ident :account.instrument/p1]
             ])

@(d/transact conn schema)
(if (>= forecast-bal min-bal) ;;(<= forecast-bal max-bal)
                                       [[:db/add id :account/balance forecast-bal]]
                                       (throw (Exception. (str "illegal balance: " forecast-bal))))
(defn balances [acc-ref]
  (d/q '[:find ?b ?mnb ?mxb :in $ ?r :where
        [?e :account/reference ?r]
        [?e :account/balance ?b]
        [?e :account/min-balance ?mnb]
        [?e :account/max-balance ?mxb]] (db conn) acc-ref))
(balances "a123")

(def a123 (first (first (q '[:find ?e :where [?e :account/reference "a123"]] (db conn)))))

(def balance-checker 
  #db/fn {:lang :clojure
          :params [db id amount]
          :code (let [e (d/entity db id)
                      min-bal (:account/min-balance e 0)
                      max-bal (:account/max-balance e 0)
                      forecast-bal (+ (:account/balance e 0) amount)]
                  (println "forecast-bal: " forecast-bal "min-bal: " min-bal)
                  (cond
                   (< forecast-bal min-bal)
                   {:account-balance [(format "balance: %.2f must be greater than min-balance: %.2f" forecast-bal min-bal)]}
                   (> forecast-bal max-bal)
                   {:account-balance [(format "balance: %.2f must be less than max-balance: %.2f" forecast-bal max-bal)]})
                  )}
  )


;; balance is within bounds, should return nil
(balances "a123")
(balance-checker (db conn) a123 5M)
(balance-checker (db conn) a123 15M)
(balance-checker (db conn) a123 -500M)
(balance-checker (db conn) a123 -2M)

;; add balance-checker to the db
(d/transact conn [{:db/id (d/tempid :db.part/user)
                   :db/ident :account/balance-checker
                   :db/fn balance-checker}])

(def adj-bal-by 
  #db/fn {:lang :clojure
                             :params [db id amount]
                             :code (let [e (d/entity db id)
                                         forecast-bal (+ (:account/balance e 0) amount)]
                                     (if-let [errors (d/invoke db :account/balance-checker db id amount)]
                                       (throw ex-info "validation failed with errors" errors)
                                       [[:db/add id :account/balance forecast-bal]])
                                     )})

(-> (adj-bal-by (db conn) a123 21M) util/should-throw)
(adj-bal-by (db conn) a123 3M)

;; should now have a history of balances for a123

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