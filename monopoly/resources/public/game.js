const btnTerminarTurno = document.getElementById("btnTerminarTurno");
let yaTireLosDados = false;

//Sistema de sesiones
let miClienteId = localStorage.getItem("monopoly_cliente_id");
if (!miClienteId) {

    miClienteId = Math.random().toString(36).substring(2, 15);
    localStorage.setItem("monopoly_cliente_id", miClienteId);
}
let miJugadorId = null; 

const SERVER_HOST = window.location.hostname; 
const API_BASE = `http://${SERVER_HOST}:8080`;
const WS_BASE = `ws://${SERVER_HOST}:8080/ws`;

const tablero = document.getElementById("tablero");

const turnoDiv = document.getElementById("turno");
const dineroDiv = document.getElementById("dinero");
const eventosDiv = document.getElementById("eventos");
const dadosDiv = document.getElementById("dados");
const compraPanel = document.getElementById("compra-panel");
const mensajeCompra = document.getElementById("mensaje-compra");
const btnComprar = document.getElementById("btnComprar");
const btnNoComprar = document.getElementById("btnNoComprar");
const subastaPanel = document.getElementById("subasta-panel");
const infoSubasta = document.getElementById("info-subasta");
const inputPuja = document.getElementById("inputPuja");
const btnPujar = document.getElementById("btnPujar");
const btnRetirarse = document.getElementById("btnRetirarse");
const construccionPanel = document.getElementById("construccion-panel");
const selectPropiedadConstruir = document.getElementById("selectPropiedadConstruir");
const btnConstruirCasa = document.getElementById("btnConstruirCasa");
const btnConstruirHotel = document.getElementById("btnConstruirHotel");
const btnHipotecar = document.getElementById("btnHipotecar");
const btnLevantarHipoteca = document.getElementById("btnLevantarHipoteca");
const listaJugadoresDiv = document.getElementById("lista-jugadores");

let subastaActual = null;

let hayDecisionPendiente = false;
let compraPendiente = null;
let jugadorCompraPendiente = null;

let estadoGlobal = null;
let tableroGlobal = [];

//Botones Lobby Inicio
document.getElementById("btnUnirse").addEventListener("click", async () => {
    const nombre = document.getElementById("inputNombre").value;
    const ficha = document.getElementById("selectFicha").value;
    if (nombre.trim() === "") return alert("Ingresa tu nombre");
    
    await fetch(`${API_BASE}/registrar/${nombre}/${ficha}/${miClienteId}`);
});

document.getElementById("btnListo").addEventListener("click", async () => {
    document.getElementById("btnListo").disabled = true;
    document.getElementById("btnListo").textContent = "Esperando a los demás...";
    await fetch(`${API_BASE}/listo/${miClienteId}`);
});
//Botones Lobby fin

async function iniciarFrontend() {
  
    const respuesta = await fetch(`${API_BASE}/estado`);
    const data = await respuesta.json();

    estadoGlobal = data;
    tableroGlobal = data.tablero;

    construirTablero(data.tablero);
    procesarActualizacionEstado(data); 
    
    
    conectarWebSocket();
}


