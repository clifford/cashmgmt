(ns cashmgmt.util.date
  (:require [clj-time.core :as cljt]
            [clj-time.format :as cljtf]
            ;; [clj-time.coerce :as cljtc]
            )
  (:import [java.util Date Calendar GregorianCalendar]))

(defn day-of-month [d]
  (cljt/day d))

(cljt/day (cljt/date-time 2012 11 20))

(cljt/date-time 20130508)

(cljt/within? (cljt/interval (cljt/date-time 1986) (cljt/date-time 1990))
                 (cljt/date-time 1987))

(cljt/within? (cljt/interval (cljt/date-time 20130508) (cljt/date-time 20130509))
              (cljt/date-time 20130508))

(defn date= [d1 d2]
  (cljt/within? (cljt/interval (cljt/date-time d1) (cljt/plus (cljt/date-time d1) (cljt/days 1)))
                (cljt/date-time d2)))

(date= 20130508 2013057)
(date= 20130508 20130508)
(date= 20130508 2013058)
(date= 20130508 20130509)

(comment
  Rich Hickey advice for instantiating dates
  user=> (read-string "#inst \"2012-09-11T11:51:26.00Z\"")
#inst "2012-09-11T11:51:26.000-00:00"

user=> (read-string (pr-str '#inst "2012-09-11T11:51:26.00Z"))
#inst "2012-09-11T11:51:26.000-00:00"
)


(cljt/plus (cljt/date-time 2013 5 8) (cljt/days 1))

(cljt/plus (cljt/date-time 1986 10 14) (cljt/days 1))
