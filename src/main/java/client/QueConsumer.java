package client;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class QueConsumer implements Runnable{

    Client client;

    public QueConsumer(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        String output;
        while (!Thread.interrupted()) {
            while ((output = client.getQueue().poll()) != null) {
                client.getUserResponseStream().println(output);
            }
        }
    }
}
