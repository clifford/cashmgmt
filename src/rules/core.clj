(ns rules.core
  (:use    [datomic.api :only [db q] :as d])
  (:import [datomic Peer Util] ;; Imports only used in data loading
           [java.io FileReader]))

;(use '[datomic.api :only [q ab] :as d])
(def uri "datomic:mem://rules")

(d/create-database uri)

(def conn (d/connect uri))

(def schema-tx (read-string (slurp "/Users/dev/src/clj/rules/rules.dtm")))

@(d/transact conn schema-tx)

(def data-tx (read-string (slurp "/Users/dev/src/clj/rules/rule-data.dtm")))

@(d/transact conn data-tx)

(q '[:find ?c
     :in $ ?x
     :where [?c :rule/name ?x]] (db conn) "fee1")

(map (comp :rule/name
           #(d/entity (db conn) %)
           first)
     (q '[:find ?c :where [?c :rule/name]]
        (db conn)))

(def rule-ids (map (comp :rule/name
            #(d/entity (db conn) %)
            first)
      (q '[:find ?c :where [?c :rule/name]]
         (db conn))))

(map (comp :rule/name #(d/entity (db conn) %) first) rule-ids)

(let [entities  (map (comp #(d/entity (db conn) %)
                           first)
                     (q '[:find ?c :where [?c :clause/name]]
                        (db conn)))]
  (map :clause/rule  entities)
  )

(defn rules []
  (let [entities  (map (comp #(d/entity (db conn) %)
                             first)
                       (q '[:find ?c :where [?c :clause/name]]
                          (db conn)))]
    entities
    ))
(first (rules))
(keys (first (rules)))
(:clause/name (first (rules)))

(let [{clause-name :clause/name
       clause-func :clause/function} (first (rules))]
  (prn "clause name:" clause-name clause-func))

(let [attr (map {cl-name :clause/name
             cl-func :clause/function
             cl-rule :clause/rule} (rules))])

(defn interrogate-rule [keys rule]
  `(let [{:keys ~@keys} ~@rule]))

(defn inspect-rule [l rule]
  (loop [l l
         rule rule
         accum ()]
    (cond 
     (empty? l) accum
     :else (recur (rest l) rule (cons (get rule (first l)) accum)))))

(inspect-rule [:a :b :d] {:a 1 :b 2 :c 3})
(inspect-rule [:a :b] {:a 1 :b 2 :c 3})
(inspect-rule [:clause/name :clause/rule :clause/function] (first (rules)))
(map #(inspect-rule [:clause/name :clause/function :clause/rule] %) (rules))

(:clause/name (first (rules)))
(map (comp first (rules)) (seq [:clause/name]))
(inspect-rule [:clause/name :clause/function] (first (rules)))

(for [k [:clause/name :clause/function] rule (rules)]
  (prn k ":" (get rule k)))

;; search for match
(q '[:find ?n
     :where
     [?e :rule/name ?n]
     [(#(< (.compareTo ^String % "g") 0) ?n)]]
   (db conn))

;; (q '[:find ?n
;;      :in $ ?x
;;      :where
;;      [?e :rule/name ?n]
;;      [(#(< (.compareTo ^String % ?x) 0) ?n)]]
;;    (db conn) :g)

(q '[:find ?n
       :where
       [(fulltext $ :rule/name "fee1") [[?e ?n]]]]
   (db conn))

(q '[:find ?r ?clause
     :in $ ?search
     :where
     [?e :rule/name ?r]
     [?e :clause/name ?clause]
     [(fulltext $ :rule/name ?search) [[?e ?clause]]]]
   (db conn)
   "fee1")

(q '[:find ?when
     :where
     [?tx :db/txInstant ?when]]
   (db conn))

(def db-dates (->> (q '[:find ?when
                        :where
                        [?tx :db/txInstant ?when]]
                      (db conn))
                   seq
                   (map first)
                   sort))

;; query against the past db state
(count (q '[:find ?c :where [?c :rule/name]]
          (d/as-of (db conn) (last db-dates))))

(count (q '[:find ?c :where [?c :rule/name]]
          (d/since (db conn) (last db-dates))))

(count (q '[:find ?c :where [?c :rule/name]]
          (d/as-of (db conn) (first db-dates))))

(d/transact conn
            '[{:db/id #db/id [:db.part/user]
               :rule/name
               "feex"}])


;;create a datomic rule which will retrieve fees

(defn create-rec [data]
  (let [eid (d/tempid :db.part/user)]
    [eid (map (fn [[k v]]
                `{:db/id ~eid ~k ~v})
              data)]))
    
