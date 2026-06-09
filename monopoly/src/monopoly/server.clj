(ns monopoly.server
  (:require
   [compojure.core :refer [GET defroutes]]
   [compojure.route :as route]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.util.response :refer [response]]
   [monopoly.core :as game]))

(defn cors [handler]
  (fn [request]
    (let [resp (handler request)]
      (-> resp
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, OPTIONS")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type")))))

(defroutes app-routes
  (GET "/estado" []
    (response (game/estado-actual)))

  (GET "/registrar/:nombre" [nombre]
    (response (game/registrar-jugador! nombre)))

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
  (run-jetty app {:port 8080
                  :join? false}))