(ns com.hypirion.clj-xchart
  (:refer-clojure :exclude [spit])
  (:require [clojure.set :as set]
            [clojure.string :as s])
  (:import (de.erichseifert.vectorgraphics2d SVGGraphics2D
                                             PDFGraphics2D
                                             EPSGraphics2D)
           (org.knowm.xchart BubbleChart
                             XYChart
                             PieChart
                             CategoryChart
                             BubbleSeries$BubbleSeriesRenderStyle
                             CategorySeries$CategorySeriesRenderStyle
                             PieSeries$PieSeriesRenderStyle
                             XYSeries$XYSeriesRenderStyle
                             XChartPanel
                             BitmapEncoder
                             BitmapEncoder$BitmapFormat)
           (org.knowm.xchart.style Styler
                                   AxesChartStyler
                                   Styler$LegendPosition
                                   Styler$TextAlignment
                                   PieStyler$AnnotationType
                                   GGPlot2Theme
                                   MatlabTheme
                                   XChartTheme)
           (org.knowm.xchart.style.markers Circle
                                           Diamond
                                           None
                                           Square
                                           TriangleDown
                                           TriangleUp)
           (org.knowm.xchart.style.lines SeriesLines)
           (java.io FileOutputStream)
           (java.awt Color
                     GridLayout)
           (javax.swing JPanel
                        JFrame
                        SwingUtilities)))

;; reduce-map + map-vals is taken from the Medley utility library:
;; https://github.com/weavejester/medley
;; Medley is under the same license (EPL1.0) as clj-xchart.
(defn- reduce-map [f coll]
  (if (instance? clojure.lang.IEditableCollection coll)
    (persistent! (reduce-kv (f assoc!) (transient (empty coll)) coll))
    (reduce-kv (f assoc) (empty coll) coll)))

(defn- map-vals
  "Maps a function over the values of an associative collection."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m k (f v)))) coll))

(defprotocol Chart
  "Protocol for charts, which extends the XChart charts with
  additional polymorphic Clojure functions."
  (add-series! [chart series-name data]
    "A method to add new series to the provided chart"))

(def colors
  "All the default java.awt colors as keywords. You can use this map
  to iterate over the keys, in case you'd like to compare different
  colors. Or you could use java.awt.Color directly to use the exact
  color you want."
  {:blue Color/BLUE
   :black Color/BLACK
   :cyan Color/CYAN
   :dark-gray Color/DARK_GRAY
   :gray Color/GRAY
   :green Color/GREEN
   :light-gray Color/LIGHT_GRAY
   :magenta Color/MAGENTA
   :orange Color/ORANGE
   :pink Color/PINK
   :red Color/RED
   :white Color/WHITE
   :yellow Color/YELLOW})

(def strokes
  "The default stroke types provided by XChart. You can also use a self-made
  stroke if you're not happy with any of the predefined ones."
  {:none SeriesLines/NONE
   :solid SeriesLines/SOLID
   :dash-dot SeriesLines/DASH_DOT
   :dash-dash SeriesLines/DASH_DASH
   :dot-dot SeriesLines/DOT_DOT})

(def markers
  "All the default XChart markers as keywords. To create your own marker, you
  must _subclass_ the org.knowm.xchart.style.markers.Marker class, so it's often
  better to use the default ones."
  {:circle (Circle.)
   :diamond (Diamond.)
   :none (None.)
   :square (Square.)
   :triangle-up (TriangleUp.)
   :triangle-down (TriangleDown.)})

(def xy-render-styles
  "The different xy-render styles: :area, :scatter and :line."
  {:area XYSeries$XYSeriesRenderStyle/Area
   :scatter XYSeries$XYSeriesRenderStyle/Scatter
   :line XYSeries$XYSeriesRenderStyle/Line})

(def pie-render-styles
  "The different pie render styles. It is :pie by default."
  {:pie PieSeries$PieSeriesRenderStyle/Pie
   :donut PieSeries$PieSeriesRenderStyle/Donut})

(def pie-annotation-types
  "The different annotation types you can use to annotate pie charts.
  By default, this is :percentage."
  {:label PieStyler$AnnotationType/Label
   :label-and-percentage PieStyler$AnnotationType/LabelAndPercentage
   :percentage PieStyler$AnnotationType/Percentage})

