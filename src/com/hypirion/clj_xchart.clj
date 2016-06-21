(ns com.hypirion.clj-xchart
  (:import (org.knowm.xchart XYChart
                             PieChart
                             XYSeries$XYSeriesRenderStyle
                             XChartPanel)
           (org.knowm.xchart.style Styler$LegendPosition)
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

(def legend-positions
  {:inside-n Styler$LegendPosition/InsideN
   :inside-ne Styler$LegendPosition/InsideNE
   :inside-nw Styler$LegendPosition/InsideNW
   :inside-se Styler$LegendPosition/InsideSW
   :inside-sw Styler$LegendPosition/InsideSW
   :outside-e Styler$LegendPosition/OutsideE})

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
  [styler
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

(defn xy-chart
  "Returns an xy-chart"
  [{:keys [width height title series legend]
    :or {width 640 height 500}}]
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
     legend (set-legend! legend))
    (doto-cond
     chart
     title (.setTitle title))))

(defn pie-chart
  "Returns a pie chart"
  [{:keys [width height title series circular legend]
    :or {width 640 height 500}}]
  {:pre [series]}
  (let [chart (PieChart. width height)]
    (doseq [[s-name num] series]
      (.addSeries chart s-name num))
    (doto-cond
     (.getStyler chart)
     (not (nil? circular)) (.setCircular circular)
     legend (set-legend! legend))
    (doto-cond
     chart
     title (.setTitle title))))

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
