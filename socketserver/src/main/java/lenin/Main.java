package lenin;

import java.net.ServerSocket;
import java.util.ArrayList;

import lenin.java_server_socket.JavaServerSocket;
import lenin.server.Server;
import lenin.server.SocketProcess;

/**
 * Hello world!
 *
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Java Server Socket");

        /*
         * New Java Socket instance
         */
        JavaServerSocket javaServerSocket = new JavaServerSocket(1802, 100);
        ServerSocket serverSocket = javaServerSocket.get();
        if (serverSocket == null) {
            System.out.println("ServerSocket is null");
            return;
        }

        /*
         * New server instance
         */
        SocketProcess server = new Server(serverSocket);

        /*
         * Bind server accept
         */ 
        if (!server.bind()) {
            System.out.println("Server bind failed");
            return;
        }

        /*
         * Session get requests from client
         */
        ArrayList<Object> dataRequest = (ArrayList<Object>) server.listen();

        /*
         * Response to client
         */
        ArrayList<Object> dataResponse = new ArrayList<>();
        dataResponse.add("Hello from server your request is: ");
        dataRequest.forEach(dataResponse::add);
        dataResponse.add(0);
        server.response(dataResponse);

        /*
         * Close server
         */
        if (!server.close()) {
            System.out.println("Server close failed");
            return;
        }

        System.out.println("Java Server Socket closed");
    }
}
