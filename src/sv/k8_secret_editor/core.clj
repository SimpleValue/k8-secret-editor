(ns sv.k8-secret-editor.core
  (:require [babashka.process :as process]
            [cheshire.core :as json]
            [sv.k8-secret-editor.util :as util]))

;; Concept:
;;
;; Utils to edit Kubernetes secrets.

(defn check-exit-code
  [process]
  (let [process-result @process
        exit (:exit process-result)]
    (if (not= exit 0)
      (throw (ex-info "non-zero exit code"
                      {:err (slurp (:err process-result))
                       :exit (:exit process-result)}))
      process-result)))

(defn get-secrets
  [params]
  (-> (process/process
       ["kubectl"
        "get" "secret"
        "-o" "json"])
      (check-exit-code)
      (:out)
      (slurp)
      (json/parse-string true)))

(defn get-secret
  [{:keys [name] :as params}]
  (->> (get-secrets params)
       (:items)
       (filter (fn [secret]
                 (= (get-in secret
                            [:metadata
                             :name])
                    name)))
       (first)))

(defn transform-data-vals
  [secret f]
  (update secret
          :data
          (fn [kvs]
            (into {}
                  (map (fn [[k v]]
                         [k (f v)]))
                  kvs))))

(defn to-text-vals
  [secret]
  (transform-data-vals secret
                       util/base64-decode))

(defn to-base64-vals
  [secret]
  (transform-data-vals secret
                       util/base64-encode))

(defn get-text-secret
  [{:keys [name] :as params}]
  (some-> (get-secret params)
          (to-text-vals)))

(defn apply!
  [kubernetes-data]
  (-> (process/process
       ["kubectl" "apply" "-f" "-"]
       {:in (json/generate-string kubernetes-data)})
      (check-exit-code)
      (select-keys [:out :err :exit])
      (update :out slurp)
      (update :err slurp))
  )

(defn swap-text-secret!
  [{:keys [name] :as params} f & args]
  (-> (get-text-secret params)
      (update :data
              (fn [data]
                (apply f data args)))
      (to-base64-vals)
      (apply!))
  )

(defn new-text-secret
  [{:keys [name data]}]
  {:apiVersion "v1"
   :data data
   :metadata {:name name}
   :kind "Secret"
   :type "Opaque"})

(defn create-text-secret!
  [{:keys [name data] :as params}]
  (-> params
      (new-text-secret)
      (to-base64-vals)
      (apply!)))

(comment
  (get-secret {:name "dev-db-secret"})

  (def text-secret-params
    {:name "dev-db-secret"
     :data {:username "some-user"
            :password "some-pass"}})

  (new-text-secret text-secret-params)

  (create-text-secret! text-secret-params)

  (get-text-secret {:name "dev-db-secret"})

  (apply!
   (-> (get-text-secret {:name "dev-db-secret"})
       (assoc-in [:data :password]
                 (str (java.util.Date.)))
       (to-base64-vals))
   )

  (swap-text-secret! {:name "dev-db-secret"}
                     assoc :password "new-secret")
  )
