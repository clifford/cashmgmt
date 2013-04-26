(ns cashmgmt.acc-dsl
  (:use [datomic.api :only [q db] :as d]
        [clojure.pprint])
  (:require [datomic.samples.repl :as util]
            [clojure.java.io :as io]
            [datomic.samples.io :as dio]
            [datomic.samples.query :as qry])
  (:import [java.lang Exception]))

;; xyz -- operational -- service accs (fees, commission, ...)
;;                    -- trade accs
;;                    -- control accs
;;     -- client/liability accs


(def conn (util/scratch-conn))
(def schema (read-string (slurp (io/resource "cashmgmt/cashmgmt-schema.edn"))))
;; (def schema (read-string (slurp  "resources/cashmgmt/cashmgmt-schema.edn")))
schema
(println "********************" (dio/read-all schema))
(d/transact conn schema)

(defn create-acc [conn ref bal]
  (let [txid (d/tempid :db.part/tx)]
   (d/transact conn [
                     { :db/id txid
                      :account/reference ref
                      :account/tag :account.tag/avail
                      :account/instrument :account.instrument/p1
                      :account/balance bal
                      :account/min-balance -2M
                      :account/max-balance 15M}
                     ])))

@(create-acc conn "a123" 10M)
(create-acc conn "a456" 7M)

(def a123 (first (first (q '[:find ?e :where [?e :account/reference "a123"]] (db conn)))))
(def a456 (first (first (q '[:find ?e :where [?e :account/reference "a456"]] (db conn)))))

(defn balances [acc-ref]
  (d/q '[:find ?b ?mnb ?mxb :in $ ?r :where
        [?e :account/reference ?r]
        [?e :account/balance ?b]
        [?e :account/min-balance ?mnb]
        [?e :account/max-balance ?mxb]] (db conn) acc-ref))
(balances "a123")



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

;; validate that the amt will keep balance within bounds. adjust the
;; balance for the account
(def credit
  #db/fn {:lang :clojure
                             :params [db id amount]
                             :code (let [e (d/entity db id)
                                         forecast-bal (+ (:account/balance e 0) amount)]
                                     (if-let [errors (d/invoke db :account/balance-checker db id amount)]
                                       (throw ex-info "validation failed with errors" errors)
                                       [[:db/add id :account/balance forecast-bal]])
                                     )})

(-> (credit (db conn) a123 21M) util/should-throw)
(credit (db conn) a123 3M)

;; db val before adjusting balance
(def dbval (db conn))

;; install the credit fn into the db
(d/transact conn [{:db/id (d/tempid :db.part/user)
                   :db/ident :account/credit
                   :db/fn credit}])

(d/transact conn [[:account/credit a123 -1M]])
(balances "a123")

;; transfer implemented as an ordinary function
(defn transfer [db from to amount note]
  (let [txid (d/tempid :db.part/tx)]
    (d/transact db [[:account/credit from (- amount)]
                      [:account/credit to amount]
                      [:db/add from :account/transactions txid]
                      [:db/add to :account/transactions txid]
                      {:db/id txid :ot/note note :ot/dr from :ot/cr to
                       :ot/txtype :ot.txtype/capital :ot/amount amount}])))

(transfer conn a456 a123 1M "pay debt")
(balances "a123")
(balances "a456")

;; find all ot's for an account
(defn txns [acc-ref]
  (map (fn [[txid txinstant]]
         (let
             [tx (d/entity (db conn)  txid)
              {amt :ot/amount note :ot/note when :ot/tx dr :ot/dr cr :ot/cr} tx]
           [dr cr amt note txinstant])
         ) (q '[:find ?txns ?when
        :in $ ?ar
        :where
                [?a :account/reference ?ar]
                [?a :account/transactions ?txns ?tx]
                [?tx :db/txInstant ?when]]
      (db conn)
      acc-ref)))

(txns "a123")

(def v (q '[:find ?txns ?when
      :in $ ?ar
      :where
      [?a :account/reference ?ar]
      [?a :account/transactions ?txns ?tx]
      [?tx :db/txInstant ?when]]

    (db conn)
    "a123"))
v

(map (fn [[txid when]] (println txid)) v)

;; install dbtransfer fn into db
(d/transact conn [{:db/id (d/tempid :db.part/user)
                   :db/ident :account/dbtransfer
                   :db/fn dbtransfer}])
(d/transact conn [[:account/dbtransfer dbval a123 a456 2M]])

;; book an operational transaction, which in turn adjusts the balance
(def book-ot
  "book an operational transaction, which should adjust both dr and cr side balances."
  #db/fn {:lang :clojure
          :params [db id amt dr-acc cr-acc tx-type note]
          :code (let [otid (d/tempid :db.part/tx)]
                  [[:credit ]
                   [:db/add dr :account/transactions db otid]
                   [:db/add cr :account/transactions db otid]
                   ])
          })


(def tx-instants (reverse (sort (d/q '[:find ?when :where [_ :db/txInstant ?when]]
                                     (d/db conn)))))
tx-instants



(pprint (sort #(compare (last %1) (last %2))
         (seq (q '[:find ?e ?b ?t ?tx
                    :in $
                    :where
                    [?tx :db/txInstant ?t]
                    [?e :account/balance ?b ?tx]]
                  (-> (db conn)
                      ;;                    (d/since #inst "2013-03-16T11:34:15.925-00:00")
                      (d/since #inst "2013-03-16")
                      )))))

(defn balance-deltas-since [t]
  (let [db (-> (db conn) (d/since t))]
    (seq (q '[:find ?e ?b ?t
              :where
             [?tx :db/txInstant ?t]
             [?e :account/balance ?b ?tx]]
           db))))

(pprint (balance-deltas-since 13194139534320))

(distinct (map :e (d/datoms (d/since (db conn) #inst "2013-03-16T11:34:15.925-00:00") :eavt)))

;; now use clojure threading macros
(def changed (->> (-> (db conn)
          (d/since #inst "2013-03-16T11:34:15.925-00:00")
          (d/datoms :eavt))
      (map :e)
      distinct))

(pprint (map #(seq (d/entity (db conn) %)) changed))
