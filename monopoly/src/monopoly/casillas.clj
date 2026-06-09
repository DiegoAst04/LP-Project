(ns monopoly.casillas
  (:require [monopoly.jugadores :refer [estado-juego enviar-carcel!
                                        liberar-carcel! turno-en-carcel!]]
            [monopoly.banco :refer [cobrar-jugador! pagar-jugador!
                                    cobrar-renta! comprar-propiedad!
                                    aplicar-ganador-subasta! procesar-puja
                                    cobrar-con-rescate!]]
            [monopoly.cartas :refer [aplicar-carta-suerte!
                                     aplicar-carta-arca-comunal!]]))

;; ─── Helpers ───────────────────────────────────────────────

(defn propiedad-libre? [id-casilla]
  (let [prop (get-in @estado-juego [:propiedades id-casilla])]
    (or (nil? prop) (nil? (:dueno prop)))))

;; ─── Manejo de carcel ──────────────────────────────────────

(defn manejar-turno-en-carcel! [id-jugador dados]
  (let [jugador  (get-in @estado-juego [:jugadores id-jugador])
        turnos   (:turnos-en-carcel jugador)
        dado1    (first dados)
        dado2    (second dados)
        dobles?  (= dado1 dado2)]
    (cond
      (:tiene-libre-carcel jugador)
      (do
        (swap! estado-juego assoc-in [:jugadores id-jugador :tiene-libre-carcel] false)
        (liberar-carcel! id-jugador)
        {:salio true :mensaje "Usas tu carta de salir gratis"})

      dobles?
      (do
        (liberar-carcel! id-jugador)
        {:salio true :mensaje "Sacaste dobles, sales de la carcel"})

      (>= turnos 2)
      (do
        (cobrar-con-rescate! id-jugador 50)
        (liberar-carcel! id-jugador)
        {:salio true :mensaje "Llevas 3 turnos en carcel, pagas $50 para salir"})

      :else
      (do
        (turno-en-carcel! id-jugador)
        {:salio false :mensaje "Sigues en la carcel"}))))

;; ─── Resolver cada tipo de casilla ─────────────────────────

(defn resolver-salida! [id-jugador]
  (pagar-jugador! id-jugador 200)
  {:tipo :salida
   :mensaje "Pasas por Salida, cobras $200"})

(defn resolver-propiedad! [id-jugador casilla dados tablero]
  (let [id-casilla (:id casilla)]
    (if (propiedad-libre? id-casilla)
      {:tipo      :compra-disponible
       :casilla   casilla
       :mensaje   (str "¿Quieres comprar " (:nombre casilla)
                       " por $" (:precio casilla) "?")}
      (cobrar-renta! id-jugador casilla (apply + dados) tablero))))

(defn resolver-estacion! [id-jugador casilla dados tablero]
  (let [id-casilla (:id casilla)]
    (if (propiedad-libre? id-casilla)
      {:tipo    :compra-disponible
       :casilla casilla
       :mensaje (str "¿Quieres comprar " (:nombre casilla)
                     " por $" (:precio casilla) "?")}
      (cobrar-renta! id-jugador casilla (apply + dados) tablero))))

(defn resolver-servicio! [id-jugador casilla dados tablero]
  (let [id-casilla (:id casilla)]
    (if (propiedad-libre? id-casilla)
      {:tipo    :compra-disponible
       :casilla casilla
       :mensaje (str "¿Quieres comprar " (:nombre casilla)
                     " por $" (:precio casilla) "?")}
      (cobrar-renta! id-jugador casilla (apply + dados) tablero))))

(defn resolver-impuesto! [id-jugador casilla]
  (let [monto     (:monto casilla)
        resultado (cobrar-con-rescate! id-jugador monto)]
    (assoc resultado
           :tipo :impuesto
           :mensaje (str "Pagas impuesto de $" monto)
           :monto monto)))

(defn resolver-ir-carcel! [id-jugador]
  (enviar-carcel! id-jugador)
  {:tipo :ir-carcel
   :mensaje "Vas directamente a la carcel"})

(defn resolver-suerte! [id-jugador dados]
  (aplicar-carta-suerte! id-jugador dados))

(defn resolver-arca-comunal! [id-jugador dados]
  (aplicar-carta-arca-comunal! id-jugador dados))

(defn resolver-esquina! []
  {:tipo :esquina
   :mensaje "Parque Gratis, no pasa nada"})

(defn resolver-carcel-visita! []
  {:tipo :carcel-visita
   :mensaje "Solo de visita"})

;; ─── Dispatcher principal ──────────────────────────────────

(defn resolver-casilla! [id-jugador casilla dados tablero]
  (let [resultado
        (case (:tipo casilla)
          :salida        (resolver-salida! id-jugador)
          :propiedad     (resolver-propiedad! id-jugador casilla dados tablero)
          :estacion      (resolver-estacion! id-jugador casilla dados tablero)
          :servicio      (resolver-servicio! id-jugador casilla dados tablero)
          :impuesto      (resolver-impuesto! id-jugador casilla)
          :ir-carcel     (resolver-ir-carcel! id-jugador)
          :suerte        (resolver-suerte! id-jugador dados)
          :arca-comunal  (resolver-arca-comunal! id-jugador dados)
          :esquina       (resolver-esquina!)
          :carcel-visita (resolver-carcel-visita!)
          {:tipo :desconocida
           :mensaje "Casilla desconocida"})]
    (assoc resultado :nombre-casilla (:nombre casilla))))