(ns pensieve.core
  (:require
   [clojure.repl :refer :all]
   [pensieve.fuse-pensieve :as fpensieve])
  (:gen-class))

(use '[clojure.java.shell :only [sh]])

(defn cmd
  ""
  [& args]
  (clojure.string/join
   " "
   (map (fn [s] (->
                 (sh "q" :in (str s))
                 :out)) args)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [[type dir] args]
    ;; Wrapping into a shell command is unneccessary with sh
    ;; (sh (cmd "mkdir" "-p" dir))
    (sh "mkdir" "-p" dir)
    ;; (sh "sh" "-c" (str (cmd "cmd" "mkdir" "-p" dir) " | pen-tv"))
    (cond
      ;; Make multiple modes. For example:
      ;; - a computer's filesystem
      ;; - chatbot memories

      ;; This mode is called 'pensieve'.
      ;; It's the prototype, and will simply imagine a filesystem.
      (= "pensieve" type) (fpensieve/main dir)
      :else (println "Please use a known system as first arg [pensieve]"))))
