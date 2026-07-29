"""
client.py — Cliente ATM en Python.

Implementa exactamente las mismas 3 capas del protocolo que la Session de Java.
Lo unico compartido entre los dos clientes es el formato del mensaje, no el codigo:

  Capa 1 (Framing):  un mensaje termina en el byte 10 ('\\n'). La lectura es
                     byte por byte, acumulando en un bytearray.
  Capa 2 (Encoding): texto plano UTF-8, campos separados por pipe '|'.
  Capa 3 (Contrato): un solo uso por conexion. Conecta -> envia -> recibe -> cierra.

Comandos:
  CONSULTAR_SALDO      -> OK|saldo
  DEPOSITAR|monto      -> OK|mensaje  |  ERROR|motivo
  RETIRAR|monto        -> OK|mensaje  |  ERROR|motivo
"""

import socket

HOST = "127.0.0.1"
PORT = 1802

DELIMITER = 10          # byte 10 (0x0A) = '\n'
SEPARATOR = "|"
ENCODING = "utf-8"


# --------------------------------------------------------------- Capa 2

def encode(*fields):
    """Une los campos con '|'. Ej: encode("RETIRAR", "50000") -> 'RETIRAR|50000'"""
    return SEPARATOR.join(str(f) for f in fields)


def decode(message):
    """Parte el mensaje en sus campos. El primero es el estado (OK o ERROR)."""
    if message is None:
        return []
    return message.split(SEPARATOR)


# --------------------------------------------------------------- Capa 1

def write_message(sock, message):
    """Concatena el delimitador, codifica en UTF-8 y envia todos los bytes."""
    data = (message + chr(DELIMITER)).encode(ENCODING)
    sock.sendall(data)


def read_message(sock):
    """
    Lee un mensaje completo del socket, byte por byte, hasta el delimitador.

    Devuelve el mensaje sin el '\\n', o None si el servidor cerro la conexion
    antes de completarlo (recv devuelve b'' al cerrarse, el equivalente del -1
    de Java).
    """
    buffer = bytearray()

    while True:
        chunk = sock.recv(1)

        # Fin de stream: el servidor cerro. Se corta el bucle de inmediato para
        # no girar en vacio.
        if not chunk:
            return None

        byte_read = chunk[0]

        # Delimitador encontrado: el mensaje esta completo.
        if byte_read == DELIMITER:
            return buffer.decode(ENCODING)

        # Se ignora '\r' para tolerar CRLF.
        if byte_read == 13:
            continue

        buffer.append(byte_read)


# --------------------------------------------------------------- Capa 3

def request(*fields):
    """
    Abre una conexion, ejecuta una sola operacion y cierra.

    Devuelve la respuesta cruda del servidor (ej. 'OK|500000.00'), o None si
    no se pudo conectar o el servidor cerro sin responder.
    """
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((HOST, PORT))
        write_message(sock, encode(*fields))
        return read_message(sock)
    except OSError as e:
        print(f">> No se pudo comunicar con el servidor {HOST}:{PORT} ({e})")
        return None
    finally:
        # Cierre limpio siempre, haya funcionado o no.
        sock.close()


def show(response):
    """Interpreta la respuesta del protocolo y la muestra al usuario."""
    if response is None:
        print(">> Sin respuesta del servidor (conexion cerrada).")
        return

    fields = decode(response)
    status = fields[0] if len(fields) > 0 else ""
    detail = fields[1] if len(fields) > 1 else ""

    if status == "OK":
        print("\n>> " + detail)
    else:
        print("\n>> ERROR: " + detail)


# --------------------------------------------------------------- Interfaz

def menu():
    print("\n" + "=" * 40)
    print("        CAJERO AUTOMATICO (ATM)")
    print("=" * 40)
    print("1. Consultar saldo")
    print("2. Depositar")
    print("3. Retirar")
    print("4. Salir")
    print("-" * 40)


def main():
    print("=== Cliente ATM (Python) ===")

    while True:
        menu()
        opcion = input("Seleccione una opcion: ").strip()

        if opcion == "4":
            print("Gracias por usar el ATM.")
            break

        if opcion == "1":
            respuesta = request("CONSULTAR_SALDO")

        elif opcion == "2":
            monto = input("Monto a depositar: ").strip()
            respuesta = request("DEPOSITAR", monto)

        elif opcion == "3":
            monto = input("Monto a retirar: ").strip()
            respuesta = request("RETIRAR", monto)

        else:
            print(">> Opcion no reconocida.")
            continue

        show(respuesta)


if __name__ == "__main__":
    main()
