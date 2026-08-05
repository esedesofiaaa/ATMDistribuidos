# ATM sobre sockets — Protocolo de texto multiplataforma

Cajero automático cliente-servidor sobre sockets TCP, con un **servidor en Java** que atiende
indistintamente a un **cliente en Java** y a un **cliente en Python**.

El objetivo del ejercicio es mostrar qué se necesita para que dos sistemas escritos en lenguajes
distintos se comuniquen: un protocolo explícito, acordado y documentado.

## Estructura

```
sockets/
├── socketserver/   Servidor Java (Maven, Java 25) — lógica del ATM
├── clientsocket/   Cliente Java (Maven, Java 21) — interfaz de consola
└── clientpy/       Cliente Python — interfaz de consola
```

Los tres componentes son **independientes**: no comparten código, ni imports, ni build.
Lo único que comparten es el formato de los mensajes descrito más abajo. Agregar un cliente en
un cuarto lenguaje no requiere tocar nada de los existentes.

## El problema que resuelve

La versión original usaba `ObjectOutputStream` / `ObjectInputStream`, la **serialización nativa de
Java**: cabecera `AC ED 00 05`, tipos identificados por nombre de clase Java, grafo de objetos.
Ese formato solo lo entiende Java, así que un cliente Python era imposible — recibía bytes que no
podía interpretar y el servidor fallaba al leer bytes que no eran un objeto Java serializado.

La solución fue reemplazar esa capa por **texto plano UTF-8**, legible por cualquier lenguaje.

## El protocolo, en 3 capas

### Capa 1 — Framing (delimitación de mensajes)

TCP es un flujo continuo de bytes: no tiene noción de "mensaje". Hay que marcar dónde termina uno.

- **Delimitador:** el carácter de salto de línea `\n` (**byte 10**, `0x0A`).
- **Emisor:** concatena `\n` al mensaje, lo convierte a bytes UTF-8 y lo envía.
- **Receptor:** lee del socket **byte por byte**, acumulando en un buffer temporal
  (`ByteArrayOutputStream` en Java, `bytearray` en Python). Al detectar el byte 10, cierra el
  mensaje, lo entrega a la Capa 2 y limpia el buffer.

**Fin de conexión:** si la lectura devuelve `-1` en Java (o `b''` en Python), el extremo remoto
cerró el socket. El bucle se corta de inmediato y se liberan los recursos. Sin esta validación el
bucle giraría en vacío quemando CPU.

### Capa 2 — Encoding (formato del mensaje)

Texto plano **UTF-8**, campos separados por el carácter pipe `|`. El primer campo es el comando
(en la petición) o el estado (en la respuesta); los siguientes son los parámetros o los datos.

```
RETIRAR|50000
OK|Retiro realizado. Nuevo saldo: 450000.00
```

El receptor reconstruye el string y lo parte con `split('|')`.

### Capa 3 — Contrato (comandos y flujo)

**Flujo síncrono de un solo uso por conexión:**

```
cliente conecta → envía petición → servidor procesa → servidor responde → se cierra la conexión
```

Cada operación del menú abre su propio socket. El saldo vive en memoria del servidor y **persiste
entre conexiones** mientras el proceso siga corriendo.

| Petición | Respuesta correcta | Respuesta con error |
|---|---|---|
| `CONSULTAR_SALDO` | `OK\|saldo_actual` | — |
| `DEPOSITAR\|monto` | `OK\|mensaje` | `ERROR\|motivo` |
| `RETIRAR\|monto` | `OK\|mensaje` | `ERROR\|motivo` |

Toda la validación vive en el servidor (*fat server*): montos no numéricos, montos no positivos,
fondos insuficientes y comandos desconocidos. Los clientes son *thin clients*: capturan la entrada
del usuario y muestran lo que responda el servidor, sin validar nada.

Las operaciones son **atómicas**: se aplican completas o no se aplican. Un rechazo por fondos
insuficientes no modifica el saldo.

## Arquitectura del código

| Capa | Servidor (Java) | Cliente (Java) | Cliente (Python) |
|---|---|---|---|
| 1 y 2 | `server/Session.java` | `client/Session.java` | `read_message` / `write_message` / `encode` / `decode` |
| 3 | `server/Server.java` (`process`) | `client/Client.java` | `request` / `show` |
| Negocio | `server/Atm.java` | — | — |
| Arranque | `Main.java` | `Main.java` | `main()` |

