package nameserver;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import rmi_test.IServer;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, INameserver, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Registry registry;
	private BufferedReader reader;
	private String domain;
	private ArrayList<String> children;


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
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.reader = new BufferedReader(new InputStreamReader(userRequestStream));
		domain="";
		children = new ArrayList<>();

		run();
		// TODO
	}

	@Override
	public void run() {

		try {

			try {
				this.domain = config.getString("domain");
			} catch (Exception e) {
				domain = "";
			}
			//INameserverForChatserver remoteC = (INameserverForChatserver) UnicastRemoteObject.exportObject(this, 0);
			INameserver remote = (INameserver) UnicastRemoteObject.exportObject(this, 0);



			if(domain.length() == 0) {
				// create and export the registry instance on localhost at the
				// specified port
				registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
				// create a remote object of this server object
				// bind the obtained remote object on specified binding name in the
				// registry
				registry.bind(config.getString("root_id"), remote);
				//registry.bind("c"+config.getString("root_id"), remoteC);
				//TODO

			} else {

				try {
					registry = LocateRegistry.getRegistry("localhost", config.getInt("registry.port"));
					registerNameserver(domain,remote,null);
				} catch (AlreadyRegisteredException e) {
					e.printStackTrace();
					//TODO: Handle Exp
				} catch (InvalidDomainException e) {
					e.printStackTrace();
					//TODO: Handle Exp
				}

			}


		} catch (RemoteException e) {
			throw new RuntimeException("Error while starting server.", e);
		} catch (AlreadyBoundException e) {
			throw new RuntimeException(
					"Error while binding remote object to registry.", e);
		}


		try {
			while (userRequestStream != null) {
				String line = reader.readLine();
				if(line.equals("!exit")) {
					exit();
					return;
				} if(line.equals("!nameservers")) {
					userResponseStream.println(this.addresses());
				} else {
					userResponseStream.println("Unkown command.");
				}
			}
		} catch (IOException e) {
			userResponseStream.println("No connection to Server");
		}



	}

	@Override
	public String nameservers() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addresses() throws IOException {
		StringBuilder sb = new StringBuilder();
		for(String child: children) {
			sb.append(child+"\n");
		}
		return sb.toString();
	}

	@Override
	public String exit() throws IOException {

		if(userResponseStream != null) {
			userResponseStream.close();
		}

		if(userRequestStream !=null) {
			userRequestStream.close();
		}

		try {
			// unexport the previously exported remote object
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			System.err.println("Error while unexporting object: "
					+ e.getMessage());
		}

		try {
			// unbind the remote object so that a client can't find it anymore
			registry.unbind(config.getString("root_id"));
		} catch (Exception e) {
			System.err.println("Error while unbinding object: "
					+ e.getMessage());
		}

		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		// TODO: start the nameserver
	}

	@Override
	public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

		String[] domainparts = domain.split("\\.");
		if(domainparts.length>1) {
			userResponseStream.println("#2");
			try {

				String subdomain = domainparts[0];
				for(int i = 1; i<domainparts.length-1; i++) {
					subdomain+="."+domainparts[i];
				}
				userResponseStream.println("**"+domainparts[domainparts.length-1]);
				userResponseStream.println("**"+subdomain);

				INameserver remote = (INameserver) registry.lookup(domainparts[domainparts.length-1]);
				remote.registerNameserver(subdomain,nameserver,nameserverForChatserver);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		} else {
			userResponseStream.println("#3");
			userResponseStream.println("**"+domainparts.length);

			if(domain.equals(this.domain)) {
				userResponseStream.println("#4");
				//Add to root

				INameserver remote = null;
				try {
					remote = (INameserver) registry.lookup(config.getString("root_id"));
				} catch (NotBoundException e) {
					e.printStackTrace();
				}
				if(remote.addChildren(domain) == true) {
						try {
							registry.bind(config.getString("domain"), nameserver);
							//registry.bind("c"+config.getString("root_id"), nameserverForChatserver);
							//TODO C Registry
						} catch (AlreadyBoundException e) {
							e.printStackTrace();
						}
					}
			} else if(this.addChildren(domain) == true) {
				userResponseStream.println("#5");
				try {
					userResponseStream.println("**"+domain);
					userResponseStream.println("**"+this.domain);

					registry.bind(domain+this.domain, nameserver); //TODO Recursive Check
					//registry.bind("c"+config.getString("root_id"), nameserverForChatserver);
					//TODO C Registry
				} catch (AlreadyBoundException e) {
					e.printStackTrace();
				}
			}
			else {
				//TODO Excpetion
			}


		}



		//WRONG Way to digest domain
		/**
		userResponseStream.println("#1");
		String[] domainparts = domain.split(".");
		if(domainparts.length>=1) {
			userResponseStream.println("#2");
			String subdomain = domainparts[0];
			for(int i = 1; i<domainparts.length-1; i++) {
				subdomain+="."+domainparts[i];
			}
			try {
				INameserver remote = (INameserver) registry.lookup(config.getString(domainparts[0]));
				//digest domain
				remote.registerNameserver(subdomain,nameserver,nameserverForChatserver);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		} else {
			userResponseStream.println("#3");
			if(this.addChildren(domain) == true) {
				try {
					registry.bind(config.getString("domain"), nameserver);
					//registry.bind("c"+config.getString("root_id"), nameserverForChatserver);
					//TODO C Registry
				} catch (AlreadyBoundException e) {
					e.printStackTrace();
				}
			} else {
				//TODO Excpetion
			}
		}
		**/

	}

	@Override
	public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

	}

	@Override
	public INameserverForChatserver getNameserver(String zone) throws RemoteException {
		return null;
	}

	@Override
	public String lookup(String username) throws RemoteException {
		return null;
	}

	@Override
	public boolean addChildren(String domain) throws RemoteException {
		if(!children.contains(domain)) {
			this.children.add(domain);
			return true;
		} else {
			return false;
		}
	}
}
