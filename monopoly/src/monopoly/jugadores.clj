(ns monopoly.jugadores)

(def estado-juego
  (atom {:turno 0
         :fase  :lobby
         :dobles-seguidos 0
         :subasta nil
         :jugadores []}))

;; ─── Crear jugadores ───────────────────────────────────────

(defn crear-jugador [id nombre ficha cliente-id]
  {:id               id
   :nombre           nombre
   :ficha            (Integer/parseInt ficha)
   :cliente-id       cliente-id  
   :listo?           false       
   :posicion         0
   :dinero           2500
   :propiedades      []
   :en-carcel        false
   :turnos-en-carcel 0
   :tiene-libre-carcel false
   :quebrado         false})

(defn agregar-jugador! [nombre]
  (let [id (count (:jugadores @estado-juego))]
    (swap! estado-juego update :jugadores conj (crear-jugador id nombre))
    {:exito true :id id :nombre nombre}))

;; ─── Movimiento ────────────────────────────────────────────

(defn tirar-dados []
  [(inc (rand-int 6)) (inc (rand-int 6))])

(defn mover-jugador! [id-jugador casillas]
  (let [pos-actual (get-in @estado-juego [:jugadores id-jugador :posicion])
        pos-nueva  (mod (+ pos-actual casillas) 40)
        ;; Si la nueva posición es menor a la actual, significa que dimos la vuelta al tablero
        paso-salida? (< pos-nueva pos-actual)]

    ;; 1. Actualizamos la posición en el tablero
    (swap! estado-juego assoc-in [:jugadores id-jugador :posicion] pos-nueva)

    ;; 2. ¡NUEVO! Si pasó por salida, le sumamos los $200 reales al instante
    (when paso-salida?
      (swap! estado-juego update-in [:jugadores id-jugador :dinero] + 200))

    {:pos-nueva pos-nueva :paso-salida? paso-salida?}))

;; ─── Turno ─────────────────────────────────────────────────

(defn jugador-actual []
  (let [id-actual (mod (:turno @estado-juego)
                       (count (:jugadores @estado-juego)))]
    (get-in @estado-juego [:jugadores id-actual])))

(defn id-jugador-actual []
  (mod (:turno @estado-juego)
       (count (:jugadores @estado-juego))))

(defn siguiente-turno! []
  (swap! estado-juego update :turno inc)
  {:exito true :turno (:turno @estado-juego)})

;; ─── Carcel ────────────────────────────────────────────────

(defn enviar-carcel! [id]
  (swap! estado-juego update-in [:jugadores id] assoc
         :posicion         10
         :en-carcel        true
         :turnos-en-carcel 0)
  {:exito true :mensaje "Jugador enviado a la carcel"})

(defn turno-en-carcel! [id]
  (swap! estado-juego update-in [:jugadores id :turnos-en-carcel] inc)
  {:exito true})

(defn liberar-carcel! [id]
  (swap! estado-juego update-in [:jugadores id] assoc
         :en-carcel        false
         :turnos-en-carcel 0)
  {:exito true :mensaje "Jugador liberado de la carcel"})

;; ─── Quiebra ───────────────────────────────────────────────

(defn quebrar-jugador! [id]
  (swap! estado-juego assoc-in [:jugadores id :quebrado] true)
  {:exito true :mensaje (str "Jugador " id " quebro")})

(defn jugadores-activos []
  (filter #(not (:quebrado %)) (:jugadores @estado-juego)))

(defn hay-ganador? []
  (<= (count (jugadores-activos)) 1))

(defn ganador []
  (first (jugadores-activos)))

;; ─── Info ──────────────────────────────────────────────────

(defn estado-jugador [id]
  (get-in @estado-juego [:jugadores id]))

(defn resumen-jugador [id]
  (let [j (get-in @estado-juego [:jugadores id])]
    {:id          (:id j)
     :nombre      (:nombre j)
     :ficha       (:ficha j)
     :cliente-id  (:cliente-id j)
     :listo       (:listo? j)     
     :posicion    (:posicion j)
     :dinero      (:dinero j)
     :en-carcel   (:en-carcel j)
     :quebrado    (:quebrado j)
     :propiedades (:propiedades j)}))

(defn resumen-todos []
  (mapv #(resumen-jugador (:id %)) (:jugadores @estado-juego)))