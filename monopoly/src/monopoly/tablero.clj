(ns monopoly.tablero)

(def tablero
  [{:id 0  :nombre "Salida"              :tipo :salida}
   {:id 1  :nombre "Mediterraneo"        :tipo :propiedad :color :morado   :precio 60   :renta 2    :renta-hotel 250}
   {:id 2  :nombre "Arca Comunal"        :tipo :arca-comunal}
   {:id 3  :nombre "Mar Baltico"         :tipo :propiedad :color :morado   :precio 60   :renta 4    :renta-hotel 450}
   {:id 4  :nombre "Impuesto Renta"      :tipo :impuesto  :monto 200}
   {:id 5  :nombre "Estacion Kings"      :tipo :estacion  :precio 200}
   {:id 6  :nombre "Oriental Avenue"     :tipo :propiedad :color :celeste  :precio 100  :renta 6    :renta-hotel 550}
   {:id 7  :nombre "Suerte"              :tipo :suerte}
   {:id 8  :nombre "Vermont Avenue"      :tipo :propiedad :color :celeste  :precio 100  :renta 6    :renta-hotel 550}
   {:id 9  :nombre "Connecticut Avenue"  :tipo :propiedad :color :celeste  :precio 120  :renta 8    :renta-hotel 600}
   {:id 10 :nombre "Carcel/Visita"       :tipo :carcel-visita}
   {:id 11 :nombre "St. Charles Place"   :tipo :propiedad :color :rosado   :precio 140  :renta 10   :renta-hotel 750}
   {:id 12 :nombre "Compania Electrica"  :tipo :servicio  :precio 150}
   {:id 13 :nombre "States Avenue"       :tipo :propiedad :color :rosado   :precio 140  :renta 10   :renta-hotel 750}
   {:id 14 :nombre "Virginia Avenue"     :tipo :propiedad :color :rosado   :precio 160  :renta 12   :renta-hotel 900}
   {:id 15 :nombre "Estacion Penn"       :tipo :estacion  :precio 200}
   {:id 16 :nombre "St. James Place"     :tipo :propiedad :color :naranja  :precio 180  :renta 14   :renta-hotel 950}
   {:id 17 :nombre "Arca Comunal"        :tipo :arca-comunal}
   {:id 18 :nombre "Tennessee Avenue"    :tipo :propiedad :color :naranja  :precio 180  :renta 14   :renta-hotel 950}
   {:id 19 :nombre "New York Avenue"     :tipo :propiedad :color :naranja  :precio 200  :renta 16   :renta-hotel 1000}
   {:id 20 :nombre "Parque Gratis"       :tipo :esquina}
   {:id 21 :nombre "Kentucky Avenue"     :tipo :propiedad :color :rojo     :precio 220  :renta 18   :renta-hotel 1050}
   {:id 22 :nombre "Suerte"              :tipo :suerte}
   {:id 23 :nombre "Indiana Avenue"      :tipo :propiedad :color :rojo     :precio 220  :renta 18   :renta-hotel 1050}
   {:id 24 :nombre "Illinois Avenue"     :tipo :propiedad :color :rojo     :precio 240  :renta 20   :renta-hotel 1100}
   {:id 25 :nombre "Estacion Santa Fe"   :tipo :estacion  :precio 200}
   {:id 26 :nombre "Atlantic Avenue"     :tipo :propiedad :color :amarillo :precio 260  :renta 22   :renta-hotel 1150}
   {:id 27 :nombre "Ventnor Avenue"      :tipo :propiedad :color :amarillo :precio 260  :renta 22   :renta-hotel 1150}
   {:id 28 :nombre "Compania Aguas"      :tipo :servicio  :precio 150}
   {:id 29 :nombre "Marvin Gardens"      :tipo :propiedad :color :amarillo :precio 280  :renta 24   :renta-hotel 1200}
   {:id 30 :nombre "Ir a la Carcel"      :tipo :ir-carcel}
   {:id 31 :nombre "Pacific Avenue"      :tipo :propiedad :color :verde    :precio 300  :renta 26   :renta-hotel 1275}
   {:id 32 :nombre "North Carolina"      :tipo :propiedad :color :verde    :precio 300  :renta 26   :renta-hotel 1275}
   {:id 33 :nombre "Arca Comunal"        :tipo :arca-comunal}
   {:id 34 :nombre "Pennsylvania Avenue" :tipo :propiedad :color :verde    :precio 320  :renta 28   :renta-hotel 1400}
   {:id 35 :nombre "Estacion Marylebone" :tipo :estacion  :precio 200}
   {:id 36 :nombre "Suerte"              :tipo :suerte}
   {:id 37 :nombre "Park Place"          :tipo :propiedad :color :azul     :precio 350  :renta 35   :renta-hotel 1500}
   {:id 38 :nombre "Impuesto Lujo"       :tipo :impuesto  :monto 100}
   {:id 39 :nombre "Boardwalk"           :tipo :propiedad :color :azul     :precio 400  :renta 50   :renta-hotel 2000}])

(defn obtener-casilla [id]
  (get tablero id))

(defn casillas-por-tipo [tipo]
  (filter #(= (:tipo %) tipo) tablero))

(defn casillas-por-color [color]
  (filter #(= (:color %) color) tablero))