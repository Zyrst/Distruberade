package UDPChat.Server;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

//Student Simon Hedström(c13simhe)

import java.io.IOException;
import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
	
    private static CopyOnWriteArrayList<ClientConnection> m_connectedClients = new CopyOnWriteArrayList<ClientConnection>();
    private static DatagramSocket m_socket;

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
			m_socket = new DatagramSocket(portNumber);
			//Thread for looking for crashed clients
			Thread ack = new Thread(new ackThread());
			ack.start();
		} catch (SocketException e) {
			e.printStackTrace();
			System.err.println("Not able to bind socket");
		}
    }
    class ackThread implements Runnable
    {

    	@Override
    	public void run() 
    	{
    		// TODO Auto-generated method stub
    		while(true)
        	{
    	    	for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
    			{
    	    		String ack = "serverAck";
    			    itr.next().sendMessage(ack, m_socket);
    			}
    	    	//Try to remove crashed clients
    	    	Server.removeCrashed();
    	    	try {
    				Thread.sleep(500);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        	}
        }
    }
    	
    

    private void listenForClientMessages() 
    {
		System.out.println("Waiting for client messages... ");
		do {
			byte[] buf = new byte[256];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			
			try {
				m_socket.receive(packet);
				
			} catch (IOException e) {
				System.err.println("Not able to receive packet");
			}
			
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
					System.out.println();
					boolean reply = replyMessage(packet.getAddress(), packet.getPort(), splinter[0]);
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
			
			
		} while (true);
    }
    
    //Public function remove a client
    //Use it with a user client crashed
    public static void removeCrashed()
    {
    	ClientConnection c;
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
		{
			
		    c = itr.next();
		    int i = c.triedConnect();
		    String name = c.getName();
		    if(i > 10)
		    {
		    	//If tridconnect is over 10 than remove 
		    	//This might be wrong and a person should be tagged DC instead
		    	System.out.println("Removed crashed client");
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

    public static void broadcast(String message)
    {
		for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) 
		{
		    itr.next().sendMessage(message, m_socket);
		}
    }
    
    public boolean replyMessage(InetAddress address, int port,String name)
    {
    	//Send an ack that a message was received
		String one = "ack";
		return sendPrivateMessage(one, name);
    }
    
}



