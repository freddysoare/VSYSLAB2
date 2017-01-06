package chatserver;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.AESChannel;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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
    private AESChannel aesChannel;


    public TCPIOThread(Chatserver chatserver, Socket tcpSocket) {
        this.chatserver = chatserver;
        this.tcpSocket = tcpSocket;
        this.loggedIn = false;
        try {
            in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
            aesChannel = new AESChannel(out);
        } catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String clientCommand;
            while ((clientCommand = in.readLine()) != null)  {

                System.out.println("[SERVER] RECEIVED: " + clientCommand);
                System.out.println("[SERVER] DECODED: " + decypher_AES(clientCommand));
                /*if(!loggedIn) {
                    clientCommand = decypher_RSA(clientCommand);
                } else {
                    clientCommand = decypher_AES(clientCommand);
                }*/

                String[] c = clientCommand.split(" ");
                if(loggedIn == true) {
                    if (clientCommand.startsWith("!lookup") && c.length >= 2) {
                        try {
                            String ipadress = chatserver.getNameserver(c[1]).lookup(c[1]);
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

                        //+++Aufgabe 2
                        //chatserver.getRegister().put(clientName,c[1]);
                        try {
                            chatserver.getNameserver(clientName).registerUser(clientName,c[1]);
                            out.println("Successfully registered address for "+clientName+".");
                            out.flush();
                        } catch (AlreadyRegisteredException e) {
                            out.println(clientName+" already registered.");
                            out.flush();
                        } catch (InvalidDomainException e) {
                            out.println("The nameserver "+clientName+" is not running.");
                            out.flush();
                        }
                        //---Aufgabe 2
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
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Base64DecodingException e) {
            e.printStackTrace();
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

    public String decypher_RSA(String codedMessage) {
        return "";
    }

    public String decypher_AES(String codedMessage) throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, Base64DecodingException {
        return aesChannel.decrypt(codedMessage);
    }
}
