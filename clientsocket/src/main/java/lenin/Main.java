package lenin;

import java.net.Socket;
import java.util.ArrayList;

import lenin.client.Client;
import lenin.client.SocketProcess;
import lenin.java_client_socket.JavaClientSocket;

/**
 * Hello world!
 *
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Java Client Socket");

        /*
         * New Java Socket instance
         */
        JavaClientSocket javaClientSocket = new JavaClientSocket(1802, "127.0.0.1");
        Socket clientSocket = javaClientSocket.get();
        if (clientSocket == null) {
            System.out.println("ClientSocket is null");
            return;
        }

        /*
         * New Client instance
         */
        SocketProcess client = new Client(clientSocket);

        /*
         * Connect to server
         */
        if (!client.connect()) {
            System.out.println("Failed to connect to server");
            return;
        }

        /*
         * Response to server
         */
        ArrayList<Object> dataResponse = new ArrayList<>();
        dataResponse.add("Hello from client");
        dataResponse.add("data");
        dataResponse.add(0);
        client.response(dataResponse);

        /*
         * Listen to server
         */
        ArrayList<Object> dataRequest = (ArrayList<Object>) client.listen();

        dataRequest.forEach(System.out::println);

        /*
         * Close server
         */
        if (!client.close()) {
            System.out.println("Client close failed");
            return;
        }

        System.out.println("Java Client Socket closed");
    }
}
