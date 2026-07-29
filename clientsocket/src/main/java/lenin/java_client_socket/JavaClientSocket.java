  package lenin.java_client_socket;

import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaClientSocket {
  private int port;
	private String address;

  public JavaClientSocket(int port, String address) {
    this.port = port;
    this.address = address;		
  }

  public Socket get() {
    try {
      return new Socket(InetAddress.getByName(address), this.port);
    } catch (Exception e) {
      Logger.getLogger(this.getClass().getName()).log(Level.WARNING, e.getMessage(), e);    
			return null;
    }
  }
}
