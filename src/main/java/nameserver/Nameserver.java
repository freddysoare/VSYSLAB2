package nameserver;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
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
	private ConcurrentHashMap<String, INameserver> children;
	private ConcurrentHashMap<String, String> userAddresses;
	INameserver remote;
	INameserverForChatserver remoteC;


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
		children = new ConcurrentHashMap<>();
		userAddresses = new ConcurrentHashMap<>();

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
			remote = (INameserver) UnicastRemoteObject.exportObject(this, 0);
			remoteC = (INameserverForChatserver) remote;



			//case where root-ns is started
			if(domain.length()  == 0) {
				registry = LocateRegistry.createRegistry(config.getInt("registry.port"));
				registry.bind(config.getString("root_id"), remote);
			} else {
			//case when ns is attached to the hierarchy
					try {
						registry = LocateRegistry.getRegistry("localhost", config.getInt("registry.port"));
						INameserver rootNS = (INameserver) registry.lookup(config.getString("root_id"));
						synchronized (rootNS) {
							rootNS.registerNameserver(domain, remote, remoteC);
						}
					} catch (NotBoundException e) {
						userResponseStream.println(e.getMessage());
						try {
							this.exit();
						} catch (IOException e1) {
							userResponseStream.println(e1.getMessage());
						}
					} catch (AlreadyRegisteredException e) {
						userResponseStream.println(e.getMessage());
						try {
							this.exit();
						} catch (IOException e1) {
							userResponseStream.println(e1.getMessage());
						}
					} catch (InvalidDomainException e) {
						userResponseStream.println(e.getMessage());
						try {
							this.exit();
						} catch (IOException e1) {
							userResponseStream.println(e1.getMessage());
						}
					}
			}


		} catch (RemoteException e) {
			try {
				this.exit();
			} catch (IOException e1) {
				userResponseStream.println(e1.getMessage());
			}
			throw new RuntimeException("Error while starting server.", e);
		} catch (AlreadyBoundException e) {
			try {
				this.exit();
			} catch (IOException e1) {
				userResponseStream.println(e1.getMessage());
			}
			throw new RuntimeException(
					"Error while binding remote object to registry.", e);
		}


		try {
			while (userRequestStream != null) {
				String line = reader.readLine();
				if(line.equals("!exit")) {
					exit();
					return;
				} else if(line.equals("!nameservers")) {
					userResponseStream.println(this.nameservers());
				} else if(line.equals("!addresses")) {
					userResponseStream.println(this.addresses());
				} else {
					userResponseStream.println("Unkown command.");
				}
			}
		} catch (IOException e) {
			userResponseStream.println("No connection to Server");
		}
		try {
			this.exit();
		} catch (IOException e1) {
			userResponseStream.println(e1.getMessage());
		}



	}

	@Override
	public String nameservers() throws IOException {
		StringBuilder sb = new StringBuilder();
		for(String child: children.keySet()) {
			sb.append(child+"\n");
		}
		if(sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	@Override
	public String addresses() throws IOException {
		StringBuilder sb = new StringBuilder();
		for(String address: userAddresses.keySet()) {
			sb.append(address+" "+ userAddresses.get(address)+"\n");
		}
		if(sb.length() > 0) {
			sb.setLength(sb.length() - 1);
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

		if(domain.length()==0) {
			try {
				// unbind the remote object so that a client can't find it anymore
				registry.unbind(config.getString("root_id"));
			} catch (Exception e) {
				System.err.println("Error while unbinding object: "
						+ e.getMessage());
			}
		}

		return null;
	}



	@Override
	public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

		String[] domainparts = domain.split("\\.");
		if(domainparts.length>1) {
			//digest domain
			String subdomain = this.digestDomain(domain);
			INameserver remote = children.get(domainparts[domainparts.length-1]);
			remote.registerNameserver(subdomain,nameserver,nameserverForChatserver);

		} else {
			if(domain.equals(this.domain)) {
				//Add to root

				INameserver remote = null;
				try {
					remote = (INameserver) registry.lookup(config.getString("root_id"));
				} catch (NotBoundException e) {
					userResponseStream.println(e.getMessage());
				}
				remote.addChildren(domain,nameserver,nameserverForChatserver);
			} else  {
				this.addChildren(domain,nameserver,nameserverForChatserver);
			}


		}





	}

	@Override
	public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
		userAddresses.put(username,address);
	}

	@Override
	public INameserverForChatserver getNameserver(String zone) throws RemoteException {
		return children.get(zone);
	}

	@Override
	public String lookup(String username) throws RemoteException {
		return userAddresses.get(username);
	}

	@Override
	public void addChildren(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver)
			throws AlreadyRegisteredException, RemoteException {
		if(!children.containsKey(domain)) {
			this.children.put(domain,nameserver);
		} else {
			throw new AlreadyRegisteredException("Zone already exists");
		}
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),System.in, System.out);

		//Nameserver nameserver = new Nameserver("serv", new Config("ns-at"),
		//		System.in, System.out);
	}

	public String digestDomain(String domain) {
		String[] domainparts = domain.split("\\.");
		String subdomain = domainparts[0];
		for(int i = 1; i<domainparts.length-1; i++) {
			subdomain+="."+domainparts[i];
		}
		return subdomain;
	}

}