function procesarActualizacionEstado(data) {
    estadoGlobal = data;
    tableroGlobal = data.tablero;

    const lobbyPantalla = document.getElementById("lobby-pantalla");
    const contenedor = document.getElementById("contenedor");

    
    if (data.estado.fase === "lobby") {
        lobbyPantalla.style.display = "flex";
        contenedor.style.display = "none";
        
        const ul = document.getElementById("ulJugadoresLobby");
        ul.innerHTML = "";
        let yoEstoyRegistrado = false;

        
        data.jugadores.forEach(j => {
            const li = document.createElement("li");
            li.innerHTML = `${j.nombre} ${j.listo ? "✅" : "⏳"}`;
            ul.appendChild(li);
            
            
            if (j["cliente-id"] === miClienteId) {
                yoEstoyRegistrado = true;
                miJugadorId = j.id;
            }
        });

      
        if (yoEstoyRegistrado) {
            document.getElementById("registro-jugador").style.display = "none";
            document.getElementById("lista-espera").style.display = "block";
        }
        return;
    }

   
    lobbyPantalla.style.display = "none";
    contenedor.style.display = "grid";

    // Actualización visual normal
    actualizarPanel(data);
    actualizarDuenos(data);
    actualizarMejoras(data);
    actualizarHipotecas(data);
    actualizarFichasDesdeEstado(data.jugadores);
    marcarJugadorActual(data);
    actualizarJugadoresQuebrados(data.jugadores);
    verificarGanador(data);
    actualizarPanelJugadores(data);

    //Bloqueo de controles
    const esMiTurno = (data["turno-id"] === miJugadorId);

    mostrarSubasta(data.estado.subasta);
    
    
    const bloqueadoPorEvento = hayDecisionPendiente || data.estado.subasta !== null;

 
    if (!esMiTurno) {
        
        document.getElementById("btnDado").style.display = "block";
        document.getElementById("btnDado").disabled = true;
        btnTerminarTurno.style.display = "none";
    } else {
        
        if (yaTireLosDados) {
            document.getElementById("btnDado").style.display = "none";
            btnTerminarTurno.style.display = "block";
            btnTerminarTurno.disabled = bloqueadoPorEvento; 
        } else {
            document.getElementById("btnDado").style.display = "block";
            document.getElementById("btnDado").disabled = bloqueadoPorEvento;
            btnTerminarTurno.style.display = "none";
        }
    }
    
    // Botones de construccion y gestion

    const enSubasta = data.estado.subasta !== null;
    document.getElementById("btnConstruirCasa").disabled = !esMiTurno || enSubasta;
    document.getElementById("btnConstruirHotel").disabled = !esMiTurno || enSubasta;
    document.getElementById("btnHipotecar").disabled = !esMiTurno || enSubasta;
    document.getElementById("btnLevantarHipoteca").disabled = !esMiTurno || enSubasta;
}


function conectarWebSocket() {
    const socket = new WebSocket(WS_BASE);

    socket.onopen = () => {
        console.log("Conectado exitosamente al flujo distribuido del Monopoly");
    };

    socket.onmessage = (event) => {
       
        const nuevoEstado = JSON.parse(event.data);
        console.log("Estado actualizado recibido desde el servidor central:", nuevoEstado);
        
      
        procesarActualizacionEstado(nuevoEstado);
    };

    socket.onclose = () => {
        console.warn("Conexión perdida con el servidor. Intentando reconectar en 3 segundos...");
        setTimeout(conectarWebSocket, 3000);
    };
}

function actualizarPanelJugadores(data) {
    const jugadores = data.jugadores;
    const propiedadesEstado = data.estado.propiedades || {};
    const colores = ["🔴", "🔵", "🟢", "🟡"];

    listaJugadoresDiv.innerHTML = "";

    jugadores.forEach(jugador => {
        let propiedades = 0;
        let estaciones = 0;
        let servicios = 0;
        let patrimonio = jugador.dinero;

        const propiedadesReales = Object.entries(propiedadesEstado)
            .filter(([_, prop]) => prop.dueno === jugador.id)
            .map(([id]) => parseInt(id));

        propiedadesReales.forEach(idPropiedad => {
            const casilla = tableroGlobal.find(c => c.id === idPropiedad);
            const propEstado = propiedadesEstado[idPropiedad];

            if (!casilla) return;

            if (casilla.tipo === "propiedad") propiedades++;
            if (casilla.tipo === "estacion") estaciones++;
            if (casilla.tipo === "servicio") servicios++;

            if (propEstado && propEstado.hipotecada) {
                patrimonio += Math.floor(casilla.precio / 2);
            } else {
                patrimonio += casilla.precio || 0;
            }

            if (propEstado) {
                patrimonio += (propEstado.casas || 0) * Math.floor((casilla.precio || 0) / 2);

                if (propEstado.hotel) {
                    patrimonio += casilla.precio || 0;
                }
            }
        });

        const div = document.createElement("div");
        div.classList.add("tarjeta-jugador");
        div.classList.add(`tarjeta-${jugador.id}`);

        if (jugador.quebrado) {
            div.classList.add("jugador-quebrado");
        }

        const totalCompradas = propiedades + estaciones + servicios;

        div.innerHTML =
            `<strong>${colores[jugador.id] || "⚪"} ${jugador.nombre} ${jugador.quebrado ? "💀" : ""}</strong><br>
            🏘️ Total: ${totalCompradas}<br>
            🏠 Terrenos: ${propiedades}<br>
            🚂 Estaciones: ${estaciones}<br>
            ⚡ Servicios: ${servicios}<br>
            💰 Patrimonio: $${patrimonio}`;

        listaJugadoresDiv.appendChild(div);
    });
}

