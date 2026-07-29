package lenin.client;

import java.net.Socket;

/**
 * Client — Capa 3 del protocolo, lado cliente (thin client).
 *
 * No valida nada: arma el comando, lo envia, lee la respuesta y la devuelve.
 * Toda la logica de negocio vive en el servidor.
 */
public class Client implements SocketProcess {

  private final Socket socket;
  private Session session;

  public Client(Socket socket) {
    this.socket = socket;
    this.session = null;
  }

  @Override
  public boolean connect() {
    this.session = new Session(this.socket);
    return true;
  }

  @Override
  public boolean response(String message) {
    if (this.session == null) {
      return false;
    }
    return this.session.write(message);
  }

  @Override
  public String listen() {
    if (this.session == null) {
      return null;
    }
    // Una sola lectura: el contrato es una respuesta por conexion.
    return this.session.read();
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

  /**
   * Ejecuta una operacion completa: envia el comando y devuelve la respuesta
   * cruda del servidor (por ejemplo "OK|500000.00").
   *
   * @return la respuesta del servidor, o null si la conexion se corto.
   */
  public String execute(String... fields) {
    String request = Session.encode(fields);
    if (!this.response(request)) {
      return null;
    }
    return this.listen();
  }
}
