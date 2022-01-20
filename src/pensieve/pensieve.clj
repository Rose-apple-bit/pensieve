(ns pensieve.pensieve
  (:require
   [clj-http.client :as client]
   [clojure.repl :refer :all]
   [pensieve.util :as u]
   ;; [clojure.data.json :as json]
   [cheshire.core :as json])
  (:gen-class))

(use '[clojure.java.shell :only [sh]])

(defn cmd
  ""
  [& args]
  (clojure.string/join
   " "
   (map (fn [s] (->
                 (sh "q" :in s)
                 :out)) args)))

(defn api-get-pensieve-directories []
  (comment
    (-> (client/get "https://dog.ceo/api/breeds/list/all"
                    {:as :json})
        :body :message))
  (comment
    (-> (sh "unbuffer" "penf" "-u" "-nto" "--pool" "-j"
            "pf-list-subdirectories/2"
            "/dumbledores_adventures/"
            ;; Existing dirs. Frustratingly, when empty, this will instead use the default
            "")))
  (json/decode
   (-> (sh "unbuffer" "penf" "-u" "-nto" "--pool" "-j"
           "pf-list-subdirectories/1"
           "/dumbledores_adventures/")
       :out)))

(def mapi-get-pensieve-directories (memoize api-get-pensieve-directories))

;; Pull some remote values
(defn api-get-pensieve-filenames [filename]
  (comment
    (-> (client/get
         ;; "https://dog.ceo/api/breeds/list/all"
         (str "https://dog.ceo/api/breed/" filename "/images")
         {:as :json})
        :body :message))
  (map
   (fn [s] (str s ".txt"))
   (json/decode
    (-> (sh "unbuffer" "penf" "-u" "-nto" "--pool" "-j"
            "pf-list-subdirectories/1"
            (str "/dumbledores_adventures/" filename "/"))
        :out))))
;; This gets the body and then gets the message from the body

(def mapi-get-pensieve-filenames (memoize api-get-pensieve-filenames))

;; How does the threading macro work with a :body key as the first form?
;; I think it converts :body to (:body).
;; So this is syntax sugar, for then extracting the value associated with the key.
(defn api-get-pensieve-file [filename s]
  (-> (client/get
       (str "https://images.dog.ceo/breeds/" filename "/" s)
       ;; {:as :stream}
       {:as :byte-array})
      :body))
;; So it might as well be written like this:
;; (defn api-get-pensieve-file [filename s]
;;   (:body (client/get
;;           (str "https://images.dog.ceo/breeds/" filename "/" s)
;;           ;; {:as :stream}
;;           {:as :byte-array})))

(def mapi-get-pensieve-file (memoize api-get-pensieve-file))

(defn get-pensieve-filenames [filename]
  (mapi-get-pensieve-filenames filename))

(defn get-pensieve-file
  "Ensure that P has the leading slash.
  Sample: /whippet/n02091134_10242.jpg"
  [p]
  (let [[_ filename s] (u/split-by-slash p)]
    (mapi-get-pensieve-file filename s)))

(defn get-pensieve-filenames []
  (->> (mapi-get-pensieve-directories)
       keys
       (map #(subs (str %) 1))
       (into [])))

(def filenames-atom (atom nil))

(defn set-filenames-atom! []
  (reset! filenames-atom (get-pensieve-filenames)))

(defn get-files []
  (if @filenames-atom @filenames-atom
      (set-filenames-atom!)))

(defn filename-exists? [path]
  (u/member (subs path 1) (get-files)))

(defn get-few-pensieve-filenames [filename]
  (into [] (take 10 (get-pensieve-filenames filename))))

(defn get-filename-only [s]
  (nth (reverse (u/split-by-slash s)) 0))

(defn get-file-list-clean [filename]
  (doall
   (into [] (map get-filename-only (get-few-pensieve-filenames filename)))))

(def file-listing-cache (atom {}))

(defn set-file-listing-cache! [filename]
  (swap! file-listing-cache conj {(keyword filename) (get-file-list-clean filename)}))

(defn get-pensieve-list! [filename]
  (let [kw (keyword filename)]
    (if (kw @file-listing-cache)
      (kw @file-listing-cache)
      (kw (set-file-listing-cache! filename)))))

(defn file-exists?
  "Check against the path string, S always has a leading slash.
  Sample: /whippet/n02091134_10918.jpg"
  [p]
  (let [[_ filename s] (u/split-by-slash p)]
    (let [files ((keyword filename) @file-listing-cache)]
      (u/member s files))))
