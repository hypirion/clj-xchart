# Render Options

XChart ships with roughly a million different options, and clj-xchart attempts
to do the same. However, instead of performing to mutable modifications on the
chart itself, you pass in a map of styling values in clj-xchart.

All charts support the following options:

```clj
{:width "Width of chart in pixels (default: 640)"
 :height "Height of chart in pixels (default: 500)"
 :title "Title of the chart, written above the chart itself"
 :theme "Styling theme for this chart"
 :render-style "Chart-specific rendering style."
 :annotations-font "Font for annotations"
 :annotations? "Boolean, whether or not to view annotations"
 :chart {:background-color "The background color of the chart"
         :font-color "The color of the font in this chart"
         :padding "The chart padding in ints"
         :title {:box {:background-color "Background color for the title box"
                       :border-color "Border color for the title box"
                       :visible? "Whether or not to show the title box"}
                 :font "Font of the title"
                 :padding "The chart title padding"
                 :visible? "Whether or not to show a title"}}
 :legend {:background-color "The background color of the legend"
          :border-color "The border color of the legend"
          :font "The font of the series names within the legend"
          :padding "Legend padding"
          :position "The position of the legend. By default :outside-e"
          :series-line-length "The length of the series line, if applicable"
          :visible? "Whether the legend is visible or not"}
 :plot {:background-color "The background color for the plot"
        :border-color "The border color for the plot"
        :border-visible? "Whether or not to show the plot border"
        :content-size "The content size of the plot inside the plot area of the
                       chart. Must be between 0.0 and 1.0"}
 :series [{:color "Color of the nth series. Can be overwritten if the series
                   itself contains color styling information."
           :stroke "Sets the line stroke for the nth series. Can be overwritten
                    if the series itself contains line stroke information."
           :marker "Sets the marker for the nth series. Can be overwritten
                    if the series itself contains marker information."}]}
```


Wherever you can specify a color, you must use a java.awt.Color. For
convenience, you can also use the following keywords which represent their
respective color:

```clj
#{:blue :black :cyan :dark-gray :gray :grey :green :light-gray
  :magenta :orange :pink :red :white :yellow}
```

Wherever you set a marker, you could make your own by subclassing
`org.knowm.xchart.style.markers.Marker`. That is a lot of effort though, so it's
probably easier to just use the ones shipped with xchart. Here they are:

```clj
#{:circle :diamond :none :square :triangle-up :triangle-down}
```

For text alignments, only the following options can be used:

```clj
#{:centre :left :right}
```

Legends can be placed at different positions. Here are all the possible
locations you can place them:

```clj
#{:inside-n :inside-ne :inside-nw :inside-se :inside-sw :outside-e}
```

Note that, although `:inside-n` is available, `:inside-s` isn't. I'd recommend
to look up the options here or try it out before blindly shipping a different
legend position to production.

By default, the legend is placed `:outside-e`.

Finally, themes. You can make your own theme (Which will not be discussed here,
go to XChart's documentation if you're interested) or use the themes bundled
with XChart by default:

```clj
#{:ggplot2 :matlab :xchart}
```

The `:xchart` option is chosen by default.