(let [[rule-eid rule-sql] (create-rec {:rule/name "advisor"})
      clauses    [["clause1" "1"]
                  ["clause2" "2"]
                  ["clause3" "3"]
                  ["clause4" "4"]]
      clause-sql  (->> clauses
                       (map (fn [cls] (->> (conj cls rule-eid)
                                          (zipmap [:clause/name :clause/function :clause/rule])
                                          create-rec



                                          second)))
                       flatten)
      sql (vec (concat rule-sql clause-sql))
      ]
  (d/transact conn sql))

;; sql to add a business rule
(defn add-rule [name clause-functions]
  `(let [[rule-eid rule-sql] (create-rec {:rule/name ~name})
         clause-sql (->> (map vec clause-functions)
                         (map (fn [cls] (->> (conj cls rule-eid)
                                            (zipmap [:clause/name :clause/function :clause/rule])
                                            create-rec
                                            second)))
                         flatten)
         sql (vec (concat rule-sql clause-sql))
         ] (d/transact conn sql)))

(add-rule :rule1 {"cl1" "f1"
                  "cl2" "f2"})

(q '[:find ?cn
     :where
     [?e :rule/name "advisor"]
;     [?e :db/ident ?cn]
     [?cl :clause/rule ?e]
     [?cl :clause/name ?cn]
     ] (db conn))

;; sql to find rule
(def rule1-sql '[[[rulex ?cl]
                  [?cl :clause/name "clause1"]
                  [?cl :clause/name "clause1"]]])
(q '[:find ?n
     :in $ %
     :where
     [?c :clause/function ?n]
     (rulex ?c)]
   (db conn)
   rule1-sql)

(def rec1 [ {:clause/name "c1",
             :clause/function "f1",
             :clause/rule (d/tempid :db.part/user),
             :db/id #db/id [:db.part/user -1]}

            {
             :rule/name "r1",
             :db/id #db/id [:db.part/user -1]}
            ]
  )

(d/transact conn rec1)





;; example data
;; ({:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000097]} {:track/id 101, :db/id #db/id[:db.part/user -1000097]} {:track/num 1, :db/id #db/id[:db.part/user -1000097]} {:track/name "Smells Like Teen Spirit", :db/id #db/id[:db.part/user -1000097]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000098]} {:track/id 102, :db/id #db/id[:db.part/user -1000098]} {:track/num 2, :db/id #db/id[:db.part/user -1000098]} {:track/name "In Bloom", :db/id #db/id[:db.part/user -1000098]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000099]} {:track/id 103, :db/id #db/id[:db.part/user -1000099]} {:track/num 3, :db/id #db/id[:db.part/user -1000099]} {:track/name "Come As You Are", :db/id #db/id[:db.part/user -1000099]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000100]} {:track/id 104, :db/id #db/id[:db.part/user -1000100]} {:track/num 4, :db/id #db/id[:db.part/user -1000100]} {:track/name "Breed", 
;; :db/id #db/id[:db.part/user -1000100]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000101]} {:track/id 105, :db/id #db/id[:db.part/user -1000101]} {:track/num 5, :db/id #db/id[:db.part/user -1000101]} {:track/name "Lithium", :db/id #db/id[:db.part/user -1000101]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000102]} {:track/id 106, :db/id #db/id[:db.part/user -1000102]} {:track/num 6, :db/id #db/id[:db.part/user -1000102]} {:track/name "Polly", :db/id #db/id[:db.part/user -1000102]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000103]} {:track/id 107, :db/id #db/id[:db.part/user -1000103]} {:track/num 7, :db/id #db/id[:db.part/user -1000103]} {:track/name "Territorial Pissings", :db/id #db/id[:db.part/user -1000103]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000104]} {:track/id 108, :db/id #db/id[:db.part/user -1000104]} {:track/num 8, :db/id #db/id[:db.part/user -1000104]} {:track/name
;;  "Drain You", :db/id #db/id[:db.part/user -1000104]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000105]} {:track/id 109, :db/id #db/id[:db.part/user -1000105]} {:track/num 9, :db/id #db/id[:db.part/user -1000105]} {:track/name "Lounge Act", :db/id #db/id[:db.part/user -1000105]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000106]} {:track/id 110, :db/id #db/id[:db.part/user -1000106]} {:track/num 10, :db/id #db/id[:db.part/user -1000106]} {:track/name "Stay Away", :db/id #db/id[:db.part/user -1000106]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000107]} {:track/id 111, :db/id #db/id[:db.part/user -1000107]} {:track/num 11, :db/id #db/id[:db.part/user -1000107]} {:track/name "On A Plain", :db/id #db/id[:db.part/user -1000107]} {:track/album #db/id[:db.part/user -1000096], :db/id #db/id[:db.part/user -1000108]} {:track/id 112, :db/id #db/id[:db.part/user -1000108]} {:track/num 12, :db/id #db/id[:db.part/user -1000108]
;; } {:track/name "Something In The Way, with the hidden track", :db/id #db/id[:db.part/user -1000108]})