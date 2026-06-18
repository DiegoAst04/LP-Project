(ns monopoly.server
  (:require
   [compojure.core :refer [GET defroutes]]
   [compojure.route :as route]
   [org.httpkit.server :refer [run-server with-channel on-close send!]] 
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.util.response :refer [response]]
   [clojure.data.json :as json]                                        
   [monopoly.jugadores :refer [estado-juego]]                          
   [monopoly.core :as game]))

(defn cors [handler]
  (fn [request]
    (let [resp (handler request)]
      (-> resp
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, OPTIONS")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type")))))


(def connected-channels (atom #{}))


(defn ws-handler [request]
  (with-channel request channel
  
    (swap! connected-channels conj channel)
    (println "Nueva computadora conectada al juego. Total:" (count @connected-channels))
    
   
    (on-close channel (fn [status]
                        (swap! connected-channels disj channel)
                        (println "Computadora desconectada. Total restantes:" (count @connected-channels))))))


(add-watch estado-juego :websocket-broadcaster
           (fn [_key _ref _old-state _new-state]
             (let [estado-actual-json (json/write-str (game/estado-actual))]
             
               (doseq [channel @connected-channels]
                 (send! channel estado-actual-json)))))

(defroutes app-routes
  ;; Ruta para que el cliente se conecte al WebSocket
  (GET "/ws" request (ws-handler request))

  (GET "/estado" []
    (response (game/estado-actual)))

  (GET "/registrar/:nombre/:ficha/:cliente-id" [nombre ficha cliente-id]
    (response (game/registrar-jugador! nombre ficha cliente-id)))

  (GET "/listo/:cliente-id" [cliente-id]
    (response (game/marcar-listo! cliente-id)))

  (GET "/iniciar" []
    (response (game/iniciar-juego!)))

  (GET "/tirar-turno" []
    (response
     (if (empty? (:jugadores (:estado (game/estado-actual))))
       {:exito false
        :mensaje "No hay jugadores registrados"}
       (game/tirar-turno!))))
  
  (GET "/no-comprar/:casilla" [casilla]
    (response
     (game/accion-no-comprar!
      (Integer/parseInt casilla))))
  
  (GET "/pujar/:jugador/:puja" [jugador puja]
    (response
     (game/accion-puja!
      (Integer/parseInt jugador)
      (Integer/parseInt puja))))
  
  (GET "/comprar/:jugador/:casilla" [jugador casilla]
    (response
     (game/accion-comprar!
      (Integer/parseInt jugador)
      (Integer/parseInt casilla))))
  
  (GET "/terminar-turno" []
    (response (game/accion-terminar-turno!)))
  
  (GET "/construir-casa/:jugador/:casilla" [jugador casilla]
    (response
     (game/accion-construir-casa!
      (Integer/parseInt jugador)
      (Integer/parseInt casilla))))
  
  (GET "/construir-hotel/:jugador/:casilla" [jugador casilla]
    (response
     (game/accion-construir-hotel!
      (Integer/parseInt jugador)
      (Integer/parseInt casilla))))
  
  (GET "/hipotecar/:jugador/:casilla" [jugador casilla]
    (response
     (game/accion-hipotecar!
      (Integer/parseInt jugador)
      (Integer/parseInt casilla))))
  
  (GET "/levantar-hipoteca/:jugador/:casilla" [jugador casilla]
    (response
     (game/accion-levantar-hipoteca!
      (Integer/parseInt jugador)
      (Integer/parseInt casilla)))) 

  (route/resources "/")

  (route/not-found
   (response {:error "Ruta no encontrada"})))

(def app
  (-> app-routes
      wrap-json-response
      cors))


(defn iniciar-servidor []
  (run-server app {:port 8080
                   :ip "0.0.0.0"})) 
