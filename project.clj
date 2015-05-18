(defproject quiescent-examples "0.1.0"

  :dependencies [
    [org.clojure/clojure "1.7.0-beta2"]
    [org.clojure/clojurescript "0.0-3269"]
    [cljsjs/react "0.12.2-8"]
    [quiescent "0.1.4"]
    [sablono "0.3.4"]]

  :plugins [
    [lein-cljsbuild "1.0.5"]]

  :source-paths ["src"]

  :clean-targets [
    "target"
    "public/js/main.js"
    "public/js/main.min.js"]

  :cljsbuild {
    :builds {
      :dev {
        :source-paths ["src-cljs"]
        :compiler {
          :output-to "public/js/main.js"
          :optimizations :whitespace }}

      :prod {
        :source-paths ["src-cljs"]
        :compiler {
          :output-to "public/js/main.min.js"
          :optimizations :advanced
          :pretty-print false }}}})
