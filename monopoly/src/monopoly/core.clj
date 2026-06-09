(ns monopoly.core
  (:require [monopoly.tablero  :refer [tablero obtener-casilla]]
            [monopoly.jugadores :refer [estado-juego
                                        agregar-jugador!
                                        mover-jugador!
                                        tirar-dados
                                        jugador-actual
                                        id-jugador-actual
                                        siguiente-turno!
                                        hay-ganador?
                                        ganador
                                        jugadores-activos
                                        resumen-todos
                                        enviar-carcel!]]
            [monopoly.banco    :refer [repartir-propiedades-iniciales!
                                       construir-casa!
                                       construir-hotel!
                                       hipotecar!
                                       levantar-hipoteca!
                                       comprar-propiedad!
                                       aplicar-ganador-subasta!
                                       procesar-puja]]
            [monopoly.casillas :refer [resolver-casilla!
                                       manejar-turno-en-carcel!]]))

;; ─── Inicializar ───────────────────────────────────────────

(defn inicializar-estado! []
(swap! estado-juego assoc
       :propiedades {}
       :turno 0
       :fase :lobby
       :dobles-seguidos 0
       :jugadores [])
  {:exito true :mensaje "Estado inicializado"})

(defn registrar-jugador! [nombre]
  (agregar-jugador! nombre))

(defn iniciar-juego! []
  (if (< (count (:jugadores @estado-juego)) 2)
    {:exito false :mensaje "Se necesitan al menos 2 jugadores"}
    (do
      (repartir-propiedades-iniciales! tablero)
      (swap! estado-juego assoc :fase :jugando)
      {:exito  true
       :mensaje "Juego iniciado"
       :estado  @estado-juego})))

;; ─── Fortuna ───────────────────────────────────────────────

(defn calcular-fortuna [id-jugador]
  (let [jugador     (get-in @estado-juego [:jugadores id-jugador])
        dinero      (:dinero jugador)
        props-ids   (:propiedades jugador)
        propiedades (:propiedades @estado-juego)]
    (+ dinero
       (reduce (fn [total id-casilla]
                 (let [prop    (get propiedades id-casilla)
                       casilla (obtener-casilla id-casilla)]
                   (if (or (nil? prop) (nil? casilla))
                     total
                     (let [valor-base  (if (:hipotecada prop)
                                         (int (/ (:precio casilla) 2))
                                         (:precio casilla))
                           valor-casas (* (or (:casas prop) 0)
                                          (int (/ (:precio casilla) 2)))
                           valor-hotel (if (:hotel prop) (:precio casilla) 0)]
                       (+ total valor-base valor-casas valor-hotel)))))
               0
               props-ids))))

(defn resultado-final []
  (let [activos (jugadores-activos)]
    (if (= 1 (count activos))
      {:ganador (:nombre (first activos))
       :fortunas []}
      (let [fortunas (mapv (fn [j]
                             {:nombre  (:nombre j)
                              :fortuna (calcular-fortuna (:id j))})
                           activos)
            ganador-f (apply max-key :fortuna fortunas)]
        {:ganador  (:nombre ganador-f)
         :fortunas fortunas}))))

(defn juego-terminado? []
  (let [total-jugadores (count (:jugadores @estado-juego))
        quebrados       (count (filter :quebrado
                                       (:jugadores @estado-juego)))]
    (cond
      (= total-jugadores 2)
      (>= quebrados 1)

      (= total-jugadores 3)
      (>= quebrados 2)

      (= total-jugadores 4)
      (>= quebrados 2)

      :else false)))

;; ─── Acciones del turno ────────────────────────────────────

