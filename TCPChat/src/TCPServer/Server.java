package TCPServer;

//
//Source file for the server side. 
//
//Created by Sanny Syberfeldt
//Maintained by Marcus Brohede
//

//Student Simon Hedstr�m(c13simhe)

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Server {
	
 private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
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

 @SuppressWarnings("unchecked")
private void listenForClientMessages() 
 {
		System.out.println("Waiting for client messages... ");
		do {
			
			try {
				clientSocket = m_socket.accept();
			} catch (IOException e) {
				System.err.println("Not able to receive packet");
			}
			
			//TODO Not fuck things up
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
			
			JSONObject obj = new JSONObject();
			JSONParser parser = new JSONParser();
			String inMSG = null; 
		
			try {
				inMSG = (String) m_in.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("Unable to read an object");
				e.printStackTrace();
			}
			
			try {
				obj = (JSONObject) parser.parse(inMSG);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String command = obj.get("command").toString();
			String sender = obj.get("name").toString();
			System.out.println("Command issued " + command);
			//TODO Look at JSON object and see what message type
			
			switch(command)
			{
				//Add a client
				case  "handshake" :
				{
					//Receive data about the client
					int port = clientSocket.getPort();
					InetAddress address = clientSocket.getInetAddress();
					
					boolean added = false;
					//Test if the name already exist
					added = addClient(sender, address, port);
					
					JSONObject returnMSG = new JSONObject();
					
					if (added == false)
					{
						//Deny the client
						returnMSG.put("decision", false);
						System.err.println("Already a user with that name");
					}
					else if(added == true)
					{
						returnMSG.put("decision", true);
						System.out.println("No user with that name"); 
						
					}
				 
					//TODO Send back to client and broadcast it
					sendPrivateMessage(returnMSG,sender);
					JSONObject announce = new JSONObject();
					announce.put("message", sender + " has joined");
					if(added == true)
						broadcast(announce);
					break;
				}
				
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
				case "private":
				{
					//TODO Use JSON Parameters and make a private message
					
					//Split the name and the message
					//Due to me adding in a weird way I had to split and reconstruct
					//So we get <Name> whispered: <Message>
					
					//sendPrivateMessage(construct, name);
				}
				
				//List all the connected users
				case "list":
				{
					//TODO List all connected users and return them
					ClientConnection c;
					String name = null;
					//String sender = new String(buf,2,buf[1]);
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
							name += "   " + c.getName();
							i++;
						}
					}
					String msg = "Current users: " + name;;
					//sendPrivateMessage(msg,sender);
					break;
				}
				
				//Disconnect a user who request it
				case "disconnect":
				{
					//TODO Disconnect a user
					//String name = new String(buf,2,buf[1]);
					ClientConnection c;
					/*for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
					{
					    c = itr.next();
					    if(c.hasName(name))
					    {
					    	System.out.println("Removed user");
					    	//String msg = name + " has left";
							m_connectedClients.remove(c);
							//broadcast(msg);
					    	break;
					    }
					}*/
					break;
				}
			}					
		} while (true);
 }
 
 
 public boolean addClient(String name, InetAddress address, int port) 
 {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
		    c = itr.next();
		    if(c.hasName(name)) {
			return false; // Already exists a client with this name
		    }
		}
		m_connectedClients.add(new ClientConnection(name, address, port, m_out));
		return true;
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

}