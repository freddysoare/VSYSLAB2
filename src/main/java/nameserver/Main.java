package nameserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by alfredmincinoiu on 28/12/2016.
 */
public class Main {
    public static void main(String...args) {

        System.out.println("1");

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 10059);
            System.out.println("2");
            INameserver server = (INameserver) registry.lookup("vienna.at");
            System.out.println("3");


            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                String input = userInputReader.readLine();
                if (input == null || input.startsWith("!stop")) {
                    break;
                } else if (input.startsWith("!ping")) {
                    String response = server.lookup("");
                    System.out.println("Received response from Server: " + response);
                } else {
                    System.out.println("Command not known!");
                }

            }



        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
