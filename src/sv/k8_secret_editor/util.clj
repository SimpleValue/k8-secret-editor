(ns sv.k8-secret-editor.util)

(defn base64-encode [to-encode]
  (.encodeToString (java.util.Base64/getEncoder)
                   (.getBytes to-encode)))

(defn base64-decode [to-decode]
  (String. (.decode (java.util.Base64/getDecoder)
                    to-decode)
           "UTF-8"))
