(ns sv.k8-secret-editor.edit
  (:require [sv.k8-secret-editor.core :as core]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defn pprint-map [m]
  (println "{")
  (doall (for [[k v] m] (do (print " ")
                            (pr (str/replace (str k)
                                             #"^:"
                                             ""))
                            (print " ")
                            (pr v)
                            (println))))
  (print "}"))

(defn edit-text-secret!
  [{:keys [name ready] :as params}]
  (when-not (core/get-secret params)
    (core/create-text-secret! (assoc params
                                     :data
                                     {})))
  (let [text-secret (core/get-text-secret params)
        tmp-file (java.io.File/createTempFile "secret-"
                                              ".edn")]
    (.deleteOnExit tmp-file)
    (try
      (spit tmp-file
            (with-out-str
              (pprint-map (:data text-secret))))
      (when (ready {:tmp-file tmp-file})
        (let [new-data (edn/read-string (slurp tmp-file))]
          (-> text-secret
              (assoc :data new-data)
              (core/to-base64-vals)
              (core/apply!))))
      (finally
        (.delete tmp-file)))
    ))

(defn terminal-edit-text-secret!
  [params]
  (try
    (let [result (edit-text-secret!
                  (assoc params
                         :ready (fn [{:keys [tmp-file]}]
                                  (println "Wrote secret to:"
                                           (.getCanonicalPath tmp-file))
                                  (println "Edit it, then press return to save it.")
                                  (= (.read *in*)
                                     10))))]
      (System/exit (:exit result)))
    (catch Throwable e
      (prn "Error:"
           e)
      (System/exit 1)))
  )
