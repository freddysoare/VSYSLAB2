package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class UDPMessageReciever {
    private DatagramSocket udpSocket;
    private Client client;

    public UDPMessageReciever(Client client) {
        this.client = client;
    }

    public String getUserList() {
        try {
            udpSocket = new DatagramSocket();
            String request = "!list";
            byte[] bufferI = request.getBytes();
            DatagramPacket packetI = new DatagramPacket(bufferI, bufferI.length,
                    InetAddress.getByName(client.getConfig().getString("chatserver.host")),
                    client.getConfig().getInt("chatserver.udp.port"));
            udpSocket.send(packetI);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
            udpSocket.receive(packet);
            String userList = new String(packet.getData(),0,packet.getLength());
            userList = userList.substring(0, userList.length()-1);
            client.getQueue().add(userList);
            return userList;
        } catch (IOException e) {
            System.out.println("No connection to Server");
        }
        return null;

    }

    public void exit() {
        if(udpSocket != null) {
            udpSocket.close();
        }
    }

}