function actualizarJugadoresQuebrados(jugadores) {
    jugadores.forEach(jugador => {
        const ficha = document.getElementById(`jugador-${jugador.id}`);

        if (!ficha) return;

        if (jugador.quebrado) {
            ficha.classList.add("ficha-quebrada");
            ficha.textContent = "💀";
        } else {
            ficha.classList.remove("ficha-quebrada");
        }
    });
}

function actualizarPanelConstruccion(jugador) {
    selectPropiedadConstruir.innerHTML = "";

    const opcionBase = document.createElement("option");
    opcionBase.value = "";
    opcionBase.textContent = "Selecciona propiedad";
    selectPropiedadConstruir.appendChild(opcionBase);

    jugador.propiedades.forEach(idPropiedad => {
        const casilla = tableroGlobal.find(c => c.id === idPropiedad);

        if (!casilla) return;

        if (
            casilla.tipo === "propiedad" ||
            casilla.tipo === "estacion" ||
            casilla.tipo === "servicio"
        ) {
            const option = document.createElement("option");
            option.value = idPropiedad;
            option.textContent = casilla.nombre;
            selectPropiedadConstruir.appendChild(option);
        }
    });
}

function actualizarFichasDesdeEstado(jugadores) {
    jugadores.forEach(jugador => {
        let ficha = document.getElementById(`jugador-${jugador.id}`);
        
        
        if (!ficha) {
            crearFicha(jugador.id, jugador.ficha);
        }
        
        moverJugador(jugador.id, jugador.posicion);
    });
}

function actualizarDuenos(data) {
    const propiedades = data.estado.propiedades || {};
    const simbolos = ["🔴", "🔵", "🟢", "🟡"];

    document.querySelectorAll(".dueno-casilla").forEach(div => {
        div.innerHTML = "";
        div.className = "dueno-casilla";
    });

    for (const idCasilla in propiedades) {
        const infoPropiedad = propiedades[idCasilla];
        const divDueno = document.getElementById(`dueno-${idCasilla}`);

        if (!divDueno) {
            console.warn("No existe divDueno para casilla", idCasilla);
            continue;
        }

        if (infoPropiedad.dueno !== undefined && infoPropiedad.dueno !== null) {
            if (infoPropiedad.hipotecada) {
                divDueno.innerHTML =
                    `Dueño ${simbolos[infoPropiedad.dueno]}<br>💰 H`;
            } else {
                divDueno.textContent =
                    `Dueño ${simbolos[infoPropiedad.dueno]}`;
            }

            divDueno.classList.add(`dueno-${infoPropiedad.dueno}`);
        }
    }
}

function actualizarMejoras(data) {

    const propiedades = data.estado.propiedades || {};

    for (const idCasilla in propiedades) {

        const propiedad = propiedades[idCasilla];

        const div =
            document.getElementById(`mejoras-${idCasilla}`);

        if (!div) continue;

        if (propiedad.hotel) {
            div.textContent = "🏨";
        }
        else if (propiedad.casas > 0) {
            div.textContent = "🏠".repeat(propiedad.casas);
        }
        else {
            div.textContent = "";
        }
    }
}

