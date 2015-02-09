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
import java.util.ArrayList;
import java.util.Iterator;

public class Server {
	
 private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
 private ServerSocket m_socket;
 private Socket clientSocket;

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

 private void listenForClientMessages() 
 {
		System.out.println("Waiting for client messages... ");
		Thread thread;
		do {
			
			
			try {
				clientSocket = m_socket.accept();
				
			} catch (IOException e) {
				System.err.println("Not able to receive packet");
			}
			//Create new thread for a new message
			thread = new Thread();
			thread.start();

			
			try {
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			} catch (IOException e) {
				System.err.println("Unable to create Obj Output stream");
				e.printStackTrace();
			}
			try {
				ObjectInputStream  in = new ObjectInputStream (clientSocket.getInputStream());
			} catch (IOException e) {
				System.err.println("Unable to create Obj Input stream");
				e.printStackTrace();
			}
			byte[] buf = null;
			//TODO Look at JSON object and see what message type
			//Which type of message in buf[0]
			switch(buf[0])
			{
				//Add a client
				case  1 :
				{
					//Receive data about the client
					int port = clientSocket.getPort();
					InetAddress address = clientSocket.getInetAddress();
					//Convert name to string from the buff
					//TODO Get name and stuff
					
					boolean added = false;
					//Test if the name already exist
					//added = addClient(name, address, port);
					
					//Return buffer 
					if (added == false)
					{
						//Deny the client
						//TODO Make an JSOn object with decision
						System.err.println("Already a user with that name");
					}
					else if(added == true)
					{
						System.out.println("No user with that name"); 
						
					}
				
					//Return the decision 
					//TODO Send back to client and broadcast it
					//sendPrivateMessage(msg,name);
					
					//broadcast(name + " has joined");
					break;
				}
				
				//Broadcast message
				case 2:
				{
					//TODO Receive the message and broadcast it
					String message = new String(buf, 2, buf[1]);
					String[] name = message.split(" ", 2);
					System.out.println("Broadcast message");
					broadcast(message);
						
					break;
				}
				
				//Send a private message
				case 3:
				{
					//TODO Use JSON Parameters and make a private message
					String message = new String(buf,2, buf[1]);
					
					//Split the name and the message
					//Due to me adding in a weird way I had to split and reconstruct
					//So we get <Name> whispered: <Message>
					String[] splinter = message.split(" ", 5);
					String construct = splinter[0] + " " + splinter[1] + " " + splinter[4];
					String name = splinter[3];
					sendPrivateMessage(construct, name);
				}
				
				//List all the connected users
				case 4:
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
				case 5:
				{
					//TODO Disconnect a user
					String name = new String(buf,2,buf[1]);
					ClientConnection c;
					for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
					{
					    c = itr.next();
					    if(c.hasName(name))
					    {
					    	System.out.println("Removed user");
					    	String msg = name + " has left";
							m_connectedClients.remove(c);
							broadcast(msg);
					    	break;
					    }
					}
					break;
				}
			}		
			removeCrashed();
			
		} while (true);
 }
 
 //Public function remove a client
 //Use it with a user client crashed
 public void removeCrashed()
 {
 	ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
		{
			
		    c = itr.next();
		    int i = c.triedConnect();
		    String name = c.getName();
		    if(i > 10 && c.hasName(name) == true)
		    {
		    	System.out.println("Tried to remove a crashed client");
		    	m_connectedClients.remove(c);
		    	String msg = name + " disconnected";
		    	broadcast(msg);
		    	break;
		    }
		   
		}
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
		m_connectedClients.add(new ClientConnection(name, address, port));
		return true;
 }

 public boolean sendPrivateMessage(String message, String name) 
 {
		ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
		{
		    c = itr.next();
		    if(c.hasName(name)) {
		    	c.sendMessage(message, m_socket);
		    	return true;
		    }
		}
		return false;
 }

 public void broadcast(String message)
 {
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
		{
		    itr.next().sendMessage(message, m_socket);
		}
 }

}