package chatserver;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nameserver.INameserverForChatserver;
import nameserver.exceptions.InvalidDomainException;
import util.Config;
import util.SecurityUtils;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	public InputStream userRequestStream;
    public PrintStream userResponseStream;
	private Config config;
    private Config userConfig;
    private boolean running = true;
    private static ConcurrentHashMap<String,String> register;

    private static ArrayList<Thread> clients;
    private TCPManagerThread tcpManagerThread;
    private UDPManagerThread udpManagerThread;
    private ExecutorService executorService;
    BufferedReader reader;

    //+++Aufgabe 2
    private INameserverForChatserver nameserver;
    private Registry registry;
    //---Auftage 2

    /**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
        this.register = new ConcurrentHashMap<>();
		this.componentName = componentName;
		this.config = config;
        this.userConfig = new Config("user");
        this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
        this.executorService = Executors.newCachedThreadPool();
        this.reader = new BufferedReader(new InputStreamReader(userRequestStream));

        SecurityUtils.registerBouncyCastle();


        //+++Aufgabe 2
        try {
            registry = LocateRegistry.getRegistry(config.getString("registry.host"),config.getInt("registry.port"));
            nameserver = (INameserverForChatserver) registry.lookup("root-nameserver");
        } catch (RemoteException ex) {
            userResponseStream.println("No connection to Root-NS");
            try {
                this.exit();
            } catch (IOException e) {
                userResponseStream.println(e.getMessage());
            }
        } catch (NotBoundException ex) {
            userResponseStream.println("No connection to Root-NS");
            try {
                this.exit();
            } catch (IOException e) {
                userResponseStream.println(ex.getMessage());
            }
        }
        //---Aufgabe 2

        tcpManagerThread = new TCPManagerThread(this);
        new Thread(tcpManagerThread).start();

        udpManagerThread = new UDPManagerThread(this);
        new Thread(udpManagerThread).start();

    }

    public void run() {
        try {

            while (userRequestStream != null) {
                String line = reader.readLine();
                if(line.equals("!exit")) {
                    exit();
                    return;
                } else if(line.equals("!users")) {
                    userResponseStream.print(users());
                } else {
                    userResponseStream.println("Unkown command.");
                }
            }
        } catch (IOException e) {
            userResponseStream.println("No connection to Server");
        }

        try {
            this.exit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
	public String users() throws IOException {

        String userStatus = "";
        Set<String> users = userConfig.listKeys();
        for(String user: users) {
            userStatus += user.substring(0,user.length()-9);
            if(register.get(user.substring(0,user.length()-9)) != null) {
                userStatus += " online\n";
            } else {
                userStatus += " offline\n";
            }
        }

        return userStatus;
	}

	@Override
	public String exit() throws IOException {

        if(tcpManagerThread != null) {
            tcpManagerThread.exit();
        }

        if(udpManagerThread !=  null) {
            udpManagerThread.exit();
        }

        if(userResponseStream != null) {
            userResponseStream.close();
        }

        if(userRequestStream !=null) {
            userRequestStream.close();
        }

        if(executorService != null) {
            executorService.shutdown();
        }

        return "Server was shutdown sucessfully";
	}


	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
        Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
        chatserver.run();

        // TODO: start the chatserver
	}

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public static void setRegister(ConcurrentHashMap<String, String> register) {
        Chatserver.register = register;
    }

    public static ConcurrentHashMap<String, String> getRegister() {
        return register;
    }

    public Config getUserConfig() {
        return userConfig;
    }

    public void setUserConfig(Config userConfig) {
        this.userConfig = userConfig;
    }

    public TCPManagerThread getTcpManagerThread() {
        return tcpManagerThread;
    }

    public PrintStream getUserResponseStream() {
        return userResponseStream;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public INameserverForChatserver getNameserver() {
        return nameserver;
    }

    public void setNameserver(INameserverForChatserver nameserver) {
        this.nameserver = nameserver;
    }

    public INameserverForChatserver getNameserver(String domain) throws InvalidDomainException{

        if(!(domain.length() > 0)) {
            throw new InvalidDomainException("no nameserver found");
        }

        INameserverForChatserver subnameserver = nameserver;
        ArrayList<String> domainparts = new ArrayList<>();
        domainparts.addAll(Arrays.asList(domain.split("\\.")));
        domainparts.remove(0);
        Collections.reverse(domainparts);
        for(String d : domainparts) {
            try {
                subnameserver = (INameserverForChatserver) subnameserver.getNameserver(d);
                if(subnameserver == null) {
                    throw new InvalidDomainException("no nameserver found");
                }
            } catch (RemoteException e) {
                throw new InvalidDomainException(e.getMessage());
            }
        }
        return subnameserver;
    }
}

