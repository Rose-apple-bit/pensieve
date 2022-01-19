(defproject pensieve "0.1.0-SNAPSHOT"
  :description "PenSieve is a filesystem for automatic prompting with Pen.el"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.9.1"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [cheshire "5.8.1"]
                 [org.clojure/tools.reader "1.3.2"]
                 [com.github.serceman/jnr-fuse "0.5.2.1"]]
  :repositories {"bintray" "https://jcenter.bintray.com"}
  :main ^:skip-aot pensieve.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
