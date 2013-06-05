(ns cashmgmt.order-gen
  (:require [clojure.math.combinatorics :as comb]
            [clojure.test.generative.generators :as gen]
            )
  (:require [cashmgmt.acc-dsl :as cm]))

;; cashmgmt.acc-dsl :as a

(comb/selections [1 2 3] 2)

;; portfolio has a current state {:agef 1600 :agbf 2000} which equates to say, 40% agef & 60% agbf but
;; now the market price moves and the allocation changes to say, 41% agef & 59% agbf
;; plan is to seed 2 accounts (as per above) then we generate prices for these instruments which alters
;; the porfolios proportions. Code must then rebalance to bring bring back into line.
;;
;; input -> revalue(new-price) fn
;; fn under test -> rebalance
;; output -> ratios fn
