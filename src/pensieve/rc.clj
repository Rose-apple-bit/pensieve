(ns pensieve.rc
  (:require
   [clojure.repl :refer :all])
  (:gen-class))

(defn get-xdg-config-home []
  (or (System/getenv "XDG_CONFIG_HOME")
      (System/getProperty "user.home")))

(defn get-rc-file-raw []
  (let [defaults (read-string (slurp "conf/default-rc.edn"))
        home-rc (format "%s/.pensieverc" (System/getProperty "user.home"))
        xdg-rc (format "%s/pensieve/pensieverc" (get-xdg-config-home))]
    (conj
      defaults
      (if (.exists (clojure.java.io/file home-rc))
        (read-string (slurp home-rc)))
      (if (.exists (clojure.java.io/file xdg-rc))
        (read-string (slurp xdg-rc))))))

(defn get-rc []
  (let [rc (get-rc-file-raw)]
    rc))
