(defproject zole "0.1.0-SNAPSHOT"
  :description "Zole GUI client"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391" :exclusions [org.clojure/tools.reader]]
                 [jarohen/chord "0.7.0"]
                 [reagent "0.5.1" :exclusions [org.clojure/tools.reader]]
                 [reagent-forms "0.5.22"]
                 [reagent-utils "0.1.7"]
                 [jarohen/chord "0.7.0"]
                 [cljsjs/bootstrap "3.3.6-0"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.1.7" :exclusions [org.clojure/tools.reader]]]

  :plugins [[lein-figwheel "0.5.8"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [lein-asset-minifier "0.2.7" :exclusions [org.clojure/clojure]]]

  :minify-assets
  {:assets
   {"resources/public/css/zole.min.css" "resources/public/css/zole.css"
    "resources/public/css/bootstrap.min.css" "resources/public/css/bootstrap.css"}}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                :figwheel {:open-urls ["http://localhost:3449/index.html"]}

                :compiler {:main zole.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/zole.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/zole.js"
                           :main zole.core
                           :optimizations :advanced
                           :pretty-print false
                           :closure-defines {"zole.core.dynamic_ws_port" true}}}]}

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.8.2"]
                                  [figwheel-sidecar "0.5.8"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {; for nREPL dev you really need to limit output
                                  :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
