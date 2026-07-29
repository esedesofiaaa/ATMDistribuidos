package lenin;

import java.net.ServerSocket;

import lenin.java_server_socket.JavaServerSocket;
import lenin.server.Atm;
import lenin.server.Server;

/**
 * Punto de entrada del servidor del ATM.
 *
 * Atiende clientes de forma secuencial (uno a la vez, sin hilos). El saldo vive
 * en memoria y persiste entre conexiones mientras el proceso siga corriendo, lo
 * que permite que el cliente Java y el cliente Python operen sobre la misma
 * cuenta uno despues del otro.
 */
public class Main {

  private static final int PORT = 1802;
  private static final int BACKLOG = 100;
  private static final String SALDO_INICIAL = "500000.00";

  public static void main(String[] args) {
    System.out.println("=== Servidor ATM (protocolo de texto | + \\n) ===");

    JavaServerSocket javaServerSocket = new JavaServerSocket(PORT, BACKLOG);
    ServerSocket serverSocket = javaServerSocket.get();
    if (serverSocket == null) {
      System.out.println("ERROR: no se pudo abrir el puerto " + PORT);
      return;
    }

    Atm atm = new Atm(SALDO_INICIAL);
    Server server = new Server(serverSocket, atm);

    System.out.println("Escuchando en el puerto " + PORT);
    System.out.println("Saldo inicial: " + atm.consultarSaldo());
    System.out.println("Ctrl+C para detener.\n");

    // Un cliente a la vez: se acepta, se atiende una peticion, se cierra y se
    // vuelve a esperar al siguiente.
    while (true) {
      if (!server.bind()) {
        break;
      }

      String request = server.listen();

      // null = el cliente cerro la conexion sin enviar una peticion completa.
      if (request == null) {
        System.out.println("   el cliente cerro sin enviar peticion");
      } else {
        String response = server.process(request);
        server.response(response);
        System.out.println("   respuesta enviada: " + response);
      }

      server.close();
      System.out.println("<- Conexion cerrada\n");
    }

    System.out.println("Servidor detenido.");
  }
}
