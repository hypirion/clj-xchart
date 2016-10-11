(ns com.hypirion.clj-xchart-test
  (:require [clojure.test :refer :all]
            [com.hypirion.clj-xchart :as c]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(def category-series-gen
  (gen/map gen/string-ascii (gen/double* {:infinite? false
                                          :NaN? false})
           {:max-elements 100}))

(def category-series-map-gen
  (gen/map gen/string-ascii category-series-gen {:max-elements 10}))

(defspec normalization-doesnt-shuffle-values
  (prop/for-all [category-map category-series-gen]
    (let [normalized-map (#'c/normalize-category-series category-map)]
      (= category-map
         (zipmap (:x normalized-map) (:y normalized-map))))))

(defspec normalization-extends-x-values
  (prop/for-all [series-map category-series-map-gen]
    (let [normalized-categories (#'c/normalize-category-series-map series-map nil)
          x-values (set (mapcat keys (vals series-map)))]
      (every? #(= (set (:x (val %))) x-values)
              normalized-categories))))

(def nonempty-category-series-gen
  (gen/not-empty category-series-gen))

(def nonempty-category-series-map-gen
  "Where the series itself is nonempty, not the map containing series"
  (gen/map gen/string-ascii nonempty-category-series-gen {:max-elements 10}))

(defspec transpose-map-is-involutory-ish
  (prop/for-all [series-map nonempty-category-series-map-gen]
    (= series-map (c/transpose-map (c/transpose-map series-map)))))