function actualizarHipotecas(data) {
    const propiedades = data.estado.propiedades || {};

    for (const idCasilla in propiedades) {
        const propiedad = propiedades[idCasilla];
        const casillaDiv = document.getElementById(`casilla-${idCasilla}`);

        if (!casillaDiv) continue;

        if (propiedad.hipotecada) {
            casillaDiv.classList.add("hipotecada");
        } else {
            casillaDiv.classList.remove("hipotecada");
        }
    }
}

function actualizarPanel(data) {
    const jugadores = data.jugadores;

    if (jugadores.length === 0) {
        turnoDiv.innerHTML = "<strong>TURNO ACTUAL</strong><br>Sin jugadores";
        dineroDiv.textContent = "Dinero: -";
        return;
    }

    //Mostrar de quién es el turno (público)
    const turnoId = data["turno-id"];
    const jugadorTurno = jugadores.find(j => j.id === turnoId) || jugadores[0];

    turnoDiv.innerHTML =
        `<strong>🎮 TURNO ACTUAL</strong><br>
        👉 ${jugadorTurno.nombre}`;

    // 2. Mostrar mis datos personales (privado de cada pantalla)
    const miJugador = jugadores.find(j => j.id === miJugadorId);

    if (miJugador) {
        dineroDiv.innerHTML =
            `<strong>💵 MI DINERO</strong><br>
            $${miJugador.dinero}`;

      
        actualizarPanelConstruccion(miJugador);
    } else {
        
        dineroDiv.innerHTML = `<strong>💵 DINERO</strong><br>-`;
        selectPropiedadConstruir.innerHTML = '<option value="">Selecciona propiedad</option>';
    }
}

function obtenerNombreCasilla(idCasilla) {
    const casilla = tableroGlobal.find(c => c.id === idCasilla);
    return casilla ? casilla.nombre : `Casilla ${idCasilla}`;
}

function construirTablero(datos) {
    tablero.innerHTML = "";

    let indice = 0;

    for (let c = 11; c >= 1; c--) {
        const d = datos[indice];
        crearCasilla(d.nombre, 11, c, d.color, d.id);
        indice++;
    }

    for (let f = 10; f >= 2; f--) {
        const d = datos[indice];
        crearCasilla(d.nombre, f, 1, d.color, d.id);
        indice++;
    }

    for (let c = 1; c <= 11; c++) {
        const d = datos[indice];
        crearCasilla(d.nombre, 1, c, d.color, d.id);
        indice++;
    }

    for (let f = 2; f <= 10; f++) {
        const d = datos[indice];
        crearCasilla(d.nombre, f, 11, d.color, d.id);
        indice++;
    }

    const centro = document.createElement("div");
    centro.classList.add("centro");
    centro.textContent = "MONOPOLY";
    tablero.appendChild(centro);
}

function crearCasilla(nombre, fila, columna, color, id) {
    const casilla = document.createElement("div");

    casilla.classList.add("casilla");
    casilla.id = `casilla-${id}`;

    casilla.style.gridRow = fila;
    casilla.style.gridColumn = columna;

    if (color) {
        const barra = document.createElement("div");
        barra.classList.add("barra-color");
        barra.classList.add(color);
        casilla.appendChild(barra);
    }

    const nombreDiv = document.createElement("div");
    nombreDiv.classList.add("nombre-casilla");
    nombreDiv.textContent = nombre;
    casilla.appendChild(nombreDiv);

    const dueno = document.createElement("div");
    dueno.classList.add("dueno-casilla");
    dueno.id = `dueno-${id}`;
    casilla.appendChild(dueno);

    const mejoras = document.createElement("div");
    mejoras.classList.add("mejoras");
    mejoras.id = `mejoras-${id}`;
    casilla.appendChild(mejoras);

    const fichas = document.createElement("div");
    fichas.classList.add("fichas");
    casilla.appendChild(fichas);

    tablero.appendChild(casilla);
}

