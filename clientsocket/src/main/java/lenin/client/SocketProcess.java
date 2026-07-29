package lenin.client;

/**
 * Contrato del ciclo de vida del cliente sobre el socket.
 *
 * Los mensajes son String con el formato de la Capa 2 del protocolo, no
 * objetos serializados por Java.
 */
public interface SocketProcess {

  /** Abre la sesion sobre el socket ya conectado. */
  boolean connect();

  /** Envia una peticion al servidor. */
  boolean response(String message);

  /** Lee la respuesta del servidor. Devuelve null si el servidor cerro. */
  String listen();

  /** Cierra la sesion. */
  boolean close();
}
