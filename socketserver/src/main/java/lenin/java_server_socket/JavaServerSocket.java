package lenin.java_server_socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaServerSocket {
	private int port;
	private int amountClients;

	public JavaServerSocket(int port, int amountClients) {
		this.port = port;
		this.amountClients = amountClients;
	}

  public ServerSocket get() {
		try {
			return new ServerSocket(this.port, this.amountClients);
		} catch (IOException e) { 
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, e.getMessage(), e);    
			return null;
		}
	}
}