Las dos clases `Session` son **contrapartes independientes**, no código compartido: mismo
delimitador, mismo separador y mismo charset, implementados por separado en cada proyecto. Esa
duplicación es intencional y es justamente el punto — lo que interopera es el formato, no el código.

## Cómo ejecutar

Levanta primero el servidor y déjalo corriendo; después abre los clientes.

### Servidor (Java)

El `pom.xml` del servidor apunta a Java 25, así que necesita un JDK 25 o superior en tiempo de
ejecución. Si el `java` del PATH es más antiguo, invoca el JDK explícitamente:

```bash
cd socketserver && mvn compile && /opt/homebrew/opt/openjdk/bin/java -cp target/classes lenin.Main
```

Arranca escuchando en el puerto **1802** con un saldo inicial de **500000.00**, e imprime cada
petición y respuesta para que se pueda seguir el intercambio.

### Cliente Java

```bash
cd clientsocket && mvn compile && java -cp target/classes lenin.Main
```

### Cliente Python

```bash
cd clientpy && python3 client.py
```

### Cliente crudo, para depurar

Como el protocolo es texto plano, se puede hablar con el servidor sin escribir un cliente:

```bash
printf 'CONSULTAR_SALDO\n' | nc 127.0.0.1 1802
```

Esto es imposible con serialización nativa de Java, y es una buena prueba de que el formato es
realmente neutral.

## Demostración de interoperabilidad

Con el servidor corriendo, la secuencia que evidencia que ambos clientes comparten estado:

1. Desde **Python**: `CONSULTAR_SALDO` → `OK|500000.00`
2. Desde **Python**: `RETIRAR|100000` → `OK|Retiro realizado. Nuevo saldo: 400000.00`
3. Desde **Python**: `DEPOSITAR|50000.50` → `OK|Deposito realizado. Nuevo saldo: 450000.50`
4. Desde **Java**: `CONSULTAR_SALDO` → `OK|450000.50` ← *ve las operaciones hechas desde Python*
5. Desde **Java**: `RETIRAR|50000` → `OK|Retiro realizado. Nuevo saldo: 400000.50`
6. Desde **`nc`**: `CONSULTAR_SALDO` → `OK|400000.50` ← *ve las operaciones de ambos clientes*

Casos de error verificados:

```
RETIRAR|99999999   ->  ERROR|Fondos insuficientes. Saldo disponible: 450000.50
RETIRAR|-5         ->  ERROR|El monto a retirar debe ser mayor que cero
DEPOSITAR|abc      ->  ERROR|El monto 'abc' no es un numero valido
DEPOSITAR          ->  ERROR|Falta el monto a depositar
VOLAR|1            ->  ERROR|Comando no reconocido: VOLAR
```

## Detalles de implementación

- **`split("|")` en Java es una expresión regular.** Un pipe sin escapar significa "alternación
  vacía" y parte el string carácter por carácter. Se usa `split("\\|", -1)`; el `-1` conserva los
  campos vacíos al final.
- **Se ignora el byte 13 (`\r`)** en ambos lenguajes, para tolerar clientes que envíen CRLF como
  `telnet` o `nc`.
- **Montos con `BigDecimal`** en el servidor, no `double`, para evitar errores de redondeo en
  aritmética de dinero.
- **`close()` aísla cada cierre** en su propio `try`, de modo que un fallo al cerrar un stream no
  impida cerrar el socket. Ninguna excepción escapa del método.

## Limitaciones conocidas

Son deliberadas, para mantener el ejercicio enfocado en el protocolo:

- **Sin concurrencia.** El servidor atiende un cliente a la vez, de forma secuencial. Mientras
  procesa una petición, otro cliente espera en la cola del sistema operativo. Como cada conexión
  dura milisegundos, en la práctica no se nota.
- **Sin autenticación.** Hay una sola cuenta y no se piden credenciales.
- **Sin persistencia.** El saldo vive en memoria y se reinicia al reiniciar el servidor.
- **Sin escape de `|`.** Si un valor contuviera un pipe, rompería el parseo. Los datos actuales
  (montos y mensajes fijos) no lo contienen.
- **Sin cifrado.** El tráfico va en texto plano; cualquiera en la red puede leerlo.

## Nota sobre el mundo real

Escribir el protocolo a mano es lo valioso de este ejercicio, pero en producción esta capa no se
programa: **gRPC**, **Apache Thrift** o **Java RMI** generan automáticamente el código de
serialización y transporte a partir de una definición del contrato. Por dentro hacen exactamente
lo que hay aquí — framing, encoding y despacho de comandos — con formatos más eficientes y
manejo de errores más completo.

