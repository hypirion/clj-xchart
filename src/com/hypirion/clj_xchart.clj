(ns com.hypirion.clj-xchart
  (:import (org.knowm.xchart XYChart
                             PieChart
                             PieSeries$PieSeriesRenderStyle
                             XYSeries$XYSeriesRenderStyle
                             XChartPanel)
           (org.knowm.xchart.style Styler
                                   Styler$LegendPosition
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
           (javax.swing JPanel
                        JFrame
                        SwingUtilities)
           (java.awt Color
                     GridLayout)))

(defn add-series
  ([^XYChart chart s-name x-data y-data]
   (.addSeries chart s-name x-data y-data))
  ([^XYChart chart s-name x-data y-data error-bars]
   (.addSeries chart s-name x-data y-data error-bars)))

(def colors
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

(def markers
  {:circle (Circle.)
   :diamond (Diamond.)
   :none (None.)
   :square (Square.)
   :triangle-up (TriangleUp.)
   :triangle-down (TriangleDown.)})

(def xy-render-styles
  {:area XYSeries$XYSeriesRenderStyle/Area
   :scatter XYSeries$XYSeriesRenderStyle/Scatter
   :line XYSeries$XYSeriesRenderStyle/Line})

(def pie-render-styles
  {:pie PieSeries$PieSeriesRenderStyle/Pie
   :donut PieSeries$PieSeriesRenderStyle/Donut})

(def pie-annotation-types
  {:label PieStyler$AnnotationType/Label
   :label-and-percentage PieStyler$AnnotationType/LabelAndPercentage
   :percentage PieStyler$AnnotationType/Percentage})

(def legend-positions
  {:inside-n Styler$LegendPosition/InsideN
   :inside-ne Styler$LegendPosition/InsideNE
   :inside-nw Styler$LegendPosition/InsideNW
   :inside-se Styler$LegendPosition/InsideSW
   :inside-sw Styler$LegendPosition/InsideSW
   :outside-e Styler$LegendPosition/OutsideE})

(def themes
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
           position series-line-length visible]}]
  (doto-cond
   styler
   background-color (.setLegendBackgroundColor (colors background-color background-color))
   border-color (.setLegendBorderColor (colors border-color border-color))
   font (.setLegendFont font)
   padding (.setLegendPadding (int padding))
   position (.setLegendPosition (legend-positions position))
   series-line-length (.setLegendSeriesLineLength (int series-line-length))
   (not (nil? visible)) (.setLegendVisible visible)))

(defn- set-chart-title-style!
  [^Styler styler
   {:keys [box-background-color box-border-color box-visible
           font padding visible]}]
  (doto-cond
   styler
   box-background-color (.setChartTitleBoxBackgroundColor (colors box-background-color box-background-color))
   box-border-color (.setChartTitleBoxBorderColor (colors box-border-color box-border-color))
   (not (nil? box-visible)) (.setChartTitleBoxVisible (boolean box-visible))
   font (.setChartTitleFont font)
   padding (.setChartTitlePadding (int padding))
   visible (.setChartTitleVisible visible)))

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
   {:keys [background-color border-color border-visible content-size]}]
  (doto-cond
   styler
   background-color (.setPlotBackgroundColor (colors background-color background-color))
   border-color (.setPlotBorderColor (colors border-color border-color))
   (not (nil? border-visible)) (.setPlotBorderVisible (boolean border-visible))
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
      (let [{:keys [color stroke marker]} (series i)]
        (when color
          (aset series-colors i (colors color color)))
        (when stroke
          (aset series-lines i stroke))
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



;; TODO: Add in font as a shortcut to set all fonts not yet set.

(defn xy-chart
  "Returns an xy-chart"
  ([series]
   (xy-chart series {}))
  ([series
    {:keys [width height title theme render-style]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (XYChart. width height)]
     (doseq [[s-name data] series]
       (if (sequential? data)
         (apply add-series chart s-name data)
         (let [{:keys [x y error-bars style]} data
               {:keys [marker-color marker-type
                       line-color line-style line-width
                       fill-color show-in-legend render-style]} style]
           (doto-cond
            (if error-bars
              (add-series chart s-name x y error-bars)
              (add-series chart s-name x y))
            marker-color (.setMarkerColor (colors marker-color marker-color))
            marker-type (.setMarker (markers marker-type marker-type))
            line-color (.setLineColor (colors line-color line-color))
            line-style (.setLineStyle line-style)
            line-width (.setLineWidth (float line-width))
            fill-color (.setFillColor (colors fill-color fill-color))
            (not (nil? show-in-legend)) (.setShowInLegend (boolean show-in-legend))
            render-style (.setXYSeriesRenderStyle (xy-render-styles render-style))))))
     (doto-cond
      (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (xy-render-styles render-style)))
     (set-default-style! (.getStyler chart) styling)
     (doto-cond
      chart
      title (.setTitle title)))))

(defn pie-chart
  "Returns a pie chart"
  ([series]
   (pie-chart series {}))
  ([series
    {:keys [width height title circular theme render-style annotation-distance
            start-angle draw-all-annotations donut-thickness annotation-type]
     :or {width 640 height 500}
     :as styling}]
   {:pre [series]}
   (let [chart (PieChart. width height)]
     (doseq [[s-name num] series]
       (.addSeries chart s-name num))
     (doto-cond
      (.getStyler chart)
      theme (.setTheme (themes theme theme))
      render-style (.setDefaultSeriesRenderStyle (pie-render-styles render-style))
      (not (nil? circular)) (.setCircular (boolean circular))
      (not (nil? draw-all-annotations)) (.setDrawAllAnnotations (boolean draw-all-annotations))
      annotation-distance (.setAnnotationDistance (double annotation-distance))
      donut-thickness (.setDonutThickness (double donut-thickness))
      start-angle (.setStartAngleInDegrees (double start-angle))
      annotation-type (.setAnnotationType (pie-annotation-types annotation-type)))
     (set-default-style! (.getStyler chart) styling)
     (doto-cond
      chart
      title (.setTitle title)))))

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
