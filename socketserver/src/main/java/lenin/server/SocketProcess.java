package lenin.server;

/**
 * Contrato del ciclo de vida del servidor sobre el socket.
 *
 * Los mensajes ya no son Object serializados por Java, sino String con el
 * formato de la Capa 2 del protocolo, lo que los hace legibles por cualquier
 * lenguaje.
 */
public interface SocketProcess {

  /** Espera (bloqueando) a que un cliente se conecte y abre la sesion. */
  boolean bind();

  /** Lee una peticion del cliente. Devuelve null si el cliente cerro. */
  String listen();

  /** Envia una respuesta al cliente. */
  boolean response(String message);

  /** Cierra la sesion del cliente atendido. */
  boolean close();
}
