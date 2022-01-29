(ns servico-clojure.components.servidor
  (:require [io.pedestal.http :as http]
            [io.pedestal.test :as test]
            [io.pedestal.interceptor :as i]
            [com.stuartsierra.component :as component]))

(defonce server (atom nil))

(defn start-server [service-map]
  (reset! server (http/start (http/create-server service-map))))

(defn test-request [verb url]
  (test/response-for (::http/service-fn @server) verb url))

(defn stop-server []
  (http/stop @server))

(defn restart-server [service-map]
  (stop-server)
  (start-server service-map))

(defrecord Servidor [database rotas]
  component/Lifecycle

  (start [this]
    (println "Start servidor")
    (let [assoc-store (fn [context]
                        (update context :request assoc :store (:store database)))
          db-interceptor {:name  :db-interceptor
                          :enter assoc-store}
          service-map-base {::http/routes (:endpoints rotas)
                            ::http/port   9999
                            ::http/type   :jetty
                            ::http/join?  false}
          service-map (-> service-map-base
                          (http/default-interceptors)
                          (update ::http/interceptors conj (i/interceptor db-interceptor)))]
      (try (start-server service-map) (catch Exception e (println "Erro ao executar start" (.getMessage e))))
      (try (restart-server service-map) (catch Exception e (println "Erro ao executar restart" (.getMessage e))))
      (assoc this :test-request test-request)))

  (stop [this]
    (assoc this :test-request nil)))

(defn new-servidor []
  (map->Servidor {}))