(defn tirar-turno! []
  (let [jugadores (:jugadores @estado-juego)]
    (if (empty? jugadores)
      {:exito false
       :mensaje "No hay jugadores registrados"}

      (let [id      (id-jugador-actual)
            jugador (jugador-actual)]
        (cond
          (:quebrado jugador)
          (do
            (siguiente-turno!)
            {:exito true
             :eventos [{:tipo :quiebra
                        :mensaje (str (:nombre jugador) " esta quebrado, se salta su turno")}]
             :estado @estado-juego})

          (:en-carcel jugador)
          (let [dados (tirar-dados)
                suma  (apply + dados)
                resultado-carcel (manejar-turno-en-carcel! id dados)]
            (if (:salio resultado-carcel)
              (let [resultado   (mover-jugador! id suma)
                    pos-nueva   (:pos-nueva resultado)
                    casilla     (obtener-casilla pos-nueva)
                    res-casilla (resolver-casilla! id casilla dados tablero)]
                (swap! estado-juego assoc :dobles-seguidos 0)
                (siguiente-turno!)
                {:exito true
                 :eventos (filterv some?
                                   [{:tipo :dados
                                     :dados dados
                                     :suma suma}
                                    {:tipo :carcel
                                     :mensaje (:mensaje resultado-carcel)}
                                    {:tipo :movimiento
                                     :posicion pos-nueva
                                     :casilla (:nombre casilla)}
                                    res-casilla])
                 :estado @estado-juego})

              (do
                (swap! estado-juego assoc :dobles-seguidos 0)
                (siguiente-turno!)
                {:exito true
                 :eventos [{:tipo :dados
                            :dados dados
                            :suma suma}
                           {:tipo :carcel
                            :mensaje (:mensaje resultado-carcel)}]
                 :estado @estado-juego})))

          :else
          (let [dados       (tirar-dados)
                suma        (apply + dados)
                dobles?     (= (first dados) (second dados))
                dobles-ant  (or (:dobles-seguidos @estado-juego) 0)
                dobles-new  (if dobles? (inc dobles-ant) 0)]

            (if (and dobles? (= dobles-new 3))
              (do
                (enviar-carcel! id)
                (swap! estado-juego assoc :dobles-seguidos 0)
                (siguiente-turno!)
                {:exito true
                 :eventos [{:tipo :dados
                            :dados dados
                            :suma suma}
                           {:tipo :carcel
                            :mensaje "Tres dobles seguidos, vas a la carcel"}]
                 :estado @estado-juego})

              (let [resultado   (mover-jugador! id suma)
                    pos-nueva   (:pos-nueva resultado)
                    casilla     (obtener-casilla pos-nueva)
                    res-casilla (resolver-casilla! id casilla dados tablero)
                    eventos     (filterv some?
                                         [{:tipo :dados
                                           :dados dados
                                           :suma suma}
                                          (when (:paso-salida? resultado)
                                            {:tipo :salida
                                             :mensaje "Pasaste por Salida, cobras $200"})
                                          {:tipo :movimiento
                                           :posicion pos-nueva
                                           :casilla (:nombre casilla)}
                                          res-casilla
                                          (when dobles?
                                            {:tipo :dobles
                                             :mensaje "Dobles! Puedes volver a tirar"})])]

                (swap! estado-juego assoc :dobles-seguidos dobles-new)

                (when (not dobles?)
                  (siguiente-turno!))

                {:exito true
                 :eventos eventos
                 :dobles? dobles?
                 :estado @estado-juego}))))))))

(defn accion-comprar! [id-jugador id-casilla]
  (let [casilla (obtener-casilla id-casilla)
        resultado (comprar-propiedad! id-jugador casilla)]
    (assoc resultado :estado @estado-juego)))

(defn accion-no-comprar! [id-casilla]

  (let [jugadores (mapv :id (jugadores-activos))]
    (swap! estado-juego assoc :subasta
           {:activa true
            :casilla-id id-casilla
            :puja-actual 0
            :ganador-actual nil
            :jugadores-activos jugadores
            :turno-subasta 0})
    {:exito true
     :tipo :subasta-iniciada
     :mensaje "Subasta iniciada"
     :subasta (:subasta @estado-juego)
     :estado @estado-juego}))

(defn avanzar-turno-subasta! []
  (swap! estado-juego update-in [:subasta :turno-subasta] inc))