function crearFicha(id, indiceFichaElegida) {
    const simbolos = ["🚗", "🎩", "🐶", "🚢"];
    const ficha = document.createElement("div");

    ficha.id = `jugador-${id}`;
    ficha.classList.add("ficha-jugador");
    ficha.classList.add(`ficha-${id}`); 


    ficha.textContent = simbolos[indiceFichaElegida] || "●";

    document.querySelector("#casilla-0 .fichas").appendChild(ficha);
}

function marcarJugadorActual(data) {
    document.querySelectorAll(".ficha-jugador").forEach(ficha => {
        ficha.classList.remove("ficha-activa");
    });

    const turnoId = data["turno-id"];
    const fichaActual = document.getElementById(`jugador-${turnoId}`);

    if (fichaActual) {
        fichaActual.classList.add("ficha-activa");
    }
}

function moverJugador(id, posicion) {
    const ficha = document.getElementById(`jugador-${id}`);

    if (!ficha) return;

    document
        .querySelector(`#casilla-${posicion} .fichas`)
        .appendChild(ficha);
}

function mostrarSubasta(subasta) {
    if (!subasta) {
        subastaPanel.style.display = "none";

        subastaActual = null;
        return;
    }

    subastaActual = subasta;
    subastaPanel.style.display = "block";

    const jugadores = estadoGlobal.jugadores;

    const jugadorTurnoId =
        subasta["jugadores-activos"][
            subasta["turno-subasta"] % subasta["jugadores-activos"].length
        ];

    const jugadorTurno =
        jugadores.find(j => j.id === jugadorTurnoId);

    const ganadorActual =
        jugadores.find(j => j.id === subasta["ganador-actual"]);

    infoSubasta.innerHTML =
        `Propiedad: ${obtenerNombreCasilla(subasta["casilla-id"])}<br>
         Puja actual: $${subasta["puja-actual"]}<br>
         Ganando: ${ganadorActual ? ganadorActual.nombre : "Nadie"}<br>
         Turno de puja: ${jugadorTurno ? jugadorTurno.nombre : "-"}`;


    const esMiTurnoSubasta = (jugadorTurno && jugadorTurno.id === miJugadorId);
    document.getElementById("btnPujar").disabled = !esMiTurnoSubasta;
    document.getElementById("btnRetirarse").disabled = !esMiTurnoSubasta;
}

function verificarGanador(data) {
    if (!data["ganador?"]) return;

    const resultado = data["resultado-final"];

    let mensaje =
        `<strong>🏆 FIN DE PARTIDA</strong><br>
         Ganador: ${resultado.ganador}`;

    if (resultado.fortunas && resultado.fortunas.length > 0) {
        mensaje +=
            `<br><br><strong>Fortunas finales:</strong><br>` +
            resultado.fortunas
                .map(f => `${f.nombre}: $${f.fortuna}`)
                .join("<br>");
    }

    eventosDiv.innerHTML = mensaje;

    btnDado.disabled = true;
    btnConstruirCasa.disabled = true;
    btnConstruirHotel.disabled = true;
    btnHipotecar.disabled = true;
    btnLevantarHipoteca.disabled = true;
    btnComprar.disabled = true;
    btnNoComprar.disabled = true;
    btnPujar.disabled = true;
    btnRetirarse.disabled = true;
}

