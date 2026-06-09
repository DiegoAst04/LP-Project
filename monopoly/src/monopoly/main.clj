(ns monopoly.main
  (:require [monopoly.server :as server]))

(defn -main [& args]
  (server/iniciar-servidor)
  (println "Servidor Monopoly iniciado en puerto 8080"))