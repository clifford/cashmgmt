

(ns cashmgmt.accountold
  (:use [datomic.api :only [q db] :as d])
  (:require [cashmgmt.query :as query]
            [clojure.java.io :as io]
            [cashmgmt.transact :as t])
  (:import [java.util Date]))

(def uri "datomic:mem://accounts")

;; create database
(d/create-database uri)

;; connect to database
(def conn (d/connect uri))

;; parse schema dtm file
(def schema-tx (read-string (slurp "/Users/dev/src/clj/acc/schema.dtm")))

schema-tx
;; submit schema transaction
@(d/transact conn schema-tx)

;; parse seed data dtm file
(def data-tx (read-string (slurp "/Users/dev/src/clj/acc/accounts.dtm")))

;; submit seed data transaction
@(d/transact conn data-tx)

;;(t/install conn data-tx :account/name)

(defn accounts []
  (d/q '[:find ?c ?n ?b ?count :where [?c :account/name ?n] [?c :account/balance ?b] [?c :account/transaction-count ?count]] (db conn)))

(def all-acc-txs '[:find ?tx ?f ?t ?a ?n ?tx-type-name ?when
               :in $
               :where
                   [?from :account/name ?f]
                   [?to :account/name ?t]
                   [?tx :ot/from ?from]
                   [?tx :ot/to ?to]
                   [?tx :ot/amount ?a]
                   [?tx :ot/note ?n]
                   [?tx :ot/txtype ?type]
                   [?type :db/ident ?tx-type-name]
                   [?tx :db/txInstant ?when]])

(def acc-txs '[:find ?tx ?f ?t ?a ?n ?tx-type-name ?when
               :in $ ?e
               :where
               [?from :account/name ?f]
               [?to :account/name ?t]
               [?tx :ot/from ?from]
               [?tx :ot/to ?to]
               [?tx :ot/amount ?a]
               [?tx :ot/note ?n]
               [?tx :ot/txtype ?type]
               [?type :db/ident ?tx-type-name]
               [?tx :db/txInstant ?when]
               [?e :account/transactions ?tx]])

(defn history
  ([] (q all-acc-txs (db conn)))
  ([acct] (q acc-txs (db conn) acct) ))

(defn transfer [ from to amount note]
  (let [txid (datomic.api/tempid :db.part/tx)]
    (d/transact conn [[:transfer from to amount]
                      [:db/add from :account/transactions txid]
                      [:db/add to :account/transactions txid]
                      [:inc from :account/transaction-count 1]
                      [:inc to :account/transaction-count 1]
                      {:db/id txid, :ot/note note :ot/from from
                       :ot/to to :ot/amount amount :ot/txtype :ot.txtype/capital}])))

(defn credit [ to amount ]
  (d/transact conn [[:credit to amount]]))


(def issuer (first (first (q '[:find ?e :where [?e :account/name "issuer"]] (db conn)))))
(def bob (first (first (q '[:find ?e :where [?e :account/name "bob"]] (db conn)))))
(def alice (first (first (q '[:find ?e :where [?e :account/name "alice"]] (db conn)))))

(prn (accounts))
(transfer issuer alice 77M "Issuance to Alice")
(transfer issuer bob 23M "Issuance to Bob")
(transfer alice bob 7M "Tomatoes")

;; get back fields as map
;(into {} (map vector [:eid :from :to :amt :note :type :when] (first (seq history))))

(println "History")
(prn (history))
;; #<HashSet [[13194139534319 "issuer" "alice" 77M "Issuance to Alice" #inst "2012-05-08T14:35:25.252-00:00"], 
;;            [13194139534320 "issuer" "bob" 23M "Issuance to Bob" #inst "2012-05-08T14:35:25.256-00:00"], 
;;            [13194139534321 "alice" "bob" 7M "Tomatoes" #inst "2012-05-08T14:35:25.262-00:00"]]>

(println "Accounts")
(prn (accounts))
;; #<HashSet [[17592186045421 "issuer" -100M 2], [17592186045423 "alice" 70M 2], [17592186045422 "bob" 30M 2]]>

(defn alice-txs [db entity attr] (q '[:find ?n ?amt ?tn
                        :in $ ?e ?attr 
                        :where
                        [?e :account/transactions ?tx]
                        [?tx ?attr ?e]
                        [?tx :ot/note ?n ]
                        [?tx :ot/amount ?amt]
                        [?tx :ot/txtype ?t]
                        [?t :db/ident ?tn]]
                      db entity attr))
(alice-txs (db conn) alice :ot/from)
(alice-txs (db conn) alice :ot/to)


(defn to-map
  ([history]
     (to-map history [:eid :from :to :amt :note :type :when]))
  ([history keys]
     (loop [h history
            accum '()
            ks keys]
                                        ;    (println h accum)
       (if (> (count h) 0)
         (recur (rest h) (conj accum (into {} (map vector ks (first h)))) ks)
         accum))))

(to-map (history))
(first (to-map (history)))
(map :when (to-map (history)))



;(let [{:keys [exit out err] :as result} (apply sh/sh args)])
(let [{:keys [eid when] :as res} (first (to-map (history)))]
  (println eid when res))

(defn extract-keys [keys history]
  (loop [h (seq history)
         accum ()
         ks (into [] (map (comp symbol name) keys))]
    (prn ks)
    (if (> (count h) 0)
      (let [{:keys [eid when] :as res} (first h)]
        (prn h eid when)
        (recur (rest h) (conj accum eid when) ks))
      accum)))

(defn extract-keys-2 [keys history]
  (loop [h (seq history)
         accum ()]
    (if (> (count h) 0)
      (let [kys (select-keys (first h) keys)]
        (prn kys)
        (recur (rest h) (conj accum (vals kys))))
      accum)))

;; (defn extract-keys-3 [keys history]
;;   (loop [h (seq history)
;;          accum ()
;;          ks (into [] (map (comp symbol name) keys))]
;;     (prn ks)
;;     (if (> (count h) 0)
;;       (let [kmap (my-zipmap ks keys)
;;             kmap (first h)]
;;         (prn kmap eid when)
;;         (recur (rest h) (conj accum eidv whenv) ks))
;;       accum)))

(extract-keys-2 [:eid :when] (to-map (history)))
(extract-keys ["eid" "when"] (to-map (history)))
(count ( history))

(defn my-zipmap [keys vals]
  (loop [my-map {}
         my-keys (seq keys)
         my-vals (seq vals)]
    (if (and my-keys my-vals)
      (recur (assoc my-map (first my-keys) (first my-vals))
             (next my-keys)
             (next my-vals))
      my-map)))
(my-zipmap [:a :b :c] [1 2 3])

