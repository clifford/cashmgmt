(ns cashmgmt.portfolio
  (:require [datomic.api :only [q db] :as d]
            [datomic.samples.repl :as u]
            [clojure.java.io :as io]
            [cashmgmt.acc-dsl :as a]
            [cashmgmt.introspect :as i]
            [datomic.samples.query :as qry]
            [cashmgmt.util.unique :as uq :reload true]))

(def conn (u/scratch-conn))

(def schema (read-string (slurp (io/resource "cashmgmt/cashmgmt-schema.edn"))))
(d/transact conn schema)

(defn create [ref accs]
  "create a portfolio"
  (let [txid (d/tempid :db.part/tx)]
    (d/transact conn [
                      {:db/id txid
                       :portfolio/reference ref
                       :portfolio/position accs}])))

(create "p1" [])
@(a/create-acc conn "a123" 10M)
(a/create-acc conn "a456" 7M)
(def a123 (first (first (d/q '[:find ?e :where [?e :account/reference "a123"]] (d/db conn)))))
(def a456 (first (first (d/q '[:find ?e :where [?e :account/reference "a456"]] (d/db conn)))))
(:db/id a123)
@(create "p2" [a123 a456])

;; verify that portfolio contains the account
(i/portfolio-accs conn "p2" (partial map (fn [[k v]] (format "%s -> %s" k v))))
(def accs (i/portfolio-accs conn "p2" (let [r {}] (partial map (fn [[k v]] (assoc r k v))))))
(first accs)
(second accs)

(a/transfer a123 a456 33M "cig annuity")
(a/transfer a123 a456 17M "repay debt")

;; instrument
(defn create-instrument [conn instr types]
  (let [txid (d/tempid :db.part/tx)]
    (d/transact conn [{:db/id txid
                       :instrument/reference instr}
                      [:db/add txid :instrument/type types]
                      ;; (map #(:db/add txid :instrument/type %) types)
                      ])))
@(create-instrument conn "b133" :fixed-income)
(d/q '[:find ?t
       :where
       [?e :instrument/type ?t]
       [?e :instrument/reference "b133"]]
     (d/db conn))

(ffirst (d/q '[:find ?e :where [?e :instrument/reference "b133"]] (d/db conn)))



;; quote
(defn create-quote [])

(defn list-existing-values-e
  "list existing values for entity"
  [r db attr]
  (->> (d/q '[:find ?val
              :in $ ?attr ?r
              :where
              [?e ?attr ?val]
              [?e :instrument/reference ?r]]
            db attr r)
       (map first)
       ))

(defn list-existing-values
  "Returns subset of values that already exist as unique
   attribute attr in db"
  [db attr]
  (->> (d/q '[:find ?val
              :in $ ?attr ;;[?val ...]
              :where [_ ?attr ?val]]
            db attr)
       (map first)
       ))

(list-existing-values (d/db conn) :instrument/type)
(list-existing-values-e "b133" (d/db conn) :instrument/type)
(uq/existing-values (d/db conn) :instrument/type [:bond :fixed-income :x987])
(let [txid (d/tempid :db.part/tx)
      emap [{:db/id txid
              :instrument/type "x988"}]]
    (uq/assert-new-values conn :db.part/tx :instrument/type emap))

(let [txid (ffirst (d/q '[:find ?e :where [?e :instrument/reference "b133"]] (d/db conn)))
      emap [{:db/id txid
              :instrument/type "x999"}]]
    (uq/assert-new-values conn :db.part/tx :instrument/type emap))

(let [txid (ffirst (d/q '[:find ?e :where [?e :instrument/reference "b133"]] (d/db conn)))]
  (d/transact conn [[:db/add txid :instrument/type "cig1"]]))

(let [txid (ffirst (d/q '[:find ?e :where [?e :instrument/reference "b133"]] (d/db conn)))
      emap1 {:db/id (d/tempid :db.part/tx)
             :instrument/type "a1"}
      emap2 {:db/id (d/tempid :db.part/tx)
             :instrument/type "b1"}]
  (uq/assert-new-values-on conn txid :instrument/type [emap1 emap2] ))
;; position
(defn create-pos [conn acc ])

(comment design
  p ->* p ->* acc
  portfolios are view across accounts, accounts belong to a legal entity and record all flows for an instrument.
  they also indicate the status of the asset (avail, reserved, intransit).
  portfolios can be nested and can track one another
  p(123) -> {:x 100, :y 300}  -> a(a234) {:x 100} a(988) {:y 300}

  acc is a collection of operational transactions (or entries) which when netted return the position in the account

  p (333) -> {:x 100 :y 50}  p (444) tracks p (333), as p (333) changes either through market movements or re-adjustment, so too does p (444)

  !concerns:
  where do positions fit into this model?
  are they needed?
  positions can be thought of as the aggregation of accounts by investor, product provider, desk, book.

  p ->* accs
  p ->* p
  p -> value date
  p ->


)
