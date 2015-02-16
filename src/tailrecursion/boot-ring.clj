(ns tailrecursion.boot-ring
  {:boot/export-tasks true}
  (:require
   [clojure.java.io                 :as io]
   [boot.pod                        :as pod]
   [boot.util                       :as util]
   [boot.core                       :as core :refer [deftask]]
   [ring.adapter.jetty              :as jetty]
   [ring.middleware.cors            :as cors]
   [ring.middleware.session         :as session]
   [ring.middleware.session.cookie  :as cookie]
   [ring.middleware.reload          :as reload]
   [ring.middleware.head            :as head]
   [ring.middleware.file            :as file]
   [ring.middleware.file-info       :as file-info]))

(def server     (atom nil))
(def middleware (atom identity))

(defn ring-task [mw]
  (swap! middleware comp mw)
  identity)

(defn handle-404
  [req]
  {:status 404 :headers {} :body "Not Found :("})

(deftask cors
  "Ring task to support cross-origin requests.

  allowed-origins is a list of regular expressions matching the permitted 
  origin(s) or a single function which takes the origin as its argument 
  to return a truthy value if it is to be allowed."
  [o origins  SYM sym "permitted origins"]
  (ring-task #(apply cors/wrap-cors % allowed-origins)))

(deftask files
  "Ring task to serve static files.

  The document root can be specified via the optional `docroot` argument. If not
  specified the :target-path will be used."
  [d docroot  PATH str "the directory for static files"]
  (let [root (or docroot (core/get-env :target-path))]
    (.mkdirs (io/file root))
    (ring-task #(-> (file/wrap-file % root) (file-info/wrap-file-info)))))

(deftask head
  "Ring task to handle HEAD requests."
  []
  (ring-task head/wrap-head))

(deftask session-cookie
  "Ring task to support client sessions via a session cookie.

  The optional `key` argument sets the cookie encryption key to the given 16
  character string."
  [s session-key  STR str "a 16 byte session key"]
  (let [dfl-key "a 16-byte secret"
        store   (cookie/cookie-store {:key (or key dfl-key)})]
    (ring-task #(session/wrap-session % {:store store}))))

(deftask dev-mode
  "Ring task to add the `X-Dev-Mode` header to all responses."
  []
  (let [set-dev #(assoc % "X-Dev-Mode" "true")
        add-hdr #(update-in % [:headers] set-dev)]
    (ring-task #(comp add-hdr %))))

(deftask reload
  "Ring task to support reloading of namespaces during development."
  []
  (ring-task #(reload/wrap-reload % {:dirs (vec (core/get-env :src-paths))})))

(deftask jetty
  "Ring task to start a local Jetty server.

  The `:port` option specifies which port the server should listen on (default
  8000). The `:join?` option specifies whether Jetty should run in the foreground
  (default false)."
  [p port PORT int "the port to listen"]
  (let [port  (or port 8000)
        join? false]
    (println
      "Jetty server stored in atom here: #'tailrecursion.boot.task.ring/server...")
    (core/with-pre-wrap
      (swap! server
        #(or % (-> (@middleware handle-404)
                (jetty/run-jetty {:port port :join? join?})))))))

(deftask dev-server
  "Ring task to start a local development stack.

  The optional `:port` and `:join?` options are passed to the `jetty` task,
  `:key` to the `session-cookie` task, and `:docroot` to the `files` task."
  [d docroot        PATH str "docroot for static files"
   p port           PORT int "the port to listen"
   s session-key    STR  str "a 16 byte session key"]
  (let [port        (or port 8000)
        session-key (or session-key "a 16-byte secret")
        join?       false
        docrooot    (or docroot (core/get-env :target-path))]
    (comp (head) 
          (dev-mode) 
          (session-cookie session-key) 
          (files docroot) 
          (jetty :port port :join? join?))))
