package TCPServer;

//
//Source file for the server side. 
//
//Created by Sanny Syberfeldt
//Maintained by Marcus Brohede
//

//Student Simon Hedström(c13simhe)

import java.io.IOException;
import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {
	
 private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
 private ServerSocket m_socket;

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
			// TODO Auto-generated catch block
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
				m_socket.accept();
				
			} catch (IOException e) {
				System.err.println("Not able to receive packet");
			}
			//Create new thread for a new message
			thread = new Thread();
			thread.start();
			
			//Which type of message in buf[0]
			switch(buf[0])
			{
				//Add a client
				case  1 :
				{
					//Receive data about the client
					int port = packet.getPort();
					InetAddress address = packet.getAddress();
					//Convert name to string from the buff
					String name = new String(buf, 2, 20);
					//Remove white space
					name = name.trim();
					boolean added = false;
					//Test if the name already exist
					added = addClient(name, address, port);
					
					//Return buffer 
					byte[] returnBuf = new byte[8];
					if (added == false)
					{
						//Deny the client
						returnBuf[0] = 0;
						System.err.println("Already a user with that name");
					}
					else if(added == true)
					{
						returnBuf[0] = 1;
						System.out.println("No user with that name"); 
						
					}
				
					//Return the decision 
					String msg = returnBuf.toString();
					sendPrivateMessage(msg,name);
					
					broadcast(name + " has joined");
					break;
				}
				
				//Broadcast message
				case 2:
				{
					String message = new String(buf, 2, buf[1]);
					String[] name = message.split(" ", 2);
					System.out.println("Broadcast message");
					boolean reply = replyMessage(packet.getAddress(), packet.getPort(), name[0]);
					if(reply == true)
						broadcast(message);
						
					break;
				}
				
				//Send a private message
				case 3:
				{
					String message = new String(buf,2, buf[1]);
					
					//Split the name and the message
					//Due to me adding in a weird way I had to split and reconstruct
					//So we get <Name> whispered: <Message>
					String[] splinter = message.split(" ", 5);
					String construct = splinter[0] + " " + splinter[1] + " " + splinter[4];
					String name = splinter[3];
					boolean reply = replyMessage(packet.getAddress(), packet.getPort(), name);
					if(reply == true)
						sendPrivateMessage(construct, name);
				}
				
				//List all the connected users
				case 4:
				{
					ClientConnection c;
					String name = null;
					String sender = new String(buf,2,buf[1]);
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
					boolean reply = replyMessage(packet.getAddress(), packet.getPort(),sender);
					if(reply == true)
						sendPrivateMessage(msg,sender);
					break;
				}
				
				//Disconnect a user who request it
				case 5:
				{
					String name = new String(buf,2,buf[1]);
					ClientConnection c;
					for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
					{
					    c = itr.next();
					    if(c.hasName(name))
					    {
					    	System.out.println("Removed user");
					    	String msg = name + " has left";
					    	boolean reply = replyMessage(packet.getAddress(), packet.getPort(), name);
							if(reply == true)
								m_connectedClients.remove(c);
								broadcast(msg);
					    	break;
					    }
					}
					break;
				}
				//Ack reply
				case 6 : 
				{
					ClientConnection c;
					String name = new String(buf,1,20);
					name = name.trim();
					for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
					{
					    c = itr.next();
					    if(c.hasName(name))
					    {
					    	c.setAck(true);
					    }
					    
					}
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
 
 public boolean replyMessage(InetAddress address, int port,String name)
 {
 	//Send an ack that a message was received
 	//byte[] returnBuf = new byte[8];
		String one = "ack";
	/*	returnBuf = one.getBytes();
		DatagramPacket reply = new DatagramPacket(returnBuf, returnBuf.length,
				address,port);
		try {
			m_socket.send(reply);
			//broadcast(one);
		} catch (IOException e) {
			//Failed to reply , return false
			System.out.println("Tried sending a reply");
			return false;
		}
		return true;*/
			return sendPrivateMessage(one, name);
		

 }
}