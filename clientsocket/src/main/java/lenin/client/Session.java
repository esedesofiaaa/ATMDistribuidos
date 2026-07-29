package lenin.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Session — Capas 1 y 2 del protocolo, lado cliente.
 *
 * Es la contraparte exacta de la Session del servidor: mismo delimitador,
 * mismo separador, mismo charset. Lo unico que comparten los dos proyectos es
 * el formato del mensaje, no el codigo.
 *
 * Capa 1 (Framing): un mensaje termina en el byte 10 ('\n'). La lectura es
 * byte por byte acumulando en un buffer temporal hasta encontrar el delimitador.
 * Capa 2 (Encoding): texto plano UTF-8 con campos separados por pipe '|'.
 */
public class Session {

  /** Delimitador de fin de mensaje: byte 10 (0x0A). */
  private static final int DELIMITER = 10;

  /** Separador de campos de la Capa 2. */
  private static final String SEPARATOR = "|";

  private final Socket socket;
  private InputStream input;
  private OutputStream output;

  public Session(Socket socket) {
    this.socket = socket;
    try {
      this.input = socket.getInputStream();
      this.output = socket.getOutputStream();
    } catch (IOException e) {
      System.out.println("ERROR abriendo streams: " + e.getMessage());
      this.input = null;
      this.output = null;
    }
  }

  // ---------------------------------------------------------------- Capa 1

  /**
   * Lee un mensaje completo del socket, byte por byte, hasta el delimitador.
   *
   * @return el mensaje sin el '\n', o null si el socket remoto se cerro
   *         (byte -1) o si hubo un error de E/S.
   */
  public String read() {
    if (this.input == null) {
      return null;
    }

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      while (true) {
        int byteRead = this.input.read();

        // Fin de stream: el servidor cerro la conexion. Se corta el bucle de
        // inmediato para no girar en vacio quemando CPU.
        if (byteRead == -1) {
          return null;
        }

        // Delimitador encontrado: el mensaje esta completo.
        if (byteRead == DELIMITER) {
          return buffer.toString(StandardCharsets.UTF_8);
        }

        if (byteRead == 13) {
          continue;
        }

        buffer.write(byteRead);
      }
    } catch (IOException e) {
      System.out.println("ERROR leyendo del socket: " + e.getMessage());
      return null;
    }
  }

  /**
   * Envia un mensaje: le concatena el delimitador '\n', lo convierte a bytes
   * UTF-8 y lo escribe en el socket.
   */
  public boolean write(String message) {
    if (this.output == null) {
      return false;
    }
    try {
      byte[] bytes = (message + (char) DELIMITER).getBytes(StandardCharsets.UTF_8);
      this.output.write(bytes);
      this.output.flush();
      return true;
    } catch (IOException e) {
      System.out.println("ERROR escribiendo en el socket: " + e.getMessage());
      return false;
    }
  }

  // ---------------------------------------------------------------- Capa 2

  /** Une los campos con '|' para formar el mensaje. Ej: RETIRAR|50000 */
  public static String encode(String... fields) {
    return String.join(SEPARATOR, fields);
  }

  /**
   * Parte el mensaje en sus campos. El primero es el estado (OK o ERROR).
   * Se escapa el pipe porque split() recibe una expresion regular.
   */
  public static String[] decode(String message) {
    if (message == null) {
      return new String[0];
    }
    return message.split("\\" + SEPARATOR, -1);
  }

  // --------------------------------------------------------------- Cierre

  /**
   * Cierra streams y socket. Cada cierre va aislado y ninguna excepcion
   * escapa del metodo.
   */
  public boolean close() {
    boolean ok = true;

    try {
      if (this.input != null) {
        this.input.close();
      }
    } catch (IOException e) {
      ok = false;
    }

    try {
      if (this.output != null) {
        this.output.close();
      }
    } catch (IOException e) {
      ok = false;
    }

    try {
      if (this.socket != null && !this.socket.isClosed()) {
        this.socket.close();
      }
    } catch (IOException e) {
      ok = false;
    }

    this.input = null;
    this.output = null;
    return ok;
  }
}
