(ns pensieve.main
  (:require
   [clj-http.client :as client]
   [clojure.repl :refer :all]
   [pensieve.util :as u])
  (:gen-class))

(defn api-get-pensieve-breeds []
  (-> (client/get "https://pensieve.ceo/api/breeds/list/all"
                  {:as :json})
      :body :message))

(def mapi-get-pensieve-breeds (memoize api-get-pensieve-breeds))

;; Pull some remote values
(defn api-get-pensieve-pics [breed]
  (-> (client/get
       ;; "https://pensieve.ceo/api/breeds/list/all"
       (str "https://pensieve.ceo/api/breed/" breed "/images")
       {:as :json})
      :body :message))
;; This gets the body and then gets the message from the body

(def mapi-get-pensieve-pics (memoize api-get-pensieve-pics))

;; How does the threading macro work with a :body key as the first form?
;; I think it converts :body to (:body).
;; So this is syntax sugar, for then extracting the value associated with the key.
(defn api-get-pensieve-pic [breed s]
  (-> (client/get
       (str "https://images.pensieve.ceo/breeds/" breed "/" s)
       ;; {:as :stream}
       {:as :byte-array})
      :body))
;; So it might as well be written like this:
;; (defn api-get-pensieve-pic [breed s]
;;   (:body (client/get
;;           (str "https://images.pensieve.ceo/breeds/" breed "/" s)
;;           ;; {:as :stream}
;;           {:as :byte-array})))

(def mapi-get-pensieve-pic (memoize api-get-pensieve-pic))

(defn get-pensieve-pics [breed]
  (mapi-get-pensieve-pics breed))

(defn get-pensieve-pic
  "Ensure that P has the leading slash.
  Sample: /whippet/n02091134_10242.jpg"
  [p]
  (let [[_ breed s] (u/split-by-slash p)]
    (mapi-get-pensieve-pic breed s)))

(defn get-pensieve-breeds []
  (->> (mapi-get-pensieve-breeds)
       keys
       (map #(subs (str %) 1))
       (into [])))

(def breeds-atom (atom nil))

(defn set-breeds-atom! []
  (reset! breeds-atom (get-pensieve-breeds)))

(defn get-breeds []
  (if @breeds-atom @breeds-atom
      (set-breeds-atom!)))

(defn breed-exists? [path]
  (u/member (subs path 1) (get-breeds)))

(defn get-few-pensieve-pics [breed]
  (into [] (take 10 (get-pensieve-pics breed))))

(defn get-filename-only [s]
  (nth (reverse (u/split-by-slash s)) 0))

(defn get-pics-clean [breed]
  (doall
   (into [] (map get-filename-only (get-few-pensieve-pics breed)))))

(def http-cache (atom {}))

(defn set-http-cache! [breed]
  (swap! http-cache conj {(keyword breed) (get-pics-clean breed)}))

(defn get-pensieve-list! [breed]
  (let [kw (keyword breed)]
    (if (kw @http-cache)
      (kw @http-cache)
      (kw (set-http-cache! breed)))))

(defn pensieve-exists?
  "Check against the path string, S always has a leading slash.
  Sample: /whippet/n02091134_10918.jpg"
  [p]
  (let [[_ breed s] (u/split-by-slash p)]
    (let [pensieves ((keyword breed) @http-cache)]
      (u/member s pensieves))))
