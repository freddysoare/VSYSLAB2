package client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class TCPMessageReciever implements Runnable {
    private boolean running = true;
    private ServerSocket socket;
    private Socket clientSocket;
    BufferedReader in;
    PrintWriter out;
    Client client;

    public TCPMessageReciever(Client client) {
        this.client = client;
        this.socket = client.getPrivateServerSocket();
    }

    public void run() {
        while(!Thread.interrupted()) {
            try {
                clientSocket = socket.accept();
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                String clientCommand = in.readLine();
                client.getUserResponseStream().println(clientCommand);
                out.println("!ack");
                out.flush();
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("No connection to Server");
            }
        }
    }
}
