(ns com.hypirion.clj-xchart.opt
  "A namespace for clj-xchart with several optimisations when you run on big
  datasets."
  (:import (com.hypirion.clj_xchart ListMapping)))

(defn extract-field
  "Returns an immutable view over sequence mapped by field (can be a
  function, but is usually a keyword). The immutable view consumes no
  memory, at the expense of calling field multiple times for the same key.

  Immutable in this context means cannot be updated, even persistently."
  [field list]
  (ListMapping. list field))

(defn extract-series
  "Transforms coll into a series map by using the values in the
  provided keymap with exctract-field. There's no requirement to
  provide :x or :y (or any key at all, for that matter), although
  that's common.

  Example: (extract-series {:x f, :y g, :bubble bubble} coll)
        == {:x (extract-field f coll),
            :y (extract-field g coll),
            :bubble (extract-field bubble coll)}"
  [keymap coll]
  (into {}
        (for [[k v] keymap]
          [k (extract-field v coll)])))

