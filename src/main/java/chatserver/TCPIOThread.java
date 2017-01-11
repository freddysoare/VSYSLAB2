package chatserver;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import org.bouncycastle.util.encoders.Base64;
import util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Arrays;

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
    private Channel channel;
    private BaseChannel baseChannel;
    private Base64Channel base64Channel;
    private RSAChannel rsaChannel;
    private AESChannel aesChannel;


    public TCPIOThread(Chatserver chatserver, Socket tcpSocket) {
        this.chatserver = chatserver;
        this.tcpSocket = tcpSocket;
        this.loggedIn = false;
        try {
            in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
            baseChannel = new BaseChannel(in,out);
            base64Channel = new Base64Channel(baseChannel);
            rsaChannel = new RSAChannel(
                    base64Channel,
                    null,
                    Keys.readPrivatePEM(new File(chatserver.getConfig().getString("key")))
                    );
            channel = rsaChannel;
            //aesChannel = new AESChannel(base64Channel);
        } catch (IOException e) {
            chatserver.getUserResponseStream().println("Server broke down");
        }
        catch (NoSuchPaddingException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String clientCommand;
            /*SecureChannel_client victor = new SecureChannel_client(new PrintStream(System.out),tcpSocket);
            victor.authenticate();*/
            //RSAChannel_2.authenticate_server(tcpSocket);
            //byte[] read = channel.read();
            //String wubdi = new String(read);
            while ((clientCommand = channel.readLine()) != null)  {

                System.out.println("[SERVER] RECEIVED: " + clientCommand);
                //System.out.println("[SERVER] DECODED: " + decypher_AES(clientCommand));
                /*if(!loggedIn) {
                    clientCommand = decypher_RSA(clientCommand);
                } else {
                    clientCommand = decypher_AES(clientCommand);
                }*/

                String[] c = clientCommand.split(" ");
                if(loggedIn) {
                    if (clientCommand.startsWith("!lookup") && c.length >= 2) {
                        try {
                            String ipadress = chatserver.getNameserver(c[1]).lookup(c[1]);
                            if (ipadress != null && ipadress.length() != 0) {
                                if (c.length == 3) {
                                    ipadress = "!slookup " + ipadress;
                                }
                                channel.println(ipadress);
                                channel.flush();
                            } else {
                                channel.println("Wrong username or user not registered.");
                                channel.flush();
                            }
                        } catch (Exception e) {
                            channel.println("Wrong username or user not registered.");
                            channel.flush();
                        }
                    } else if(clientCommand.startsWith("!login")) {
                        channel.println("Already logged in.");
                        channel.flush();
                    } else if(clientCommand.startsWith("!logout")) {
                        chatserver.getRegister().remove(clientName);
                        loggedIn = false;
                        channel.println("Successfully logged out.");
                        channel.flush();
                    } else if (clientCommand.startsWith("!send")) {
                        //ToDo Threadsave Arraylist
                        for (TCPIOThread ci : chatserver.getTcpManagerThread().getTcpioThreads()) {
                            ci.channel.println("!public " + clientCommand.substring(6, clientCommand.length()));
                            ci.channel.flush();
                        }
                    } else if (clientCommand.startsWith("!register") && c.length == 2 && loggedIn) {

                        //+++Aufgabe 2
                        //chatserver.getRegister().put(clientName,c[1]);
                        try {
                            chatserver.getNameserver(clientName).registerUser(clientName,c[1]);
                            channel.println("Successfully registered address for "+clientName+".");
                            channel.flush();
                        } catch (AlreadyRegisteredException e) {
                            channel.println(clientName+" already registered.");
                            channel.flush();
                        } catch (InvalidDomainException e) {
                            channel.println("The nameserver "+clientName+" is not running.");
                            channel.flush();
                        }
                        //---Aufgabe 2
                    }
                    else if (clientCommand != null) {
                        channel.println("Server: Wrong command!");
                        channel.flush();
                    }
                }






                //login checker
                else if (clientCommand.startsWith("!login") && c.length == 3 && !loggedIn) {
                    try {
                        if (chatserver.getRegister().get(c[1]) != null) {
                            channel.println("Already logged in.");
                            channel.flush();
                        } else if (chatserver.getUserConfig().getString(c[1] + ".password").equals(c[2])) {
                            clientName = c[1];
                            chatserver.getRegister().put(clientName, "");
                            loggedIn = true;
                            channel.println("!login:"+clientName+":Successfully logged in.");
                            channel.flush();
                        } else {
                            channel.println("Wrong username or password.");
                            channel.flush();
                        }
                    } catch (Exception e) {
                        channel.println("Wrong username or password.");
                        channel.flush();
                    }

                } else if(clientCommand.startsWith("!authenticate") && c.length == 3 && !loggedIn)
                {
                    //System.out.println(c[1]+" wants to autheticate!");
                    PublicKey userKey = Keys.readPublicPEM(new File(chatserver.getConfig().getString("keys.dir") + "/" + c[1] + ".pub.pem"));
                    rsaChannel.setEncryptionKey(userKey);

                    //part1
                    Key key = SecurityUtils.generateAES_KEY(256);
                    byte[] serverChallenge = SecurityUtils.secureRandomNumber(32);
                    byte[] iv = SecurityUtils.secureRandomNumber(16);
                    byte[] keyEncoded = key.getEncoded();
                    String msg = "!ok" + " "
                            + c[2] + " "
                            + new String(SecurityUtils.base64Encode(serverChallenge))+ " "
                            + new String(SecurityUtils.base64Encode(keyEncoded)) + " "
                            + new String(SecurityUtils.base64Encode(iv));

                    channel.println(msg);
                    channel.flush();

                    //part2
                    try
                    {
                        aesChannel = new AESChannel(base64Channel,key,iv);
                    }
                    catch (NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException e)
                    {
                        System.err.println("Couldn't open AESChannel");
                    }
                    String answer = aesChannel.readLine();
                    if (!Arrays.equals(SecurityUtils.base64Decode(answer.getBytes()), serverChallenge))
                    {
                        System.err.println("Server challenge doesn't match!");
                        return;
                    }

                    channel = aesChannel;
                    clientName = c[1];
                    chatserver.getRegister().put(clientName, "");
                    loggedIn = true;


                }
                else {
                    channel.println("Not logged in.");
                    channel.flush();
                }


                channel.flush();
            }
        } catch (IOException ioe) {
            try
            {
                baseChannel.println("IOException");
                baseChannel.flush();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        catch (NoSuchAlgorithmException | ClassNotFoundException e)
        {
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

}
