package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class UDPIOThread implements Runnable {

    Chatserver chatserver;
    DatagramPacket packet;

    public UDPIOThread(Chatserver chatserver, DatagramPacket packet) {
        this.packet = packet;
        this.chatserver = chatserver;
    }

    public void run() {
        String input;
        while (packet != null) {
            input = new String(packet.getData(), 0, packet.getLength());
            if (input.equals("!list")) {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    String users = chatserver.users();
                    DatagramPacket out = new DatagramPacket(users.getBytes(), users.getBytes().length, packet.getSocketAddress());
                    socket.send(out);
                    socket.close();
                } catch (IOException e) {
                    chatserver.getUserResponseStream().println("Server broke down");
                }
            }
        }

    }
}
