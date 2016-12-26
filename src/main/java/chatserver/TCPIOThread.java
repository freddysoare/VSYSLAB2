package chatserver;

import java.io.*;
import java.net.Socket;

/**
 * Created by alfredmincinoiu on 26/12/2016.
 */
class TCPIOThread implements Runnable {

    private Chatserver chatserver;
    private Socket tcpSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean loggedIn;
    private String clientName;


    public TCPIOThread(Chatserver chatserver, Socket tcpSocket) {
        this.chatserver = chatserver;
        this.tcpSocket = tcpSocket;
        this.loggedIn = false;
        try {
            in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
        } catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        }
    }

    public void run() {
        try {
            String clientCommand;
            while ((clientCommand = in.readLine()) != null)  {
                String[] c = clientCommand.split(" ");
                if(loggedIn == true) {
                    if (clientCommand.startsWith("!lookup") && c.length >= 2) {
                        try {
                            String ipadress = chatserver.getRegister().get(c[1]);
                            if (ipadress != null && ipadress.length() != 0) {
                                if (c.length == 3) {
                                    ipadress = "!slookup " + ipadress;
                                }
                                out.println(ipadress);
                                out.flush();
                            } else {
                                out.println("Wrong username or user not registered.");
                                out.flush();
                            }
                        } catch (Exception e) {
                            out.println("Wrong username or user not registered.");
                            out.flush();
                        }
                    } else if(clientCommand.startsWith("!login")) {
                        out.println("Already logged in.");
                        out.flush();
                    } else if(clientCommand.startsWith("!logout")) {
                        chatserver.getRegister().remove(clientName);
                        loggedIn = false;
                        out.println("Successfully logged out.");
                        out.flush();
                    } else if (clientCommand.startsWith("!send")) {
                        //ToDo Threadsave Arraylist
                        for (TCPIOThread ci : chatserver.getTcpManagerThread().getTcpioThreads()) {
                            ci.out.println("!public " + clientCommand.substring(6, clientCommand.length()));
                            ci.out.flush();
                        }
                    } else if (clientCommand.startsWith("!register") && c.length == 2 && loggedIn == true) {
                        chatserver.getRegister().put(clientName,c[1]);
                        out.println("Successfully registered address for "+clientName+".");
                        out.flush();
                    }
                    else if (clientCommand != null) {
                        out.println("Server: Wrong command!");
                        out.flush();
                    }
                }






                //login checker
                else if (clientCommand.startsWith("!login") && c.length == 3 && loggedIn == false) {
                    try {
                        if (chatserver.getRegister().get(c[1]) != null) {
                            out.println("Already logged in.");
                            out.flush();
                        } else if (chatserver.getUserConfig().getString(c[1] + ".password").equals(c[2])) {
                            clientName = c[1];
                            chatserver.getRegister().put(clientName, "");
                            loggedIn = true;
                            out.println("!login:"+clientName+":Successfully logged in.");
                            out.flush();
                        } else {
                            out.println("Wrong username or password.");
                            out.flush();
                        }
                    } catch (Exception e) {
                        out.println("Wrong username or password.");
                        out.flush();
                    }

                } else {
                    out.println("Not logged in.");
                    out.flush();
                }


                out.flush();
            }
        } catch (IOException ioe) {
            out.println("IOException");
            out.flush();
        }
    }

    public void exit() {
        try {
            if(tcpSocket != null) {
                tcpSocket.close();
            }
        } catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        }
    }

    public String getClientName() {
        return clientName;
    }
}
