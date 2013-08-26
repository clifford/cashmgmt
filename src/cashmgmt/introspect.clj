(ns cashmgmt.introspect
  (:require [datomic.api :only [db q] :as d]
            [cashmgmt.util.query :as qry]))

(defn portfolio-accs [conn portfolio f]
  "describe accounts on this portfolio"
  (->> (qry/qes '[:find ?a
                  :in $ ?portfolio
                  :where [?p :portfolio/reference ?portfolio]
                 [?p :portfolio/position ?a]]
               (d/db conn) portfolio)
       (map first)
       (map (fn [e] (f e)))
       ))
;; example usage of portfolio-accs
;; (def f (partial map (fn [[k v]] (format "%s -> %s" k v))))
;; (portfolio-accs "p2" f)

;; (def accs (i/portfolio-accs conn "p2" (let [r {}] (partial map (fn [[k v]] (assoc r k v))))))
;; (first accs)
;; (second accs)
