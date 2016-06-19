(ns com.hypirion.clj-xchart
  (:import (org.knowm.xchart XYChart
                             XChartPanel)
           (javax.swing JPanel
                        JFrame
                        SwingUtilities)
           java.awt.GridLayout))

(defn xy-chart
  "Returns an xy-chart"
  [{:keys [width height series]
    :or {width 540 height 500}}]
  {:pre [series]}
  (let [chart (XYChart. width height)]
    (doseq [[s-name [x-data y-data error-bars]] series]
      (if error-bars
        (.addSeries chart s-name x-data y-data error-bars)
        (.addSeries chart s-name x-data y-data)))
    chart))

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
