(ns cashmgmt.util.emap
  (:require [datomic.api :only [db q] :as d]))

(defn emap
  "lookup the entity map from the query"
  [conn query]
 (let [eid (ffirst (d/q query (d/db conn)))
       emap (d/entity (d/db conn) eid)]
   ;;  (uq/assert-on-emap conn emap :instrument/type [:p1 :p2])
   emap
   ))
