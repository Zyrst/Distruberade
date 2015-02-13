package TCPServer;

//
//Source file for the server side. 
//
//Created by Sanny Syberfeldt
//Maintained by Marcus Brohede
//

//Student Simon Hedström(c13simhe)

import java.net.*;
import java.io.*;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Server  {
	
 private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
 private HashMap<String, Thread> m_clientThreads = new HashMap<String, Thread>();
 private ServerSocket m_socket;
 private Socket clientSocket;
 private ObjectInputStream  m_in;
 private ObjectOutputStream  m_out;

 public static void main(String[] args){    	
		if(args.length < 1) {
		    System.err.println("Usage: java Server portnumber");
		    System.exit(-1);
		}
		try {
		    Server instance = new Server(Integer.parseInt(args[0]));
		    instance.listenForClientMessages();
		    instance.run();
		} catch(NumberFormatException e) {
		    System.err.println("Error: port number must be an integer.");
		    System.exit(-1);
		}
 }

 private Server(int portNumber) 
 {
	// TODO: create a socket, attach it to port based on portNumber, and assign it to m_socket
 	try {
			m_socket = new ServerSocket(portNumber);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Not able to bind socket");
		}
 }
 
 private void createSocket()
 {
	 try {
			clientSocket = m_socket.accept();
			System.out.println("Accepting on server socket");
		} catch (IOException e) {
			System.err.println("Not able to receive packet");
		}
	 
	 try {
		 m_out = new ObjectOutputStream(clientSocket.getOutputStream());
	} catch (IOException e) {
		System.err.println("Unable to create Obj Output stream");
		e.printStackTrace();
	}
	try {
		m_in = new ObjectInputStream (clientSocket.getInputStream());
	} catch (IOException e) {
		System.err.println("Unable to create Obj Input stream");
		e.printStackTrace();
	}
 }

 @SuppressWarnings("unchecked")
private void listenForClientMessages() 
 {
		System.out.println("Waiting for client messages... ");
		do {
			checkStatus();
			
			//Accept a client , this is gonna be an handshake message
			createSocket();
			
			JSONObject obj = new JSONObject();
			JSONParser parser = new JSONParser();
			String inMSG = null; 
			//Stream in a message 
			try {
				inMSG = (String) m_in.readObject();
				System.out.println("Try to read on input stream");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("Unable to read an object");
				e.printStackTrace();
			}
			System.out.println(inMSG);
			//Parse it to a json object
			try {
				obj = (JSONObject) parser.parse(inMSG);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String command = obj.get("command").toString();
			String sender = obj.get("name").toString();
			System.out.println("Command issued " + command + " for" + sender);

			//Add a client
			
			//Receive data about the client
			int port = clientSocket.getPort();
			InetAddress address = clientSocket.getInetAddress();
			ServerSocket m_client = null ;
			try {
				System.out.println("Try and make a server socket");
				//Bind a random port, client unique
				m_client = new ServerSocket(0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			//Have the client to so we can do some "magic"
			ClientConnection added = null;
			//Test if the name already exist
			added = addClient(sender, address, port,m_client);
			//JSON object used for the return message
			JSONObject returnMSG = new JSONObject();
				
			//If we still have null meant there already was a user with that name
			if (added == null)
			{
				//Deny the client
				returnMSG.put("decision", false);
				String message = "Not able to join";
				returnMSG.put("message", message);
				System.err.println("Already a user with that name");
				try {
					m_out.writeObject(returnMSG.toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
				}
			//If anything else than null , accept it
			else
			{
				returnMSG.put("decision", true);
				returnMSG.put("port", m_client.getLocalPort());
				System.out.println("No user with that name");
				try {
					m_out.writeObject(returnMSG.toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				added.acceptClient();
				Thread clientThread = new Thread( added );
				clientThread.start();
				m_clientThreads.put(sender,clientThread );
					
			}
			 
			//Broadcast who joined	
			JSONObject announce = new JSONObject();
			announce.put("message", sender + " has joined");
			broadcast(announce);
			
		} while (true);
 }
 
 @SuppressWarnings({ "unchecked", "deprecation" })
public void serverCommands(JSONObject obj)
 {
	checkStatus();
	String command = obj.get("command").toString();
	String sender = obj.get("name").toString();
	JSONObject returnMSG = new JSONObject();
	System.out.println("Command issued " + command);
	switch(command)
	{
			//Broadcast message
			case "broadcast":
			{
				//TODO Receive the message and broadcast it
				String msg = obj.get("message").toString();
				msg = sender + ": " + msg;
				JSONObject sendMsg = new JSONObject();
				sendMsg.put("message", msg);
				System.out.println("Broadcast message");
				broadcast(sendMsg);
				break;
			}
			
			//Send a private message
			case "tell":
			{
				//TODO Use JSON Parameters and make a private message
				String name = obj.get("target").toString();
				String message = obj.get("message").toString();
				returnMSG.put("message", message);

				sendPrivateMessage(returnMSG, name);
				break;
			}
			
			//List all the connected users
			case "list":
			{
				//TODO List all connected users and return them
				ClientConnection c;
				String name = null;
				int i = 0;
				//List all connected users
				//Add to string which gets returned
				for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
				{
					c = itr.next();
					//First person no need for space in front of name
					if( i == 0)
					{
						name = c.getName();
						i++;
					}
					// The rest
					else
					{
						name += "  " + c.getName();
						i++;
					}
				}
				String msg = "Current users: " + name;
				returnMSG.put("message", msg);
				sendPrivateMessage(returnMSG,sender);
				break;
			}
			
			//Disconnect a user who request it
			case "disconnect":
			{
				//TODO Disconnect a user
				
				ClientConnection c;
				for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
				{
				    c = itr.next();
				    if(c.hasName(sender))
				    {
				    	
				    	System.out.println("Removed user");
				    	String msg = sender + " has left";
				    	returnMSG.put("message", msg);
						broadcast(returnMSG);
						m_connectedClients.remove(c);
						Thread client = m_clientThreads.get(sender);
						client.stop();
						m_clientThreads.remove(sender);
				    	break;
				    }
				}
				break;
			}
			case "help":
			{
				String secCommand = null;
				secCommand =  obj.get("second command").toString();
				
				String msg = null;
				
				System.out.println(secCommand);
					switch(secCommand)
					{
						case "helper": 
							msg = "Type /help and a command\n join, leave, broadcast, tell, list, zoidberg";
							break;
						case "join":
							msg = " /join - Join the chatroom";
							break;
						case "leave":
							msg = "/leave - will make you leave the chatroom";
							break;
						case "broadcast":
							msg = "/broadcast - normal message sent to everyone in the chatroom";
							break;
						case "tell":
							msg = "/tell - send a private message to a user";
							break;
						case "list":
							msg = "/list - list all connected users";
							break;
						case "zoidberg":
							msg = "/zoidberg - send a ascii charcter of zoidberg to everyone in the chatroom";
							break;	
						case "null":
							return;
					}
					returnMSG.put("message", msg);
					sendPrivateMessage(returnMSG, sender);
					break;
			}
				
			
			case "zoidberg": 
			{	
				returnMSG.put("message","(V)(°,,,°)(V) Why not Zoidberg?");
				broadcast(returnMSG);
				break;
			}
			case "join":
				createSocket();
				return;
				
		}
	checkStatus();
 }
 
 @SuppressWarnings({ "unchecked", "deprecation" })
private void checkStatus()
 {
	 JSONObject returnMSG = new JSONObject();
	 ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();)
		{
			c=itr.next();
			String sender = c.getName();
			Thread thread = m_clientThreads.get(sender);
			if(!thread.isAlive())
			{
				System.out.println("Removed user");
		    	String msg = sender + " has left";
		    	returnMSG.put("message", msg);
				broadcast(returnMSG);
				m_connectedClients.remove(c);
				Thread client = m_clientThreads.get(sender);
				client.stop();
				m_clientThreads.remove(sender);
		    	break;
			}
		}
				
 }
 
 public ClientConnection addClient(String name, InetAddress address, int port, ServerSocket socket) 
 {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
		    c = itr.next();
		    if(c.hasName(name)) {
			return null; // Already exists a client with this name
		    }
		}
		c = new ClientConnection(name, address, port, socket, this);
		m_connectedClients.add( c );
		return c;
 }

 public boolean sendPrivateMessage(JSONObject message, String name) 
 {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
		{
		    c = itr.next();
		    if(c.hasName(name)) {
		    	c.sendMessage(message);
		    	return true;
		    }
		}
		return false;
 }

 public void broadcast(JSONObject message)
 {
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
		{
		    itr.next().sendMessage(message);
		}
 }
 public void run()
	{
	 long startTime = System.currentTimeMillis();
	 long elapsedTime = 0;
	 while(true)
	 {
		 elapsedTime = System.currentTimeMillis() - startTime;
		 if(elapsedTime > 200)
		 {
			 checkStatus();
			 startTime = System.currentTimeMillis();
		
		 }
	 }
	
	}
}