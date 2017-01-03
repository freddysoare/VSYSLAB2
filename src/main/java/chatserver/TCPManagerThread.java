package chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class TCPManagerThread implements Runnable {

    private Chatserver chatserver;
    private ServerSocket serverSocket;
    private ArrayList<TCPIOThread> tcpioThreads;

    public TCPManagerThread(chatserver.Chatserver chatserver) {
        tcpioThreads = new ArrayList<>();
        this.chatserver = chatserver;
        try {
            serverSocket = new ServerSocket(chatserver.getConfig().getInt("tcp.port"));
        }
        catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        }

    }

    public void run() {
        try {
            while (serverSocket!= null && !serverSocket.isClosed()) {
                Socket tcpSocket = serverSocket.accept();
                TCPIOThread cliThread = new TCPIOThread(chatserver, tcpSocket);
                chatserver.getExecutorService().execute(cliThread);
                tcpioThreads.add(cliThread);
            }
        }
        catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        }
    }

    public void exit() {
        try {
            if(serverSocket != null) {
                serverSocket.close();
            }
            for (TCPIOThread tt : tcpioThreads) {
                if(tt != null) {
                    tt.exit();
                }
            }
        } catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        }
    }

    public ArrayList<TCPIOThread> getTcpioThreads() {
        return tcpioThreads;
    }

}
