(ns cashmgmt.core
  (:use [cashmgmt.acc-dsl]
        [cashmgmt.portfolio]
        [cashmgmt.util unique date emap])
  (:require [datomic.samples.repl :as util]
            [clojure.java.io :as io]
            [datomic.api :only [q db] :as d]))

(def conn (util/scratch-conn))
(install-db-funcs conn)

(def cashmgmt-schema (read-string (slurp (io/resource "cashmgmt/cashmgmt-schema.edn"))))
(d/transact conn cashmgmt-schema)

(def r153 (create-instrument conn "R153" :bond))
(def a789 (create-acc conn "a789" 10M r153))
(:account/tag a789)

;; seed an account with a holding in an instrument
;; then value the a portfolio which has holds the position in this account
;; the value will vary based on the price of the underlying asset.
;; If a desired ratio was stipulated for the portfolio, then a compensating order
;; should be created to bring the portfolio back into alignment

(comment
  (in {:zar :a123}
      clear-thru [:ca444])
  )