document
    .getElementById("btnDado")
    
    .addEventListener("click", async () => {

        if (hayDecisionPendiente) {
            eventosDiv.textContent = "Primero debes comprar o enviar la propiedad a subasta";
            return;
        }
        const jugadorAntesDeTirar =
            estadoGlobal.jugadores.find(j => j.id === estadoGlobal["turno-id"]);

        const respuesta = await fetch(`${API_BASE}/tirar-turno`);
        const data = await respuesta.json();

        if (!data.exito) {
            eventosDiv.textContent = data.mensaje;
            return;
        }

        const eventoDados =
            data.eventos.find(e => e.tipo === "dados");

            if (eventoDados) {

                dadosDiv.innerHTML =
                    `<strong>🎲 DADOS</strong><br>
                    🎲 ${eventoDados.dados[0]}
                    + 🎲 ${eventoDados.dados[1]}
                    = <strong>${eventoDados.suma}</strong>`;
                

                const sonDobles = eventoDados.dados[0] === eventoDados.dados[1];
                const estabaEnCarcel = jugadorAntesDeTirar["en-carcel"];

                if (sonDobles && !estabaEnCarcel) {
                    dadosDiv.innerHTML += "<br>🎉 ¡Dobles! Tiras de nuevo.";
                    yaTireLosDados = false; 

                } else if (sonDobles && estabaEnCarcel) {
                    dadosDiv.innerHTML += "<br>🎉 ¡Dobles! Sales de la cárcel (No tiras de nuevo).";
                    yaTireLosDados = true; 

                } else {
                    yaTireLosDados = true; 
                }


            }

        const eventoMovimiento =
            data.eventos.find(e => e.tipo === "movimiento");
        
        const eventoCompra = data.eventos.find(e => e.tipo === "compra-disponible");

            if (eventoCompra) {
            compraPendiente = eventoCompra.casilla;
            jugadorCompraPendiente = jugadorAntesDeTirar.id;
            hayDecisionPendiente = true;
            compraPanel.style.display = "block";
            mensajeCompra.textContent = eventoCompra.mensaje;
        } else {
            compraPanel.style.display = "none";
            compraPendiente = null;
            jugadorCompraPendiente = null;
        }

        const eventoDobles =
            data.eventos.find(e => e.tipo === "dobles");

        const eventoCarcel =
            data.eventos.find(e => e.tipo === "carcel");

        const eventoIrCarcel =
            data.eventos.find(e => e.tipo === "ir-carcel");

        const eventoCarcelVisita =
            data.eventos.find(e => e.tipo === "carcel-visita");

        const eventoSalida =
            data.eventos.find(e => e.tipo === "salida");    
        
        const eventoRenta =
            data.eventos.find(e => e.renta !== undefined);

        const eventoImpuesto =
            data.eventos.find(e => e.tipo === "impuesto");
        
        const eventoCarta =
            data.eventos.find(
                e => e.tipo === "suerte" || e.tipo === "arca-comunal"
            );
        const eventoQuiebra =
            data.eventos.find(e => e.tipo === "quiebra");

    let mensajes = [];

    if (eventoCarcel) {
        mensajes.push(
            `<strong>CÁRCEL</strong><br>${eventoCarcel.mensaje}`
        );
    }

    if (eventoIrCarcel) {
        mensajes.push(
            `<strong>IR A LA CÁRCEL</strong><br>${eventoIrCarcel.mensaje}`
        );
    }

    if (eventoCarcelVisita) {
        mensajes.push(
            `<strong>CÁRCEL / VISITA</strong><br>${eventoCarcelVisita.mensaje}`
        );
    }

    if (eventoSalida) {
        mensajes.push(
            `<strong>SALIDA</strong><br>${eventoSalida.mensaje}`
        );
    }

    if (eventoCarta) {
        mensajes.push(
            `<strong>CARTA RECIBIDA</strong><br>
            Tipo: ${eventoCarta.tipo === "suerte" ? "Suerte" : "Arca Comunal"}<br>
            Carta: ${eventoCarta.carta}<br>
            Efecto: ${eventoCarta.mensaje}`
        );
    }

    if (eventoImpuesto) {
        mensajes.push(
            `<strong>IMPUESTO</strong><br>${eventoImpuesto.mensaje}`
        );
    }

    if (eventoRenta) {
        const dueno = estadoGlobal.jugadores.find(
            j => j.id === eventoRenta["id-dueno"]
        );

        let mensajeRenta =
            `<strong>RENTA</strong><br>
            ${jugadorAntesDeTirar.nombre} debe pagar $${eventoRenta.renta}
            a ${dueno ? dueno.nombre : "otro jugador"}`;

        if (
            eventoRenta["hipotecas-auto"] &&
            eventoRenta["hipotecas-auto"].length > 0
        ) {
            mensajeRenta +=
                `<br><br><strong>Hipotecas automáticas:</strong><br>` +
                eventoRenta["hipotecas-auto"]
                    .map(nombre => `- ${nombre}`)
                    .join("<br>");
        }

        if (eventoRenta.quiebra) {
            mensajeRenta +=
                `<br><br><strong>💀 ${jugadorAntesDeTirar.nombre} quebró</strong>`;
        } else {
            mensajeRenta +=
                `<br><br>Pago realizado correctamente.`;
        }

        mensajes.push(mensajeRenta);
    }

    if (eventoDobles) {
        mensajes.push(
            `<strong>DOBLES</strong><br>${eventoDobles.mensaje}`
        );
    }
    if (eventoQuiebra) {
        mensajes.push(
            `<strong>QUIEBRA</strong><br>${eventoQuiebra.mensaje}`
        );
}

    if (mensajes.length > 0) {
        eventosDiv.innerHTML = mensajes.join("<hr>");
    } else if (eventoMovimiento) {
        eventosDiv.textContent = `Movimiento realizado: ${eventoMovimiento.casilla}`;
    } else {
        eventosDiv.textContent = "Turno procesado";
    }

        const estadoActualizado = await fetch(`${API_BASE}/estado`);
        const nuevoEstado = await estadoActualizado.json();

        procesarActualizacionEstado(nuevoEstado);
    });

