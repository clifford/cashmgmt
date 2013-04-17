(ns cashmgmt.portfolio
  (:require [datomic.api :only [q db] :as d]
            [datomic.samples.repl :as u]
            [clojure.java.io :as io]
            [cashmgmt.acc-dsl :as a]
            [datomic.samples.query :as qry]))

(def conn (u/scratch-conn))

(def schema (read-string (slurp (io/resource "cashmgmt/cashmgmt-schema.edn"))))
(d/transact conn schema)

(defn create [ref accs]
  "create a portfolio"
  (let [txid (d/tempid :db.part/tx)]
    (d/transact conn [
                      {:db/id txid
                       :portfolio/reference ref
                       :portfolio/accounts accs}])))

(create "p1" [])
@(a/create-acc "a123" 10M)
(def a123 (first (first (d/q '[:find ?e :where [?e :account/reference "a123"]] (d/db conn)))))
(:db/id a123)
@(create "p2" [a123])

;; verify that portfolio contains the account
(def e (d/entity (d/db conn) (ffirst (d/q '[:find ?a :where [?p :portfolio/reference "p2"]
                                      [?p :portfolio/accounts ?a]]
                                    (d/db conn)))))
(:account/tag e)
(:account/balance e)
(defn portfolio-accs [portfolio]
  "describe accounts on this portfolio"
  (->> (qry/qes '[:find ?a
                  :in $ ?portfolio
                  :where [?p :portfolio/reference ?portfolio]
                 [?p :portfolio/accounts ?a]]
               (d/db conn) portfolio)
      ffirst
      (map (fn [[k v]]
             (format "%s -> %s" k v)))
      ))
(portfolio-accs "p2")

(def attr (d/entity (d/db conn) (ffirst (d/q '[:find ?attr :where [?p :portfolio/reference "p2"]
                                            [?p :portfolio/accounts ?a]
                                            [?a ?attr _]]
                                    (d/db conn)))))
attr

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
