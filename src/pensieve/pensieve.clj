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

(defn penf [& args]
  ;; This is how to run a macro at runtime
  (eval
   `(-> (sh "unbuffer" "penf" "-u" "-nto" "--pool" "-j"
            ~@args)
        :out)))

(defn api-get-pensieve-directories []
  (comment
    (-> (client/get "https://dog.ceo/api/breeds/list/all"
                    {:as :json})
        :body :message))
  (comment
    (penf "pf-list-subdirectories/2"
            "/dumbledores_adventures/"
            ;; Existing dirs. Frustratingly, when empty, this will instead use the default
            ""))
  (json/decode
   (penf "pf-list-subdirectories/1"
         "/dumbledores_adventures/")))

(def mapi-get-pensieve-directories (memoize api-get-pensieve-directories))

;; Pull some remote values
(defn api-get-pensieve-filenames [directory]
  (comment
    (-> (client/get
         ;; "https://dog.ceo/api/breeds/list/all"
         (str "https://dog.ceo/api/breed/" directory "/images")
         {:as :json})
        :body :message))
  (map
   (fn [s] (str s ".txt"))
   (json/decode
    (penf
     "pf-list-subdirectories/1"
     (str "/dumbledores_adventures/" directory "/")))))
;; This gets the body and then gets the message from the body

(def mapi-get-pensieve-filenames (memoize api-get-pensieve-filenames))

;; How does the threading macro work with a :body key as the first form?
;; I think it converts :body to (:body).
;; So this is syntax sugar, for then extracting the value associated with the key.
(defn api-get-pensieve-file [directory s]
  (comment (-> (client/get
       (str "https://images.dog.ceo/breeds/" directory "/" s)
       ;; {:as :stream}
       {:as :byte-array})
      :body))
  (penf
   "pf-generate-the-contents-of-a-new-file/6"
   ""
   directory
   "/dumbledores_adventures/"
   ;; ls (other files)
   ""
   ;; ls **/* (more files)
   ""
   ;; ls dirs here
   ""))
;; So it might as well be written like this:
;; (defn api-get-pensieve-file [directory s]
;;   (:body (client/get
;;           (str "https://images.dog.ceo/breeds/" directory "/" s)
;;           ;; {:as :stream}
;;           {:as :byte-array})))

(def mapi-get-pensieve-file (memoize api-get-pensieve-file))

(defn get-pensieve-filenames [directory]
  (mapi-get-pensieve-filenames directory))

(defn get-pensieve-file
  "Ensure that P has the leading slash.
  Sample: /whippet/n02091134_10242.jpg"
  [p]
  (let [[_ directory s] (u/split-by-slash p)]
    (mapi-get-pensieve-file directory s)))

(defn get-pensieve-directories []
  (mapi-get-pensieve-directories)
  ;; (->> (mapi-get-pensieve-directories)
  ;;      keys
  ;;      (map #(subs (str %) 1))
  ;;      (into []))
  )

(def directories-atom (atom nil))

(defn set-directories-atom! []
  (reset! directories-atom (get-pensieve-directories)))

(defn get-directories []
  (if @directories-atom @directories-atom
      (set-directories-atom!)))

(defn directory-exists? [path]
  (u/member (subs path 1) (get-directories)))

(defn get-few-pensieve-filenames [directory]
  (into [] (take 10 (get-pensieve-directories directory))))

(defn get-filename-only [s]
  (nth (reverse (u/split-by-slash s)) 0))

(defn get-file-list-clean [directory]
  (doall
   (into [] (map get-filename-only (get-few-pensieve-filenames directory)))))

(def file-listing-cache (atom {}))

(defn set-file-listing-cache! [directory]
  (swap! file-listing-cache conj {(keyword directory) (get-file-list-clean directory)}))

(defn get-pensieve-list! [directory]
  (let [kw (keyword directory)]
    (if (kw @file-listing-cache)
      (kw @file-listing-cache)
      (kw (set-file-listing-cache! directory)))))

(defn file-exists?
  "Check against the path string, S always has a leading slash.
  Sample: /whippet/n02091134_10918.jpg"
  [p]
  (let [[_ directory s] (u/split-by-slash p)]
    (let [files ((keyword directory) @file-listing-cache)]
      (u/member s files))))