(def category-render-styles
  "The different styles you can use for category series."
  {:area CategorySeries$CategorySeriesRenderStyle/Area
   :bar CategorySeries$CategorySeriesRenderStyle/Bar
   :line CategorySeries$CategorySeriesRenderStyle/Line
   :scatter CategorySeries$CategorySeriesRenderStyle/Scatter
   :stick CategorySeries$CategorySeriesRenderStyle/Stick})

(def bubble-render-styles
  "Different render styles for bubble series. For now this is useless, as you
  can only use :round. Apparently :box is around the corner though."
  {:round BubbleSeries$BubbleSeriesRenderStyle/Round})

(def text-alignments
  "The different kinds of text alignments you can use."
  {:centre Styler$TextAlignment/Centre
   :left Styler$TextAlignment/Left
   :right Styler$TextAlignment/Right})

(def legend-positions
  "The different legend positions. Note that xchart implements only a
  subset of inside/outside for the different positions."
  {:inside-n  Styler$LegendPosition/InsideN
   :inside-ne Styler$LegendPosition/InsideNE
   :inside-nw Styler$LegendPosition/InsideNW
   :inside-se Styler$LegendPosition/InsideSE
   :inside-sw Styler$LegendPosition/InsideSW
   :outside-e Styler$LegendPosition/OutsideE})

(def themes
  "The different default themes you can use with xchart."
  {:ggplot2 (GGPlot2Theme.)
   :matlab (MatlabTheme.)
   :xchart (XChartTheme.)})

