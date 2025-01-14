(defproject org.clojars.flexport-clojure-eng/salesforce "2.0.1-SNAPSHOT"
  :description "A clojure library for accessing the salesforce api"
  :url "https://www.flexport.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [clj-http "3.10.0"]
                 [cheshire "5.8.1"]]

  :plugins [[lein-cljfmt "0.6.4"]
            [lein-ancient "0.6.15"]] ; lein-ancient is for detecting and fixing outdated dependencies. See https://github.com/xsc/lein-ancient

  :profiles

  {:test         {:dependencies [[clj-http-fake "1.0.3"]]
                 :resource-paths ["env/test/resources"]}})