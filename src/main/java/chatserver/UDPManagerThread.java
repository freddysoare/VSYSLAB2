package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class UDPManagerThread implements Runnable {

    private Chatserver chatserver;
    private DatagramSocket serverListener;

    public UDPManagerThread(Chatserver chatserver) {
        this.chatserver = chatserver;
        try {
            serverListener = new DatagramSocket(chatserver.getConfig().getInt("udp.port"));
        }
        catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        }

    }

    public void run() {

        while (serverListener != null && !serverListener.isClosed()) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                serverListener.receive(packet);
                chatserver.getExecutorService().execute(new UDPIOThread(chatserver,packet));
            } catch (IOException e) {
                exit();
            }
        }
    }

    public void exit() {
        if (serverListener != null) {
            serverListener.close();
        }
    }
}
