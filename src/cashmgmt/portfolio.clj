(ns cashmgmt.portfolio
  (:require [datomic.api :only [q db] :as d]
            [datomic.samples.repl :as u]))

(def conn (u/scratch-conn))

(def schema [{:db/id #db/id [:db.part/db]
              :db/ident :portfolio/reference
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/index true
              :db/unique :db.unique/identity
              :db/fulltext true
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db]
              :db/ident :portfolio/accounts
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/many
              :db.install/_attribute :db.part/db}
             {:db/id #db/id [:db.part/db]
              :db/ident :portfolio/portfolios
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/many
              :db.install/_attribute :db.part/db}])
(defn create [p]
  "create a portfolio"
  )




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