btnComprar.addEventListener("click", async () => {
    if (!compraPendiente) return;

    const casillaGuardada = compraPendiente;
    const jugadorGuardado = jugadorCompraPendiente;

    hayDecisionPendiente = false;
    compraPanel.style.display = "none";

    const respuesta = await fetch(
        `${API_BASE}/comprar/${jugadorGuardado}/${casillaGuardada.id}`
    );

    const data = await respuesta.json();

    if (!data.exito) {

        eventosDiv.innerHTML = `<strong>⚠️ ${data.mensaje}</strong><br>Debes enviarla a subasta.`;
        
      
        compraPendiente = casillaGuardada;
        jugadorCompraPendiente = jugadorGuardado;
        hayDecisionPendiente = true;
        compraPanel.style.display = "block";
        
        return; 
    }

    eventosDiv.textContent = data.mensaje;

    compraPendiente = null;
    jugadorCompraPendiente = null;
    

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);

    const jugadorComprador = nuevoEstado.jugadores.find(
        j => j.id === jugadorCompraPendiente
    );
});

btnNoComprar.addEventListener("click", async () => {
    if (!compraPendiente) return;

    hayDecisionPendiente = false;
    compraPanel.style.display = "none";

    const respuesta = await fetch(
        `${API_BASE}/no-comprar/${compraPendiente.id}`
    );

    const data = await respuesta.json();

    eventosDiv.textContent = data.mensaje;

    compraPendiente = null;
    jugadorCompraPendiente = null;

    

    document.getElementById("btnDado").disabled = true;

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    mostrarSubasta(data.subasta);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);
});

btnPujar.addEventListener("click", async () => {
    if (!subastaActual) return;

    const jugadoresActivos = subastaActual["jugadores-activos"];

    const jugadorTurnoId =
        jugadoresActivos[
            subastaActual["turno-subasta"] % jugadoresActivos.length
        ];

    const puja = parseInt(inputPuja.value);

    if (isNaN(puja)) {
        eventosDiv.textContent = "Ingresa una puja válida";
        return;
    }

    const respuesta = await fetch(
        `${API_BASE}/pujar/${jugadorTurnoId}/${puja}`
    );

    const data = await respuesta.json();

    eventosDiv.textContent = data.mensaje;

    inputPuja.value = "";

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);

  if (!data.exito) {
    eventosDiv.textContent = data.mensaje;

    if (subastaActual) {
        mostrarSubasta(subastaActual);
    }

    inputPuja.value = "";
    return;
    }

    if (data.subasta) {
        mostrarSubasta(data.subasta);
    } else {
    mostrarSubasta(null);
    hayDecisionPendiente = false;
    }
});

