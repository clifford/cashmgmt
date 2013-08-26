(ns cashmgmt.position
  (:require [cashmgmt.acc-dsl :as a]
            [datomic.api :only [q db] :as d]
            [clojure.java.io :as io]))


(comment
  a position depends on a price for a date in order to be valued. So by valuing an instrument on a date with
  a price we get a position. Prices should be stored as quotes. Whilst Yields should be stored on yield curves)
