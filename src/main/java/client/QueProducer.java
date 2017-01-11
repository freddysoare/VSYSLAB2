package client;

import java.io.IOException;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class QueProducer implements Runnable {
    Client client;
    String message;

    public QueProducer(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                message = client.getChannel().readLine();
                if (message.startsWith("!public")) {
                    String m = message.substring(8, message.length());
                    client.setLastMessage(m);
                    client.getQueue().add(m);
                    continue;
                } if (message.startsWith("!slookup")) {
                    client.setLastLookup(message.split(" ")[1]);
                    continue;
                } if (message.startsWith("!login")) {
                    String[] m = message.split(":");
                    client.setName(m[1]);
                    client.getQueue().add(m[2]);
                    continue;
                }
                else {
                    client.getQueue().add(message);
                    continue;
                }
            }
        } catch (IOException e) {
            System.out.println("No connection to Server");
        }
    }
}
