package lenin.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server — Capa 3 del protocolo: interpreta el comando y produce la respuesta.
 *
 * Atiende un cliente a la vez (sin hilos). El contrato es de un solo uso por
 * conexion: el cliente conecta, envia una peticion, recibe la respuesta y la
 * conexion se cierra.
 */
public class Server implements SocketProcess {

  private final ServerSocket serverSocket;
  private final Atm atm;
  private Session session;

  public Server(ServerSocket serverSocket, Atm atm) {
    this.serverSocket = serverSocket;
    this.atm = atm;
    this.session = null;
  }

  @Override
  public boolean bind() {
    try {
      Socket socket = this.serverSocket.accept();
      System.out.println("-> Cliente conectado desde " + socket.getInetAddress().getHostAddress());
      this.session = new Session(socket);
      return true;
    } catch (IOException e) {
      System.out.println("ERROR aceptando la conexion: " + e.getMessage());
      return false;
    }
  }

  @Override
  public String listen() {
    if (this.session == null) {
      return null;
    }
    // Una sola lectura: no hay bucle, porque el contrato es una peticion por
    // conexion. Si devuelve null, el cliente cerro sin enviar nada.
    return this.session.read();
  }

  @Override
  public boolean response(String message) {
    if (this.session == null) {
      return false;
    }
    return this.session.write(message);
  }

  @Override
  public boolean close() {
    if (this.session == null) {
      return true;
    }
    boolean ok = this.session.close();
    this.session = null;
    return ok;
  }

  // ------------------------------------------------------------ Capa 3

  /**
   * Procesa una peticion del protocolo y devuelve la respuesta ya formateada.
   *
   * Peticion:  COMANDO|parametro
   * Respuesta: OK|dato  |  ERROR|motivo
   */
  public String process(String request) {
    // Capa 2: se parte el mensaje por '|'. El campo 0 es el comando.
    String[] fields = Session.decode(request);

    if (fields.length == 0 || fields[0].isBlank()) {
      return Session.encode("ERROR", "Peticion vacia");
    }

    String command = fields[0].trim().toUpperCase();
    System.out.println("   peticion recibida: " + request);

    switch (command) {
      case "CONSULTAR_SALDO":
        return Session.encode("OK", this.atm.consultarSaldo());

      case "DEPOSITAR": {
        if (fields.length < 2) {
          return Session.encode("ERROR", "Falta el monto a depositar");
        }
        String error = this.atm.depositar(fields[1]);
        if (error != null) {
          return Session.encode("ERROR", error);
        }
        return Session.encode("OK",
            "Deposito realizado. Nuevo saldo: " + this.atm.consultarSaldo());
      }

      case "RETIRAR": {
        if (fields.length < 2) {
          return Session.encode("ERROR", "Falta el monto a retirar");
        }
        String error = this.atm.retirar(fields[1]);
        if (error != null) {
          return Session.encode("ERROR", error);
        }
        return Session.encode("OK",
            "Retiro realizado. Nuevo saldo: " + this.atm.consultarSaldo());
      }

      default:
        return Session.encode("ERROR", "Comando no reconocido: " + command);
    }
  }
}
