package lenin;

import java.net.Socket;
import java.util.Scanner;

import lenin.client.Client;
import lenin.client.Session;
import lenin.java_client_socket.JavaClientSocket;

/**
 * Cliente ATM en Java.
 *
 * Interfaz de consola que captura la operacion del usuario y muestra lo que
 * responda el servidor. No valida montos ni saldos: eso es del servidor.
 *
 * Cumple el contrato de un solo uso por conexion: cada operacion del menu abre
 * un socket nuevo, envia una peticion, lee la respuesta y cierra.
 */
public class Main {

  private static final int PORT = 1802;
  private static final String HOST = "127.0.0.1";

  public static void main(String[] args) {
    System.out.println("=== Cliente ATM (Java) ===");
    Scanner scanner = new Scanner(System.in);

    while (true) {
      menu();
      String option = scanner.nextLine().trim();

      if (option.equals("4")) {
        System.out.println("Gracias por usar el ATM.");
        break;
      }

      String response;
      switch (option) {
        case "1":
          response = request("CONSULTAR_SALDO");
          break;

        case "2": {
          System.out.print("Monto a depositar: ");
          String monto = scanner.nextLine().trim();
          response = request("DEPOSITAR", monto);
          break;
        }

        case "3": {
          System.out.print("Monto a retirar: ");
          String monto = scanner.nextLine().trim();
          response = request("RETIRAR", monto);
          break;
        }

        default:
          System.out.println(">> Opcion no reconocida.");
          continue;
      }

      show(response);
    }

    scanner.close();
  }

  private static void menu() {
    System.out.println("\n" + "=".repeat(40));
    System.out.println("        CAJERO AUTOMATICO (ATM)");
    System.out.println("=".repeat(40));
    System.out.println("1. Consultar saldo");
    System.out.println("2. Depositar");
    System.out.println("3. Retirar");
    System.out.println("4. Salir");
    System.out.println("-".repeat(40));
    System.out.print("Seleccione una opcion: ");
  }

  /**
   * Abre una conexion, ejecuta una sola operacion y cierra.
   *
   * @return la respuesta cruda del servidor, o null si no hubo respuesta.
   */
  private static String request(String... fields) {
    JavaClientSocket javaClientSocket = new JavaClientSocket(PORT, HOST);
    Socket socket = javaClientSocket.get();
    if (socket == null) {
      System.out.println(">> No se pudo conectar al servidor " + HOST + ":" + PORT);
      return null;
    }

    Client client = new Client(socket);
    client.connect();
    String response = client.execute(fields);
    client.close();
    return response;
  }

  /** Interpreta la respuesta del protocolo y la muestra al usuario. */
  private static void show(String response) {
    if (response == null) {
      System.out.println(">> Sin respuesta del servidor (conexion cerrada).");
      return;
    }

    // Capa 2: el campo 0 es el estado, el campo 1 el dato o el motivo.
    String[] fields = Session.decode(response);
    String status = fields.length > 0 ? fields[0] : "";
    String detail = fields.length > 1 ? fields[1] : "";

    if (status.equals("OK")) {
      System.out.println("\n>> " + detail);
    } else {
      System.out.println("\n>> ERROR: " + detail);
    }
  }
}