(defn finalizar-subasta-si-corresponde! []
  (let [subasta (:subasta @estado-juego)
        activos (:jugadores-activos subasta)]
    (when (= 1 (count activos))
      (let [ganador-id (first activos)
            ganador (get-in @estado-juego [:jugadores ganador-id])
            casilla-id (:casilla-id subasta)
            puja-final (:puja-actual subasta)
            resultado (aplicar-ganador-subasta! ganador casilla-id puja-final)]
        (swap! estado-juego assoc :subasta nil)
        resultado))))

(defn jugador-subasta-actual []
  (let [subasta (:subasta @estado-juego)
        activos (:jugadores-activos subasta)
        turno (:turno-subasta subasta)]
    (when (seq activos)
      (nth activos (mod turno (count activos))))))

(defn accion-puja! [id-jugador puja]
  (let [subasta (:subasta @estado-juego)]
    (if (nil? subasta)
      {:exito false :mensaje "No hay subasta activa"}

      (let [jugador-turno (jugador-subasta-actual)]
        (if (not= id-jugador jugador-turno)
          {:exito false
           :mensaje "No es tu turno en la subasta"}

          (let [jugador (get-in @estado-juego [:jugadores id-jugador])
                resultado (procesar-puja (:puja-actual subasta)
                                         puja
                                         (:dinero jugador))]
            (case (:accion resultado)

              :retiro
              (do
                (swap! estado-juego update-in [:subasta :jugadores-activos]
                       #(vec (remove #{id-jugador} %)))
                (if-let [fin (finalizar-subasta-si-corresponde!)]
                  (assoc fin :estado @estado-juego)
                  (do
                    (avanzar-turno-subasta!)
                    {:exito true
                     :mensaje (str (:nombre jugador) " se retiro de la subasta")
                     :subasta (:subasta @estado-juego)
                     :estado @estado-juego})))

              :invalida
              (assoc resultado :exito false :estado @estado-juego)

              :valida
              (do
                (swap! estado-juego assoc-in [:subasta :puja-actual] puja)
                (swap! estado-juego assoc-in [:subasta :ganador-actual] id-jugador)
                (avanzar-turno-subasta!)
                {:exito true
                 :mensaje (str (:nombre jugador) " pujo $" puja)
                 :subasta (:subasta @estado-juego)
                 :estado @estado-juego}))))))))

(defn accion-ganar-subasta! [id-jugador id-casilla puja-final]
  (let [jugador (get-in @estado-juego [:jugadores id-jugador])
        resultado (aplicar-ganador-subasta! jugador id-casilla puja-final)]
    (assoc resultado :estado @estado-juego)))

(defn accion-construir-casa! [id-jugador id-casilla]
  (let [casilla (obtener-casilla id-casilla)
        resultado (construir-casa! id-jugador id-casilla casilla tablero)]
    (assoc resultado :estado @estado-juego)))

(defn accion-construir-hotel! [id-jugador id-casilla]
  (let [casilla (obtener-casilla id-casilla)
        resultado (construir-hotel! id-jugador id-casilla casilla tablero)]
    (assoc resultado :estado @estado-juego)))

(defn accion-hipotecar! [id-jugador id-casilla]
  (let [casilla (obtener-casilla id-casilla)
        resultado (hipotecar! id-jugador id-casilla casilla)]
    (assoc resultado :estado @estado-juego)))

(defn accion-levantar-hipoteca! [id-jugador id-casilla]
  (let [casilla (obtener-casilla id-casilla)
        resultado (levantar-hipoteca! id-jugador id-casilla casilla)]
    (assoc resultado :estado @estado-juego)))

;; ─── Estado del juego ──────────────────────────────────────

(defn estado-actual []
  (let [jugadores (:jugadores @estado-juego)
        terminado (juego-terminado?)]
    {:estado    @estado-juego
     :turno-id  (if (empty? jugadores)
                  nil
                  (id-jugador-actual))
     :jugadores (resumen-todos)
     :tablero   tablero

     :ganador? terminado

     :resultado-final
     (when terminado
       (resultado-final))}))

;; ─── Main (solo para pruebas) ──────────────────────────────

(defn -main [& args]
  (println "Servidor Monopoly listo"))