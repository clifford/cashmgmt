(def model-id (d/tempid :model))

(def trading-model-data
  [{:db/id model-id
    :model/type :model.type/trading
    :model/traderCount 100
    :model/meanTradeAmount 100
    :model/initialBalance 1000
    :model/meanHoursBetweenTrades 1}])

(def trading-model
  (-> @(d/transact sim-conn trading-model-data)
      (tx-ent model-id)))
