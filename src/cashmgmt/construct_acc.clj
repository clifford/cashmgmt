;; work through at the REPL, evaulating each form
(ns cashmgmt.construct-acc
  (:use [datomic.api :only [q db] :as d])
  (:require [datomic.samples.repl :as dod]))


(def construct-account-map
  "Returns map that could be added to transaction data to create
   a new account, or nil if account exists"
  #db/fn {:lang :clojure
          :params [db id name balance min-balance max-balance]
          :code (when-not (seq (d/q '[:find ?e
                                      :in $ ?name
                                      :where [?e :account/name ?name]]
                                    db name))
                  {:db/id id
                   :account/name name
;;                   :account/type type
                   :account/balance balance
                   :account/min-balance min-balance
                   :account/max-balance max-balance
                   })})

;; get a database *value* for testing construct-user-map
;; all tests can be of pure functions!
(def dbval (d/db conn))


