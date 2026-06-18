(ns monopoly.cartas
  (:require [monopoly.jugadores :refer [estado-juego enviar-carcel!]]
            [monopoly.banco :refer [pagar-jugador!
                                    transferir!
                                    cobrar-con-rescate!]]))

;;Cartas de Suerte

(def cartas-suerte
  [{:id 1
    :descripcion "¡Avanza hasta Boardwalk!"
    :efecto (fn [id _dados]
              (swap! estado-juego assoc-in [:jugadores id :posicion] 39)
              {:tipo :suerte
               :mensaje "Te mueves a Boardwalk"
               :nueva-posicion 39})}

   {:id 2
    :descripcion "¡Avanza hasta Salida! Cobras $200"
    :efecto (fn [id _dados]
              (swap! estado-juego assoc-in [:jugadores id :posicion] 0)
              (pagar-jugador! id 200)
              {:tipo :suerte
               :mensaje "Te mueves a Salida y cobras $200"
               :nueva-posicion 0})}

   {:id 3
    :descripcion "El banco te paga dividendos de $50"
    :efecto (fn [id _dados]
              (pagar-jugador! id 50)
              {:tipo :suerte
               :mensaje "Cobras $50 del banco"})}

   {:id 4
    :descripcion "Sal gratis de la carcel"
    :efecto (fn [id _dados]
              (swap! estado-juego assoc-in
                     [:jugadores id :tiene-libre-carcel] true)
              {:tipo :suerte
               :mensaje "Tienes una carta para salir gratis de la carcel"})}

   {:id 5
    :descripcion "Ve directamente a la carcel"
    :efecto (fn [id _dados]
              (enviar-carcel! id)
              {:tipo :suerte
               :mensaje "Vas directamente a la carcel"
               :nueva-posicion 10})}

   {:id 6
    :descripcion "Paga multa de $15"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 15)]
                (assoc resultado
                       :tipo :suerte
                       :mensaje "Pagas multa de $15")))}

   {:id 7
    :descripcion "Retrocede 3 casillas"
    :efecto (fn [id _dados]
              (let [pos-actual (get-in @estado-juego [:jugadores id :posicion])
                    pos-nueva  (mod (- pos-actual 3) 40)]
                (swap! estado-juego assoc-in [:jugadores id :posicion] pos-nueva)
                {:tipo :suerte
                 :mensaje (str "Retrocedes a casilla " pos-nueva)
                 :nueva-posicion pos-nueva}))}

   {:id 8
    :descripcion "Cobras $150 por servicios prestados"
    :efecto (fn [id _dados]
              (pagar-jugador! id 150)
              {:tipo :suerte
               :mensaje "Cobras $150"})}

   {:id 9
    :descripcion "Paga $100 de honorarios medicos"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 100)]
                (assoc resultado
                       :tipo :suerte
                       :mensaje "Pagas $100 de honorarios medicos")))}

   {:id 10
    :descripcion "Cobras $100 por apuesta ganada"
    :efecto (fn [id _dados]
              (pagar-jugador! id 100)
              {:tipo :suerte
               :mensaje "Cobras $100"})}

   {:id 11
    :descripcion "Tu prestamo vence, paga $150"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 150)]
                (assoc resultado
                       :tipo :suerte
                       :mensaje "Pagas $150 de prestamo")))}

   {:id 12
    :descripcion "Cobras $200 por ganar competencia de belleza"
    :efecto (fn [id _dados]
              (pagar-jugador! id 200)
              {:tipo :suerte
               :mensaje "Cobras $200"})}

   {:id 13
    :descripcion "Paga $50 de multa de transito"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 50)]
                (assoc resultado
                       :tipo :suerte
                       :mensaje "Pagas $50 de multa")))}

   {:id 14
    :descripcion "Cobras $50 de cada jugador"
    :efecto (fn [id _dados]
              (let [otros (filter #(and (not= (:id %) id)
                                        (not (:quebrado %)))
                                  (:jugadores @estado-juego))]
                (doseq [otro otros]
                  (transferir! (:id otro) id 50))
                {:tipo :suerte
                 :mensaje "Cobras $50 de cada jugador"}))}

   {:id 15
    :descripcion "Pagas $50 a cada jugador"
    :efecto (fn [id _dados]
              (let [otros (filter #(and (not= (:id %) id)
                                        (not (:quebrado %)))
                                  (:jugadores @estado-juego))]
                (doseq [otro otros]
                  (transferir! id (:id otro) 50))
                {:tipo :suerte
                 :mensaje "Pagas $50 a cada jugador"}))}

   {:id 16
    :descripcion "Avanza a la estacion mas cercana"
    :efecto (fn [id _dados]
              (let [pos-actual  (get-in @estado-juego [:jugadores id :posicion])
                    estaciones  [5 15 25 35]
                    mas-cercana (or (first (filter #(> % pos-actual) estaciones)) 5)]
                (swap! estado-juego assoc-in [:jugadores id :posicion] mas-cercana)
                {:tipo :suerte
                 :mensaje "Avanzas a la estacion mas cercana"
                 :nueva-posicion mas-cercana}))}])

;;Cartas de Arca Comunal

(def cartas-arca-comunal
  [{:id 1
    :descripcion "Recibes $200 de herencia"
    :efecto (fn [id _dados]
              (pagar-jugador! id 200)
              {:tipo :arca-comunal
               :mensaje "Cobras $200 de herencia"})}

   {:id 2
    :descripcion "Error bancario a tu favor, cobras $200"
    :efecto (fn [id _dados]
              (pagar-jugador! id 200)
              {:tipo :arca-comunal
               :mensaje "Cobras $200 por error bancario"})}

   {:id 3
    :descripcion "Paga factura medica de $100"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 100)]
                (assoc resultado
                       :tipo :arca-comunal
                       :mensaje "Pagas $100 de factura medica")))}

   {:id 4
    :descripcion "Sal gratis de la carcel"
    :efecto (fn [id _dados]
              (swap! estado-juego assoc-in
                     [:jugadores id :tiene-libre-carcel] true)
              {:tipo :arca-comunal
               :mensaje "Tienes carta para salir gratis de la carcel"})}

   {:id 5
    :descripcion "Ve directamente a la carcel"
    :efecto (fn [id _dados]
              (enviar-carcel! id)
              {:tipo :arca-comunal
               :mensaje "Vas directamente a la carcel"
               :nueva-posicion 10})}

   {:id 6
    :descripcion "Cobras $100 de fondos de seguro"
    :efecto (fn [id _dados]
              (pagar-jugador! id 100)
              {:tipo :arca-comunal
               :mensaje "Cobras $100"})}

   {:id 7
    :descripcion "Paga impuesto escolar de $150"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 150)]
                (assoc resultado
                       :tipo :arca-comunal
                       :mensaje "Pagas $150 de impuesto escolar")))}

   {:id 8
    :descripcion "Cobras $25 por servicios de consultoria"
    :efecto (fn [id _dados]
              (pagar-jugador! id 25)
              {:tipo :arca-comunal
               :mensaje "Cobras $25"})}

   {:id 9
    :descripcion "Es tu cumpleanos, cada jugador te paga $10"
    :efecto (fn [id _dados]
              (let [otros (filter #(and (not= (:id %) id)
                                        (not (:quebrado %)))
                                  (:jugadores @estado-juego))]
                (doseq [otro otros]
                  (transferir! (:id otro) id 10))
                {:tipo :arca-comunal
                 :mensaje "Es tu cumpleanos, cobras $10 de cada jugador"}))}

   {:id 10
    :descripcion "Vendes acciones y cobras $50"
    :efecto (fn [id _dados]
              (pagar-jugador! id 50)
              {:tipo :arca-comunal
               :mensaje "Cobras $50 por venta de acciones"})}

   {:id 11
    :descripcion "Pagas $100 de multa"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 100)]
                (assoc resultado
                       :tipo :arca-comunal
                       :mensaje "Pagas $100 de multa")))}

   {:id 12
    :descripcion "Cobras $100 de reembolso de impuestos"
    :efecto (fn [id _dados]
              (pagar-jugador! id 100)
              {:tipo :arca-comunal
               :mensaje "Cobras $100 de reembolso"})}

   {:id 13
    :descripcion "Paga $50 por reparaciones"
    :efecto (fn [id _dados]
              (let [resultado (cobrar-con-rescate! id 50)]
                (assoc resultado
                       :tipo :arca-comunal
                       :mensaje "Pagas $50 de reparaciones")))}

   {:id 14
    :descripcion "Premio por segundo lugar en concurso de belleza, cobras $10"
    :efecto (fn [id _dados]
              (pagar-jugador! id 10)
              {:tipo :arca-comunal
               :mensaje "Cobras $10"})}

   {:id 15
    :descripcion "Cobras $150 de renta"
    :efecto (fn [id _dados]
              (pagar-jugador! id 150)
              {:tipo :arca-comunal
               :mensaje "Cobras $150 de renta"})}

   {:id 16
    :descripcion "Pagas $50 a cada jugador por donacion"
    :efecto (fn [id _dados]
              (let [otros (filter #(and (not= (:id %) id)
                                        (not (:quebrado %)))
                                  (:jugadores @estado-juego))]
                (doseq [otro otros]
                  (transferir! id (:id otro) 50))
                {:tipo :arca-comunal
                 :mensaje "Pagas $50 a cada jugador"}))}])

;;Aplicar carta

(defn aplicar-carta-suerte! [id-jugador dados]
  (let [carta (rand-nth cartas-suerte)]
    (assoc ((:efecto carta) id-jugador dados)
           :carta (:descripcion carta))))

(defn aplicar-carta-arca-comunal! [id-jugador dados]
  (let [carta (rand-nth cartas-arca-comunal)]
    (assoc ((:efecto carta) id-jugador dados)
           :carta (:descripcion carta))))
