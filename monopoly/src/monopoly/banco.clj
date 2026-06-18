(ns monopoly.banco
  (:require [monopoly.jugadores :refer [estado-juego quebrar-jugador!]]
            [monopoly.tablero :refer [tablero obtener-casilla]]))

;; Transferencias 

(defn cobrar-jugador! [id monto]
  (let [dinero-actual (get-in @estado-juego [:jugadores id :dinero])]
    (if (>= dinero-actual monto)
      (do
        (swap! estado-juego update-in [:jugadores id :dinero] #(- % monto))
        {:exito true})
      {:exito false :falta (- monto dinero-actual)})))

(defn pagar-jugador! [id monto]
  (swap! estado-juego update-in [:jugadores id :dinero] #(+ % monto))
  {:exito true})

(defn propiedades-hipotecables [id-jugador]
  (let [props-jugador (get-in @estado-juego [:jugadores id-jugador :propiedades])
        propiedades   (:propiedades @estado-juego)]
    (filterv
     (fn [id-casilla]
       (let [prop (get propiedades id-casilla)]
         (and (= (:dueno prop) id-jugador)
              (not (:hipotecada prop)))))
     props-jugador)))

(defn hipotecar-automaticamente! [id-jugador monto-necesario]
  (loop [props (propiedades-hipotecables id-jugador)
         total 0
         hipotecadas []]
    (if (or (empty? props)
            (>= total monto-necesario))
      {:total total
       :hipotecadas hipotecadas}

      (let [id-casilla (first props)
            casilla    (obtener-casilla id-casilla)
            valor      (int (/ (:precio casilla) 2))]
        (swap! estado-juego assoc-in
               [:propiedades id-casilla :hipotecada]
               true)

        (swap! estado-juego update-in
               [:jugadores id-jugador :dinero]
               + valor)

        (recur (rest props)
               (+ total valor)
               (conj hipotecadas (:nombre casilla)))))))

(defn cobrar-con-rescate! [id-jugador monto]
  (let [primer-intento (cobrar-jugador! id-jugador monto)]
    (if (:exito primer-intento)
      {:exito true
       :mensaje (str "Pago realizado por $" monto)}

      (let [falta (:falta primer-intento)
            auto-hipoteca (hipotecar-automaticamente! id-jugador falta)
            segundo-intento (cobrar-jugador! id-jugador monto)]
        (if (:exito segundo-intento)
          {:exito true
           :hipotecas-auto (:hipotecadas auto-hipoteca)
           :mensaje (str "Se hipotecaron propiedades automaticamente para pagar $" monto)}

          (do
            (quebrar-jugador! id-jugador)
            {:exito false
             :quiebra true
             :hipotecas-auto (:hipotecadas auto-hipoteca)
             :mensaje "Jugador no tiene fondos suficientes ni propiedades para hipotecar"}))))))

(defn valor-hipoteca-propiedad [id-casilla]
  (let [casilla (first (filter #(= (:id %) id-casilla)
                               monopoly.tablero/tablero))]
    (int (/ (:precio casilla) 2))))

(defn transferir! [id-pagador id-cobrador monto]
  (let [resultado (cobrar-con-rescate! id-pagador monto)]
    (if (:exito resultado)
      (do
        (pagar-jugador! id-cobrador monto)
        resultado)
      resultado)))
;;Propiedades

(defn agregar-propiedad-jugador! [id-jugador id-casilla]
  (swap! estado-juego update-in [:jugadores id-jugador :propiedades]
         conj id-casilla))

(defn comprar-propiedad! [id-jugador casilla]
  (let [precio    (:precio casilla)
        resultado (cobrar-jugador! id-jugador precio)]
    (if (:exito resultado)
      (do
        (agregar-propiedad-jugador! id-jugador (:id casilla))
        (swap! estado-juego assoc-in [:propiedades (:id casilla)]
               {:dueno id-jugador :hipotecada false :casas 0 :hotel false})
        {:exito true
         :mensaje (str "Propiedad comprada: " (:nombre casilla))
         :casilla (:nombre casilla)})
      {:exito false
       :mensaje "No tienes dinero suficiente para comprar"})))

;;Hipotecas

(defn hipotecar! [id-jugador id-casilla casilla]
  (let [valor (int (/ (:precio casilla) 2))
        prop  (get-in @estado-juego [:propiedades id-casilla])]
    (if (and (= (:dueno prop) id-jugador)
             (not (:hipotecada prop)))
      (do
        (swap! estado-juego assoc-in [:propiedades id-casilla :hipotecada] true)
        (pagar-jugador! id-jugador valor)
        {:exito true
         :mensaje (str "Hipotecado por $" valor)
         :monto valor})
      {:exito false
       :mensaje "No puedes hipotecar esta propiedad"})))

(defn levantar-hipoteca! [id-jugador id-casilla casilla]
  (let [costo (int (* (/ (:precio casilla) 2) 1.1))
        prop  (get-in @estado-juego [:propiedades id-casilla])]
    (if (and (= (:dueno prop) id-jugador)
             (:hipotecada prop))
      (let [resultado (cobrar-jugador! id-jugador costo)]
        (if (:exito resultado)
          (do
            (swap! estado-juego assoc-in [:propiedades id-casilla :hipotecada] false)
            {:exito true
             :mensaje (str "Hipoteca levantada, pagaste $" costo)
             :monto costo})
          {:exito false
           :mensaje "No tienes dinero para levantar la hipoteca"}))
      {:exito false
       :mensaje "Esta propiedad no esta hipotecada"})))

;;Verificar monopolio de color 

(defn casillas-de-color [color tablero]
  (filter #(= (:color %) color) tablero))

(defn tiene-monopolio? [id-jugador color tablero]
  (let [casillas    (casillas-de-color color tablero)
        propiedades (:propiedades @estado-juego)]
    (every? (fn [casilla]
              (let [prop (get propiedades (:id casilla))]
                (and (not (nil? prop))
                     (= (:dueno prop) id-jugador)
                     (not (:hipotecada prop)))))
            casillas)))

(defn monopolios-del-jugador [id-jugador tablero]
  (let [colores [:morado :celeste :rosado :naranja
                 :rojo :amarillo :verde :azul]]
    (filter #(tiene-monopolio? id-jugador % tablero) colores)))

;;Casas y Hoteles

(defn construir-casa! [id-jugador id-casilla casilla tablero]
  (let [prop       (get-in @estado-juego [:propiedades id-casilla])
        casas      (or (:casas prop) 0)
        costo      (int (/ (:precio casilla) 2))
        color      (:color casilla)
        tiene-mono (tiene-monopolio? id-jugador color tablero)

        
        casillas-color (casillas-de-color color tablero)
        min-casas      (apply min (map #(or (:casas (get-in @estado-juego [:propiedades (:id %)])) 0)
                                       casillas-color))]
    (cond
      (not= (:dueno prop) id-jugador)
      {:exito false :mensaje "No es tu propiedad"}

      (not tiene-mono)
      {:exito false :mensaje (str "Necesitas todas las propiedades del color " (name color) " para construir")}

      (:hotel prop)
      {:exito false :mensaje "Ya tiene hotel"}

      (= casas 3)
      {:exito false :mensaje "Ya tienes 3 casas, construye hotel"}

      
      (> casas min-casas)
      {:exito false :mensaje "Debes construir uniformemente. Pon casas en tus otros terrenos primero."}

      :else
      (let [resultado (cobrar-jugador! id-jugador costo)]
        (if (:exito resultado)
          (do
            (swap! estado-juego update-in [:propiedades id-casilla :casas] inc)
            {:exito true :mensaje (str "Casa construida en " (:nombre casilla)) :casilla (:nombre casilla)})
          {:exito false :mensaje "No tienes dinero para construir"})))))

(defn construir-hotel! [id-jugador id-casilla casilla tablero]
  (let [prop       (get-in @estado-juego [:propiedades id-casilla])
        costo      (:precio casilla)
        color      (:color casilla)
        tiene-mono (tiene-monopolio? id-jugador color tablero)]
    (cond
      (not= (:dueno prop) id-jugador)
      {:exito false :mensaje "No es tu propiedad"}

      (not tiene-mono)
      {:exito false
       :mensaje (str "Necesitas todas las propiedades del color "
                     (name color) " para construir")}

      (:hotel prop)
      {:exito false :mensaje "Ya tiene hotel"}

      (< (:casas prop) 3)
      {:exito false :mensaje "Necesitas 3 casas primero"}

      :else
      (let [resultado (cobrar-jugador! id-jugador costo)]
        (if (:exito resultado)
          (do
            (swap! estado-juego update-in [:propiedades id-casilla] assoc
                   :hotel true :casas 0)
            {:exito true
             :mensaje (str "Hotel construido en " (:nombre casilla))
             :casilla (:nombre casilla)})
          {:exito false :mensaje "No tienes dinero para construir hotel"})))))

;;Renta

(defn calcular-renta [casilla prop dados tablero]
  (let [dueno (:dueno prop)
        tipo  (:tipo casilla)]

    (cond
      ;;Regla 1: Estaciones
      (= tipo :estacion)
      (let [estaciones-dueno
            (count (filter (fn [c]
                             (and (= (:tipo c) :estacion) 
                                  (= (:dueno (get-in @estado-juego [:propiedades (:id c)])) dueno))) ;; Condición 2: Es mía
                           tablero))]
        (case estaciones-dueno
          1 25
          2 50
          3 100
          4 200
          25)) 

      ;;Regla 2: Servicios
      (= tipo :servicio)
      (let [servicios-dueno
            (count (filter (fn [c]
                             (and (= (:tipo c) :servicio) ;; Condición 1: Es un servicio
                                  (= (:dueno (get-in @estado-juego [:propiedades (:id c)])) dueno))) ;; Condición 2: Es mío
                           tablero))]
        (if (= servicios-dueno 2)
          (* 10 dados)
          (* 4 dados)))

      ;;Regla 3: Terrenos normales 
      :else
      (let [color      (:color casilla)
            tiene-mono (if color (tiene-monopolio? dueno color tablero) false)]
        (cond
          (:hotel prop)        (:renta-hotel casilla)
          (= (:casas prop) 3)  (* (:renta casilla) 15)
          (= (:casas prop) 2)  (* (:renta casilla) 10)
          (= (:casas prop) 1)  (* (:renta casilla) 5)
          tiene-mono           (* (:renta casilla) 2)
          :else                (:renta casilla))))))

(defn cobrar-renta! [id-jugador casilla dados tablero]
  (let [prop (get-in @estado-juego [:propiedades (:id casilla)])]
    (cond
      (nil? prop)
      {:exito false :mensaje "Casilla sin datos"}

      (nil? (:dueno prop))
      {:exito false :mensaje "Propiedad sin dueno"}

      (= (:dueno prop) id-jugador)
      {:exito true :mensaje "Es tu propiedad, no pagas renta"}

      (:hipotecada prop)
      {:exito true :mensaje "Propiedad hipotecada, no se cobra renta"}

      :else
      (let [renta     (calcular-renta casilla prop dados tablero)
            id-dueno  (:dueno prop)
            resultado (transferir! id-jugador id-dueno renta)]
        (assoc resultado
               :mensaje (str "Pagas renta de $" renta)
               :renta renta
               :id-dueno id-dueno)))))

;;Subasta 

(defn procesar-puja [puja-actual puja dinero-jugador]
  (cond
    (= puja 0)
    {:accion :retiro}

    (<= puja puja-actual)
    {:accion :invalida
     :mensaje (str "Debes pujar mas de $" puja-actual)}

    (> puja dinero-jugador)
    {:accion :invalida
     :mensaje "No tienes suficiente dinero"}

    :else
    {:accion :valida :puja puja}))

(defn aplicar-ganador-subasta! [ganador id-casilla puja-final]
  (cobrar-jugador! (:id ganador) puja-final)
  (agregar-propiedad-jugador! (:id ganador) id-casilla)
  (swap! estado-juego assoc-in [:propiedades id-casilla]
         {:dueno (:id ganador) :hipotecada false :casas 0 :hotel false})
  {:exito true
   :mensaje (str (:nombre ganador) " gano la subasta con $" puja-final)
   :ganador (:nombre ganador)
   :puja    puja-final})

;;Reparto inicial (monopoly rapido)

(defn repartir-propiedades-iniciales! [tablero]
  (let [propiedades (filter #(#{:propiedad :estacion :servicio} (:tipo %)) tablero)
        mezcladas   (shuffle propiedades)
        jugadores   (:jugadores @estado-juego)
        n-jugadores (count jugadores)]
    (doseq [[idx casilla] (map-indexed vector (take (* n-jugadores 2) mezcladas))]
      (let [id-jugador (mod idx n-jugadores)]
        (swap! estado-juego assoc-in [:propiedades (:id casilla)]
               {:dueno id-jugador :hipotecada false :casas 0 :hotel false})
        (agregar-propiedad-jugador! id-jugador (:id casilla))
        (cobrar-jugador! id-jugador (:precio casilla))))))
