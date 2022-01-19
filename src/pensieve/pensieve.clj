(ns pensieve.pensieve
  (:require
   [clj-http.client :as client]
   [clojure.repl :refer :all]
   [pensieve.util :as u])
  (:gen-class))

(defn api-get-pensieve-filenames []
  (-> (client/get "https://pensieve.ceo/api/filenames/list/all"
                  {:as :json})
      :body :message))

(def mapi-get-pensieve-filenames (memoize api-get-pensieve-filenames))

;; Pull some remote values
(defn api-get-pensieve-pics [filename]
  (-> (client/get
       ;; "https://pensieve.ceo/api/filenames/list/all"
       (str "https://pensieve.ceo/api/filename/" filename "/images")
       {:as :json})
      :body :message))
;; This gets the body and then gets the message from the body

(def mapi-get-pensieve-pics (memoize api-get-pensieve-pics))

;; How does the threading macro work with a :body key as the first form?
;; I think it converts :body to (:body).
;; So this is syntax sugar, for then extracting the value associated with the key.
(defn api-get-pensieve-pic [filename s]
  (-> (client/get
       (str "https://images.pensieve.ceo/filenames/" filename "/" s)
       ;; {:as :stream}
       {:as :byte-array})
      :body))
;; So it might as well be written like this:
;; (defn api-get-pensieve-pic [filename s]
;;   (:body (client/get
;;           (str "https://images.pensieve.ceo/filenames/" filename "/" s)
;;           ;; {:as :stream}
;;           {:as :byte-array})))

(def mapi-get-pensieve-pic (memoize api-get-pensieve-pic))

(defn get-pensieve-pics [filename]
  (mapi-get-pensieve-pics filename))

(defn get-pensieve-pic
  "Ensure that P has the leading slash.
  Sample: /whippet/n02091134_10242.jpg"
  [p]
  (let [[_ filename s] (u/split-by-slash p)]
    (mapi-get-pensieve-pic filename s)))

(defn get-pensieve-filenames []
  (->> (mapi-get-pensieve-filenames)
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

(defn get-few-pensieve-pics [filename]
  (into [] (take 10 (get-pensieve-pics filename))))

(defn get-filename-only [s]
  (nth (reverse (u/split-by-slash s)) 0))

(defn get-pics-clean [filename]
  (doall
   (into [] (map get-filename-only (get-few-pensieve-pics filename)))))

(def http-cache (atom {}))

(defn set-http-cache! [filename]
  (swap! http-cache conj {(keyword filename) (get-pics-clean filename)}))

(defn get-pensieve-list! [filename]
  (let [kw (keyword filename)]
    (if (kw @http-cache)
      (kw @http-cache)
      (kw (set-http-cache! filename)))))

(defn pensieve-exists?
  "Check against the path string, S always has a leading slash.
  Sample: /whippet/n02091134_10918.jpg"
  [p]
  (let [[_ filename s] (u/split-by-slash p)]
    (let [pensieves ((keyword filename) @http-cache)]
      (u/member s pensieves))))
