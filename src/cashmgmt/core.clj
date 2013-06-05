(ns cashmgmt.core
  (:use [cashmgmt.acc-dsl :reload true]
        [cashmgmt.portfolio :reload true]
        [cashmgmt.util unique date emap])
  (:require [datomic.samples.repl :as util]
            [clojure.java.io :as io]
            [datomic.api :only [q db] :as d]))

(def cn (util/scratch-conn))
(install-db-funcs cn)

(def cashmgmt-schema (read-string (slurp (io/resource "cashmgmt/cashmgmt-schema.edn"))))
(d/transact cn cashmgmt-schema)

(create-acc cn "a123" 10M)
(def a123 (get-acc cn "a123"))
(:account/tag a123)

;; seed an account with a holding in an instrument
;; then value the a portfolio which has holds the position in this account
;; the value will vary based on the price of the underlying asset.
;; If a desired ratio was stipulated for the portfolio, then a compensating order
;; should be created to bring the portfolio back into alignment

(createxo)
(comment
  (in {:zar :a123}
      clear-thru [:ca444])
  )
