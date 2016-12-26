package client;

import util.Config;

import java.io.*;
import java.net.*;
import java.nio.Buffer;
import java.util.Queue;
import java.util.concurrent.*;


public class Client implements IClientCli, Runnable {

    private String componentName;
    private Config config;
    private InputStream userRequestStream;
    private PrintStream userResponseStream;
    private boolean running = true;
    private Socket socket;
    private ServerSocket privateServerSocket;
    private BufferedReader in;
    private BufferedReader user_in;
    private PrintWriter out;
    private Thread tcpMessageRecieverThread;
    private String lastMessage = "No message received!";
    private Thread queProducer;
    private Thread queConsumer;
    public UDPMessageReciever udpMessageReciever;
    private String lastLookup;
    private ExecutorService executorService;
    private String name;

    private ConcurrentLinkedQueue<String> queue;

    //ToDO: java.con blocking que

    /**
    private static BufferedReader inputLine = null;
    private static PrintStream os = null;
    // The input stream
    private static DataInputStream is = null;
    **/

    /**
     * @param componentName      the name of the component - represented in the prompt
     * @param config             the configuration to use
     * @param userRequestStream  the input stream to read user input from
     * @param userResponseStream the output stream to write the console output to
     */
    public Client(String componentName, Config config,
                  InputStream userRequestStream, PrintStream userResponseStream) {
        this.componentName = componentName;
        this.config = config;
        this.userRequestStream = userRequestStream;
        this.userResponseStream = userResponseStream;
        this.udpMessageReciever = new UDPMessageReciever(this);
        executorService = Executors.newCachedThreadPool();
        queue = new ConcurrentLinkedQueue<String>();


        try {
            // Connect to Nakov Chat Server
            socket = new Socket(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port"));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException ioe) {
            System.out.println("No connection to Server");
            try {
                exit();
            } catch (IOException e) {
                System.out.println("No connection to Server");
            }
        }

        queProducer = new Thread(new QueProducer(this));
        queProducer.start();

        queConsumer = new Thread(new QueConsumer(this));
        queConsumer.start();

    }

    public void run() {
        //try {
            // Read messages from the server and print them
            String client_message;
            String server_message;
            String output;
            user_in = new BufferedReader(new InputStreamReader(userRequestStream));

        try {
            //while ((message=in.readLine()) != null && !message.equals("***ServerShutdown") && !message.equals("***ClientShutdown") && running) {
            while (running) {

                if((client_message = user_in.readLine()) != null) {

                    String[] m = client_message.split(" ");
                    if(client_message.equals("!quit")) {
                        this.exit();
                        return;
                    }
                    else if (client_message.startsWith("!login") && m.length == 3) {
                        this.login(m[1],m[2]);
                    }
                    else if(client_message.equals("!logout")) {
                        this.logout();
                    }
                    else if(client_message.startsWith("!register") && m.length == 2) {
                        this.register(m[1]);
                    }
                    else if(client_message.startsWith("!lookup") && m.length >= 2) {
                        String lookup = m[1];
                        /**
                        if(m.length == 3) {
                            lookup += " "+m[2];
                        }**/
                        this.lookup(lookup);
                    }
                    else if(client_message.startsWith("!msg") && m.length == 3) {
                        this.msg(m[1], m[2]);
                    }
                    else if (client_message.startsWith("!send")) {
                        this.send(client_message);
                    }
                    else if(client_message.startsWith("!lastMsg") && m.length == 1) {
                        userResponseStream.println(this.lastMsg());
                    } else if(client_message.startsWith("!list") && m.length == 1) {
                        this.list();
                    } else if(client_message.startsWith("!exit")) {
                        this.exit();
                    }
                    else {
                        userResponseStream.println("Unkown command.");
                        //client.getOut().println(message);
                        //client.getOut().flush();
                    }
                }
            }
        } catch (IOException exp) {
            System.out.println("No connection to Server");
        }


        try {
            exit();
        } catch (IOException e) {
            System.out.println("No connection to Server");
        }
    }

    @Override
    public String exit ()throws IOException {

        this.logout();
        running = false;


        if(udpMessageReciever != null) {
            udpMessageReciever.exit();
        }

        if(tcpMessageRecieverThread != null) {
            tcpMessageRecieverThread.interrupt();
        }

        if(queProducer != null) {
            queProducer.interrupt();
        }

        if(queConsumer != null) {
            queConsumer.interrupt();
        }

        if(userRequestStream != null) {
            userRequestStream.close();
        }

        if(privateServerSocket != null) {
            privateServerSocket.close();
        }


        if(user_in != null) {
            user_in.close();
        }


        if(executorService != null) {
            executorService.shutdown();
        }

        if(userResponseStream != null) {
            userResponseStream.close();
        }

        if(socket != null) {
            socket.close();
        }


        return "Client was shut down sucessfully";
    }

    @Override
    public String login (String username, String password)throws IOException {
        out.println("!login "+username+" "+password);
        out.flush();
        return "login";
    }

    @Override
    public String logout ()throws IOException {
        out.println("!logout");
        out.flush();
        return null;
    }

    @Override
    public String send (String message)throws IOException {
        out.println(message);
        out.flush();
        return null;
    }

    @Override
    public String list ()throws IOException {
        FutureTask<String> userList = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return udpMessageReciever.getUserList();
            }
        });
        executorService.execute(userList);
        try {
            return userList.get();
        } catch (InterruptedException e) {
            System.out.println("No connection to Server");
        } catch (ExecutionException e) {
            System.out.println("No connection to Server");
        }
        return null;


    }

    @Override
    public String msg (String username, String message)throws IOException {
        String ipadress;
        lookup(username + " !");

        lastLookup = null;


        while(lastLookup == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("No connection to Server");
            }
        }

        ipadress = lastLookup;


        Socket privateSocket = null;

        try {
            String[] ipa = ipadress.split(":");
            privateSocket = new Socket(ipa[0], Integer.parseInt(ipa[1]));
            // create a reader to retrieve messages send by the server
            BufferedReader privatServerReader = new BufferedReader(
                    new InputStreamReader(privateSocket.getInputStream()));
            // create a writer to send messages to the server
            PrintWriter privatServerWriter = new PrintWriter(
                    privateSocket.getOutputStream(), true);
            // write provided user input to the socket
            privatServerWriter.println(name +": " +message);
            userResponseStream.println(username + " replied with " + privatServerReader.readLine()+".");


        } catch (Exception e) {
            userResponseStream.println("Wrong username or user not registered.");
        } finally {
            if (privateSocket != null && !privateSocket.isClosed())
                try {
                    privateSocket.close();
                } catch (IOException e) {
                    System.out.println("No connection to Server");
                }
        }
        //}
        //in_private
        return null;
    }

    @Override
    public String lookup (String username)throws IOException {
        out.println("!lookup "+username);
        out.flush();
        return null;
    }

    @Override
    public String register (String privateAddress)throws IOException {
        String[] address = privateAddress.split(":");
        try {
            privateServerSocket = new ServerSocket(Integer.parseInt(address[1]));
            tcpMessageRecieverThread = new Thread(new TCPMessageReciever(this));
            tcpMessageRecieverThread.start();

            out.println("!register "+privateAddress);
            out.flush();

        } catch (BindException be) {
            userResponseStream.println("Connection Error");
        } catch (NumberFormatException ne) {
            userResponseStream.println("Connection Error");
        } catch (Exception pe) {
            userResponseStream.println("Connection Error");
        }
        return null;
    }

    @Override
    public String lastMsg ()throws IOException {
        return lastMessage;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public String getLastLookup() {
        return lastLookup;
    }

    public void setLastLookup(String lastLookup) {
        this.lastLookup = lastLookup;
    }

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public PrintStream getUserResponseStream() {
        return userResponseStream;
    }

    public void setUserResponseStream(PrintStream userResponseStream) {
        this.userResponseStream = userResponseStream;
    }

    public InputStream getUserRequestStream() {
        return userRequestStream;
    }

    public void setUserRequestStream(InputStream userRequestStream) {
        this.userRequestStream = userRequestStream;
    }

    public ServerSocket getPrivateServerSocket() {
        return privateServerSocket;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrivateServerSocket(ServerSocket privateServerSocket) {
        this.privateServerSocket = privateServerSocket;
    }

    public ConcurrentLinkedQueue<String> getQueue() {
        return queue;
    }

    public Config getConfig() {
        return config;
    }

    /**
     * @param args
     *            the first argument is the name of the {@link Client} component
     */

    public static void main(String[] args) {


        Client client;
        client = new Client(args[0], new Config("client"), System.in, System.out);

        client.run();

        // TODO: start the client
    }

    // --- Commands needed for Lab 2. Please note that you do not have to
    // implement them for the first submission. ---

    @Override
    public String authenticate(String username) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }



}

