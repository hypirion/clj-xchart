(defproject com.hypirion/clj-xchart "0.3.0-SNAPSHOT"
  :description "XChart wrapper for Clojure"
  :url "https://github.com/hyPiRion/clj-xchart"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.knowm.xchart/xchart "3.2.1"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :plugins [[lein-codox "0.10.1"]]
  :deploy-repositories [["releases" :clojars]]
  :codox {:source-uri "https://github.com/foo/bar/blob/{version}/{filepath}#L{line}"}
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