btnRetirarse.addEventListener("click", async () => {
    if (!subastaActual) return;

    const jugadoresActivos = subastaActual["jugadores-activos"];

    const jugadorTurnoId =
        jugadoresActivos[
            subastaActual["turno-subasta"] % jugadoresActivos.length
        ];

    const respuesta = await fetch(
        `${API_BASE}/pujar/${jugadorTurnoId}/0`
    );

    const data = await respuesta.json();

    eventosDiv.textContent = data.mensaje;

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);

    if (data.subasta) {
        mostrarSubasta(data.subasta);
    } else {
        mostrarSubasta(null);
        hayDecisionPendiente = false;
    }
});

btnConstruirCasa.addEventListener("click", async () => {
    const idCasilla = selectPropiedadConstruir.value;

    if (idCasilla === "") {
        eventosDiv.textContent = "Selecciona una propiedad";
        return;
    }

    const jugadorId = estadoGlobal["turno-id"];

    const respuesta = await fetch(
        `${API_BASE}/construir-casa/${jugadorId}/${idCasilla}`
    );

    const data = await respuesta.json();

    eventosDiv.textContent = data.mensaje;

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);
});

btnConstruirHotel.addEventListener("click", async () => {
    const idCasilla = selectPropiedadConstruir.value;

    if (idCasilla === "") {
        eventosDiv.textContent = "Selecciona una propiedad";
        return;
    }

    const jugadorId = estadoGlobal["turno-id"];

    const respuesta = await fetch(
        `${API_BASE}/construir-hotel/${jugadorId}/${idCasilla}`
    );

    const data = await respuesta.json();

    eventosDiv.textContent = data.mensaje;

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);
});

btnHipotecar.addEventListener("click", async () => {
    const idCasilla = selectPropiedadConstruir.value;

    if (idCasilla === "") {
        eventosDiv.textContent = "Selecciona una propiedad";
        return;
    }

    const jugadorId = estadoGlobal["turno-id"];

    const respuesta = await fetch(
        `${API_BASE}/hipotecar/${jugadorId}/${idCasilla}`
    );

    const data = await respuesta.json();

    eventosDiv.textContent = data.mensaje;

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);
});

btnLevantarHipoteca.addEventListener("click", async () => {
    const idCasilla = selectPropiedadConstruir.value;

    if (idCasilla === "") {
        eventosDiv.textContent = "Selecciona una propiedad";
        return;
    }

    const jugadorId = estadoGlobal["turno-id"];

    const respuesta = await fetch(
        `${API_BASE}/levantar-hipoteca/${jugadorId}/${idCasilla}`
    );

    const data = await respuesta.json();

    eventosDiv.textContent = data.mensaje;

    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();

    estadoGlobal = nuevoEstado;
    tableroGlobal = nuevoEstado.tablero;

    actualizarPanel(nuevoEstado);
    actualizarDuenos(nuevoEstado);
    actualizarMejoras(nuevoEstado);
    actualizarHipotecas(nuevoEstado);
    actualizarFichasDesdeEstado(nuevoEstado.jugadores);
    marcarJugadorActual(nuevoEstado);
    actualizarJugadoresQuebrados(nuevoEstado.jugadores);
    verificarGanador(nuevoEstado);
    actualizarPanelJugadores(nuevoEstado);
});

btnTerminarTurno.addEventListener("click", async () => {
    if (hayDecisionPendiente) {
        eventosDiv.textContent = "No puedes terminar tu turno, debes resolver la propiedad actual";
        return;
    }

    await fetch(`${API_BASE}/terminar-turno`);
    
    yaTireLosDados = false;
    btnTerminarTurno.style.display = "none";
    btnDado.style.display = "block";

    // Pedimos el estado para refrescar las pantallas
    const estadoActualizado = await fetch(`${API_BASE}/estado`);
    const nuevoEstado = await estadoActualizado.json();
    procesarActualizacionEstado(nuevoEstado);
});

iniciarFrontend();