(defmacro ^:private doto-cond
  "Example:
  (doto-cond expr
    cond1 (my call)
    cond2 (my2 call2))
  =>
  (let [e# expr]
    (when cond1 (my #e call))
    (when cond2 (my2 #2 call2)))"
  [expr & clauses]
  (let [pairs (partition 2 clauses)
        expr-sym (gensym "expr")]
    `(let [~expr-sym ~expr]
       ~@(map (fn [[cond clause]]
                `(when ~cond
                   (~(first clause) ~expr-sym ~@(rest clause))))
              pairs)
       ~expr-sym)))

(defn- set-legend!
  [^Styler styler
   {:keys [background-color border-color font padding
           position series-line-length visible?]}]
  (doto-cond
   styler
   background-color (.setLegendBackgroundColor (colors background-color background-color))
   border-color (.setLegendBorderColor (colors border-color border-color))
   font (.setLegendFont font)
   padding (.setLegendPadding (int padding))
   position (.setLegendPosition (legend-positions position))
   series-line-length (.setLegendSeriesLineLength (int series-line-length))
   (not (nil? visible?)) (.setLegendVisible (boolean visible?))))

(defn- set-chart-title-style!
  [^Styler styler
   {:keys [box font padding visible?]}]
  (let [{box-background-color :background-color
         box-border-color :color
         box-visible? :visible?} box]
    (doto-cond
     styler
     box-background-color (.setChartTitleBoxBackgroundColor (colors box-background-color box-background-color))
     box-border-color (.setChartTitleBoxBorderColor (colors box-border-color box-border-color))
     (not (nil? box-visible?)) (.setChartTitleBoxVisible (boolean box-visible?))
     font (.setChartTitleFont font)
     padding (.setChartTitlePadding (int padding))
     (not (nil? visible?)) (.setChartTitleVisible (boolean visible?)))))

(defn- set-chart-style!
  [^Styler styler
   {:keys [background-color font-color padding title]}]
  (doto-cond
   styler
   background-color (.setChartBackgroundColor (colors background-color background-color))
   font-color (.setChartFontColor (colors font-color font-color))
   padding (.setChartPadding (int padding))
   title (set-chart-title-style! title)))

(defn- set-plot-style!
  [^Styler styler
   {:keys [background-color border-color border-visible? content-size]}]
  (doto-cond
   styler
   background-color (.setPlotBackgroundColor (colors background-color background-color))
   border-color (.setPlotBorderColor (colors border-color border-color))
   (not (nil? border-visible?)) (.setPlotBorderVisible (boolean border-visible?))
   content-size (.setPlotContentSize (double content-size))))

(defn- set-series-style!
  [^Styler styler
   series]
  ;; All of these are arrays, so we mutate them and set them back in.
  (let [series-colors (.getSeriesColors styler)
        series-lines (.getSeriesLines styler)
        series-markers (.getSeriesMarkers styler)
        series (vec series)]
    (dotimes [i (count series)]
      ;; TODO: nth instead mayhaps
      (let [{:keys [color stroke marker]} (series i)]
        (when color
          (aset series-colors i (colors color color)))
        (when stroke
          (aset series-lines i (strokes stroke stroke)))
        (when marker
          (aset series-markers i (markers marker marker)))))
    (doto styler
      (.setSeriesColors series-colors)
      (.setSeriesLines series-lines)
      (.setSeriesMarkers series-markers))))

(defn- set-default-style!
  [^Styler styler
   {:keys [annotations-font annotations? chart plot legend series]}]
  (doto-cond
   styler
   annotations-font (.setAnnotationsFont annotations-font)
   (not (nil? annotations?)) (.setHasAnnotations (boolean annotations?))
   chart (set-chart-style! chart)
   legend (set-legend! legend)
   plot (set-plot-style! plot)
   series (set-series-style! series)))

(defn- set-axis-ticks!
  [^AxesChartStyler styler
   {:keys [labels marks padding visible? line-visible?]}]
  (let [{:keys [color font]} labels]
    (doto-cond
     styler
     color (.setAxisTickLabelsColor (colors color color))
     font (.setAxisTickLabelsFont font)))
  (let [{:keys [length color stroke visible?]} marks]
    (doto-cond
     styler
     length (.setAxisTickMarkLength (int length))
     color (.setAxisTickMarksColor (colors color color))
     stroke (.setAxisTickMarksStroke (strokes stroke stroke))
     (not (nil? visible?)) (.setAxisTicksMarksVisible (boolean visible?))))
  (doto-cond
   styler
   padding (.setAxisTickPadding (int padding))
   (not (nil? line-visible?)) (.setAxisTicksLineVisible (boolean line-visible?))
   (not (nil? visible?)) (.setAxisTicksVisible (boolean visible?))))

(defn- set-axis-title!
  [^AxesChartStyler styler
   {:keys [font visible? padding]}]
  (doto-cond
   styler
   font (.setAxisTitleFont font)
   padding (.setAxisTitlePadding (int padding))
   (not (nil? visible?)) (.setAxisTitleVisible (boolean visible?))))

(defn- set-axis-plot!
  [^AxesChartStyler styler
   {:keys [grid-lines margin tick-marks?]}]
  (let [{:keys [horizontal? vertical? visible? color stroke]} grid-lines]
    (doto-cond
     styler
     (not (nil? visible?)) (.setPlotGridLinesVisible (boolean visible?))
     color (.setPlotGridLinesColor (colors color color))
     stroke (.setPlotGridLinesStroke (strokes stroke stroke))
     (not (nil? horizontal?)) (.setPlotGridHorizontalLinesVisible (boolean horizontal?))
     (not (nil? vertical?)) (.setPlotGridVerticalLinesVisible (boolean vertical?))))
  (doto-cond
   styler
   margin (.setPlotMargin (int margin))
   (not (nil? tick-marks?)) (.setPlotTicksMarksVisible (boolean tick-marks?))))

(defn- set-x-axis-style!
  [^AxesChartStyler styler
   {:keys [label logarithmic? max min decimal-pattern
           tick-mark-spacing-hint ticks-visible? title-visible?]}]
  (let [{:keys [alignment rotation]} label]
    (doto-cond
     styler
     alignment (.setXAxisLabelAlignment (text-alignments alignment alignment))
     rotation (.setXAxisLabelRotation (int rotation))))
  (doto-cond
   styler
   decimal-pattern (.setXAxisDecimalPattern decimal-pattern)
   (not (nil? logarithmic?)) (.setXAxisLogarithmic (boolean logarithmic?))
   max (.setXAxisMax (double max))
   min (.setXAxisMin (double min))
   tick-mark-spacing-hint (.setXAxisTickMarkSpacingHint (int tick-mark-spacing-hint))
   (not (nil? ticks-visible?)) (.setXAxisTicksVisible (boolean ticks-visible?))
   (not (nil? title-visible?)) (.setXAxisTitleVisible (boolean title-visible?))))

(defn- set-y-axis-style!
  [^AxesChartStyler styler
   {:keys [label logarithmic? max min decimal-pattern
           tick-mark-spacing-hint ticks-visible? title-visible?]}]
  (let [{:keys [alignment rotation]} label]
    (doto-cond
     styler
     alignment (.setYAxisLabelAlignment (text-alignments alignment alignment))))
  (doto-cond
   styler
   decimal-pattern (.setYAxisDecimalPattern decimal-pattern)
   (not (nil? logarithmic?)) (.setYAxisLogarithmic (boolean logarithmic?))
   max (.setYAxisMax (double max))
   min (.setYAxisMin (double min))
   tick-mark-spacing-hint (.setYAxisTickMarkSpacingHint (int tick-mark-spacing-hint))
   (not (nil? ticks-visible?)) (.setYAxisTicksVisible (boolean ticks-visible?))
   (not (nil? title-visible?)) (.setYAxisTitleVisible (boolean title-visible?))))

(defn- set-axes-style!
  [^AxesChartStyler styler
   {:keys [axis error-bars-color plot x-axis y-axis
           date-pattern decimal-pattern locale marker timezone]}]
  (let [ebc error-bars-color ;; error-bars-color is too long to be readable in these expressions
        {axis-ticks :ticks axis-title :title} axis
        {marker-size :size} marker]
    (doto-cond
     styler
     axis-ticks (set-axis-ticks! axis-ticks)
     axis-title (set-axis-title! axis-title)
     date-pattern (.setDatePattern date-pattern)
     decimal-pattern (.setDecimalPattern decimal-pattern)
     ;; The logic here is as follows: You can specify a colour for the error
     ;; bars. If the colour is :match-series, then the colour matches the series
     ;; colour, but if you specify something else, you cannot match the series!
     (and ebc (not= ebc :match-series)) (.setErrorBarsColor (colors ebc ebc))
     (and ebc (not= ebc :match-series)) (.setErrorBarsColorSeriesColor false)
     (= ebc :match-series) (.setErrorBarsColorSeriesColor true)
     locale (.setLocale locale)
     marker-size (.setMarkerSize marker-size)
     plot (set-axis-plot! plot)
     timezone (.setTimezone timezone)
     x-axis (set-x-axis-style! x-axis)
     y-axis (set-y-axis-style! y-axis))))

(defn- add-raw-series
  ([chart s-name x-data y-data]
   (.addSeries chart s-name x-data y-data))
  ([chart s-name x-data y-data error-bars]
   (.addSeries chart s-name x-data y-data error-bars)))

(defn- assoc-in-nonexisting
  "Like assoc-in, but will only add fields not found. Nil may be found, in which
  case it is NOT updated."
  [m ks v]
  (cond->
      m
      (identical? (get-in m ks ::not-found) ::not-found)
      (assoc-in ks v)))

(defn- attach-default-font
  "Sets the font type on all the provided "
  [style-map]
  (if-let [font (:font style-map)]
    (-> style-map
        (dissoc style-map :font)
        (assoc-in-nonexisting [:axis :ticks :labels :font] font)
        (assoc-in-nonexisting [:axis :title :font] font)
        (assoc-in-nonexisting [:legend :font] font)
        (assoc-in-nonexisting [:annotations-font] font)
        (assoc-in-nonexisting [:chart :title :font] font))
    style-map))

(extend-type XYChart
  Chart
  (add-series! [chart s-name data]
    (if (sequential? data)
      (apply add-raw-series chart s-name data)
      (let [{:keys [x y error-bars style]} data
            {:keys [marker-color marker-type
                    line-color line-style line-width
                    fill-color show-in-legend? render-style]} style]
        (doto-cond
         (if error-bars
           (add-raw-series chart s-name x y error-bars)
           (add-raw-series chart s-name x y))
         render-style (.setXYSeriesRenderStyle (xy-render-styles render-style))
         marker-color (.setMarkerColor (colors marker-color marker-color))
         marker-type (.setMarker (markers marker-type marker-type))
         line-color (.setLineColor (colors line-color line-color))
         line-style (.setLineStyle (strokes line-style line-style))
         line-width (.setLineWidth (float line-width))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn xy-chart
  "Returns an xy-chart. See the tutorial for more information about
  how to create an xy-chart, and see the render-styles documentation
  for styling options."
  ([series]
   (xy-chart series {}))
  ([series
    {:keys [width height title theme render-style]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (XYChart. width height)
         styling (attach-default-font styling)]
     (doto-cond
      (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (xy-render-styles render-style)))
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto (.getStyler chart)
       (set-default-style! styling)
       (set-axes-style! styling))
     (doto-cond
      chart
      title (.setTitle title)
      (-> styling :x-axis :title) (.setXAxisTitle (-> styling :x-axis :title))
      (-> styling :y-axis :title) (.setYAxisTitle (-> styling :y-axis :title))))))

(extend-type CategoryChart
  Chart
  (add-series! [chart s-name data]
    (if (sequential? data)
      (apply add-raw-series chart s-name data)
      (let [{:keys [x y error-bars style]} data
            {:keys [marker-color marker-type
                    line-color line-style line-width
                    fill-color show-in-legend? render-style]} style]
        (doto-cond
         (if error-bars
           (add-raw-series chart s-name x y error-bars)
           (add-raw-series chart s-name x y))
         render-style (.setChartCategorySeriesRenderStyle (category-render-styles render-style))
         marker-color (.setMarkerColor (colors marker-color marker-color))
         marker-type (.setMarker (markers marker-type marker-type))
         line-color (.setLineColor (colors line-color line-color))
         line-style (.setLineStyle (strokes line-style line-style))
         line-width (.setLineWidth (float line-width))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn category-chart*
  "Returns a raw category chart. Prefer `category-chart` unless you
  run into performance issues. See the tutorial for more information
  about how to create category charts, and see the render-styles
  documentation for styling options."
  ([series]
   (category-chart* series {}))
  ([series
    {:keys [width height title theme render-style available-space-fill overlap?
            stacked?]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (CategoryChart. width height)
         styling (attach-default-font styling)]
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto-cond
      (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (category-render-styles render-style))
      available-space-fill (.setAvailableSpaceFill (double available-space-fill))
      (not (nil? overlap?)) (.setOverlapped (boolean overlap?))
      (not (nil? stacked?)) (.setStacked (boolean stacked?)))
     (doto (.getStyler chart)
       (set-default-style! styling)
       (set-axes-style! styling))
     (doto-cond
      chart
      title (.setTitle title)
      (-> styling :x-axis :title) (.setXAxisTitle (-> styling :x-axis :title))
      (-> styling :y-axis :title) (.setYAxisTitle (-> styling :y-axis :title))))))

;; Utility functions

(defn- normalize-category-series
  "Returns the content of a category series on the shape {:x ... :y ...} with
  styling data retained."
  [series-data]
  (cond (and (map? series-data)
             (contains? series-data :x)
             (contains? series-data :y)) series-data
        (and (map? series-data)
             (contains? series-data :content)) (-> (:content series-data)
                                                   (normalize-category-series)
                                                   ;; retain styling data:
                                                   (merge (dissoc series-data :content)))
        ;; Assuming keys are strings/vals
        (and (map? series-data)
             (every? (comp not keyword?)
                     (keys series-data))) {:x (keys series-data)
                                           :y (vals series-data)}
        (sequential? series-data) {:x (first series-data)
                                   :y (second series-data)}))

(defn- category-series-xs
  "Given a map of series, return all the unique x-elements as a set."
  [series]
  (->> (vals series)
       (mapcat :x)
       set))

(defn- reorder-series
  "Reorders a normalized series content to the assigned ordering"
  [{:keys [x y] :as series} x-order]
  ;; Here we may unfortunately recompute an input value. If perfomance is an
  ;; issue, we may attach the mapping onto the series.
  (let [mapping (zipmap x y)]
    (assoc series
           :x x-order
           :y (mapv (fn [x] (get mapping x 0.0)) x-order))))

;; I do have some issues differing between a single series and multiple series.
;; I'll call a map of series a series-map for now.
(defn- normalize-category-series-map
  "Given a series map, normalize the series to contain all x values with the
  order specified in x-order. If the x value does not exist in a series, the
  value 0.0 is inserted. If there are other x values not in x-order, they are
  attached at the end in sorted order."
  [series-map x-order]
  (let [series-map (map-vals normalize-category-series series-map)
        x-order (vec x-order)
        extra-xs (sort (set/difference (category-series-xs series-map)
                                       (set x-order)))
        x-order (into x-order extra-xs)]
    (map-vals #(reorder-series % x-order) series-map)))

(defn category-chart
  "Returns a category chart. See the tutorial for more information
  about how to create category charts, and see the render-styles
  documentation for styling options."
  ([series]
   (category-chart series {}))
  ([series {:keys [x-axis series-order] :as styling}]
   (let [x-order (:order x-axis)
         normalized-map (normalize-category-series-map series x-order)
         extra-categories (->> (apply dissoc normalized-map series-order)
                               (sort-by key))
         normalized-seq (concat (keep #(find normalized-map %) series-order)
                                extra-categories)]
     (category-chart* normalized-seq styling))))

(extend-type BubbleChart
  Chart
  (add-series! [chart s-name data]
    (if (sequential? data)
      (apply add-raw-series chart s-name data)
      (let [{:keys [x y bubble style]} data
            {:keys [marker-color marker-type
                    line-color line-style line-width
                    fill-color show-in-legend? render-style]} style]
        (doto-cond
         (add-raw-series chart s-name x y bubble)
         ;; NOTE: Add render style when squares are added to the impl?
         render-style (.setBubbleSeriesRenderStyle (bubble-render-styles render-style))
         line-color (.setLineColor (colors line-color line-color))
         line-style (.setLineStyle (strokes line-style line-style))
         line-width (.setLineWidth (float line-width))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn bubble-chart*
  "Returns a raw bubble chart. Bubble charts are hard to make right,
  so please see the tutorial for more information about how to create
  one. The render-styles page will give you information about styling
  options."
  ([series]
   (bubble-chart* series {}))
  ([series
    {:keys [width height title theme render-style]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (BubbleChart. width height)
         styling (attach-default-font styling)]
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto-cond
      (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (bubble-render-styles render-style)))
     (doto (.getStyler chart)
       (set-default-style! styling)
       (set-axes-style! styling))
     (doto-cond
      chart
      title (.setTitle title)
      (-> styling :x-axis :title) (.setXAxisTitle (-> styling :x-axis :title))
      (-> styling :y-axis :title) (.setYAxisTitle (-> styling :y-axis :title))))))

(defn- attach-default-annotation-distance
  "Attaches a default annotation distance if the donut thickness"
  [styling]
  (if-not (and (identical? :donut (:render-style styling))
               (not (:annotation-distance styling)))
    styling
    (assoc styling :annotation-distance
           (- 1.0 (/ (:donut-thickness styling 0.33) 2)))))

(extend-type PieChart
  Chart
  (add-series! [chart s-name data]
    (if (number? data)
      (.addSeries chart s-name data)
      (let [{:keys [render-style fill-color show-in-legend?]} (:style num)
            val (:value num)]
        (doto-cond
         (.addSeries chart s-name val)
         render-style (.setChartPieSeriesRenderStyle (pie-render-styles render-style))
         fill-color (.setFillColor (colors fill-color fill-color))
         (not (nil? show-in-legend?)) (.setShowInLegend (boolean show-in-legend?)))))))

(defn pie-chart
  "Returns a pie chart. The series map is in this case just a mapping
  from string to number. For styling information, see the
  render-styles page.

  Example:
  (c/pie-chart {\"Red\" 54
                \"Green\" 34})"
  ([series]
   (pie-chart series {}))
  ([series
    {:keys [width height title circular? theme render-style annotation-distance
            start-angle draw-all-annotations? donut-thickness annotation-type]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (PieChart. width height)
         styling (-> styling
                     attach-default-font
                     attach-default-annotation-distance)
         ;; Need to rebind this one. We could probably omit it from the keys
         ;; entry at the top, if it's not used for documentation purposes.
         annotation-distance (:annotation-distance styling)]
     (doseq [[s-name data] series]
       (add-series! chart s-name data))
     (doto-cond
      (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (pie-render-styles render-style))
      (not (nil? circular?)) (.setCircular (boolean circular?))
      (not (nil? draw-all-annotations?)) (.setDrawAllAnnotations (boolean draw-all-annotations?))
      annotation-distance (.setAnnotationDistance (double annotation-distance))
      donut-thickness (.setDonutThickness (double donut-thickness))
      start-angle (.setStartAngleInDegrees (double start-angle))
      annotation-type (.setAnnotationType (pie-annotation-types annotation-type)))
     (set-default-style! (.getStyler chart) styling)
     (doto-cond
      chart
      title (.setTitle title)
      (-> styling :x-axis :title) (.setXAxisTitle (-> styling :x-axis :title))
      (-> styling :y-axis :title) (.setYAxisTitle (-> styling :y-axis :title))))))

(defn as-buffered-image
  "Converts a chart into a java.awt.image.BufferedImage."
  [chart]
  (BitmapEncoder/getBufferedImage chart))

(def ^:private bitmap-formats
  {:png BitmapEncoder$BitmapFormat/PNG
   :gif BitmapEncoder$BitmapFormat/GIF
   :bmp BitmapEncoder$BitmapFormat/BMP
   :jpg BitmapEncoder$BitmapFormat/JPG
   :jpeg BitmapEncoder$BitmapFormat/JPG})

(def ^:private vector-formats
  {:pdf #(PDFGraphics2D. 0.0 0.0 %1 %2)
   :svg #(SVGGraphics2D. 0.0 0.0 %1 %2)
   :eps #(EPSGraphics2D. 0.0 0.0 %1 %2)})

(defn to-bytes
  "Converts a chart into a byte array."
  ([chart type]
   (if-let [bitmap-format (bitmap-formats type)]
     (BitmapEncoder/getBitmapBytes chart bitmap-format)
     (if-let [vector-format (vector-formats type)]
       (let [g (vector-format (.getWidth chart) (.getHeight chart))]
         (.paint chart g (.getWidth chart) (.getHeight chart))
         (.getBytes g))
       (throw (IllegalArgumentException. (str "Unknown format: " type)))))))

(defn view
  "Utility function to render one or more charts in a swing frame."
  [& charts]
  (let [num-rows (int (+ (Math/sqrt (count charts)) 0.5))
        num-cols (inc (/ (count charts)
                         (double num-rows)))
        frame (JFrame. "XChart")]
    (SwingUtilities/invokeLater
     #(do (.. frame (getContentPane) (setLayout (GridLayout. num-rows num-cols)))
          (doseq [chart charts]
            (if chart
              (.add frame (XChartPanel. chart))
              (.add frame (JPanel.))))
          (.pack frame)
          (.setVisible frame true)))
    frame))


(defn- guess-extension
  [fname]
  (if-let [last-dot (s/last-index-of fname ".")]
    (let [extension (s/lower-case (subs fname (inc last-dot)))]
      (keyword extension))))

(defn spit
  "Spits the chart to the given filename. If no type is provided, the type is
  guessed by the filename extension. If no extension is found, an error is
  raised."
  ([chart fname]
   (spit chart fname (guess-extension fname)))
  ([chart fname type]
   (with-open [fos (FileOutputStream. fname)]
     (.write fos (to-bytes chart type)))))

(defn- transpose-single
  [acc k1 v1]
  (reduce-kv (fn [m k2 v2]
               (assoc-in m [k2 k1] v2))
             acc v1))

(defn transpose-map
  "Transforms a map of maps such that the inner keys and outer keys are flipped.
  That is, `(get-in m [k1 k2])` = `(get-in (transpose-map m) [k2 k1])`. The
  inner values remain the same."
  [series]
  (reduce-kv transpose-single {} series))

(defn extract-series
  "Transforms coll into a series map by using the values in the provided keymap.
  There's no requirement to provide :x or :y (or any key at all, for that
  matter), although that's common.

  Example: (extract-series {:x f, :y g, :bubble bubble} coll)
        == {:x (map f coll), :y (map g coll), :bubble (map bubble coll)}"
  [keymap coll]
  (map-vals #(map % coll) keymap))

(defn- normalize-group
  [m]
  (let [sum (reduce + (vals m))]
    (map-vals #(/ % sum) m)))

(defn normalize-categories
  [m]
  (->> (transpose-map m)
       (map-vals normalize-group)
       transpose-map))
