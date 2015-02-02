/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//Student Simon Hedström(c13simhe)

package UDPChat.Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

/**
 *
 * @author brom
 */
public class ServerConnection 
{
	
	//Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;
	
    private DatagramSocket m_socket = null;
    private InetAddress m_serverAddress = null;
    private int m_serverPort = -1;
    private String m_name = null;
    private boolean m_ack = false;

    public ServerConnection(String hostName, int port) 
    {
		m_serverPort = port;
		try {
			m_serverAddress = InetAddress.getByName(hostName);
			System.out.println("Found host");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.err.println("Failure to retrieve server");
		}
		try {
			m_socket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("Failure to create socket");
		}
    }

    public boolean handshake(String name) 
    {
    	m_name = name;
   	// TODO:
   	// * marshal connection message containing user name
   	// * send message via socket
   	// * receive response message from server
   	// * unmarshal response message to determine whether connection was successful
   	// * return false if connection failed (e.g., if user name was taken)    
		
    	byte[] buf = new byte[name.length() + 2];
		//Add a one so we have a bit first to manipulate
		String clientName = 1 + "2" + name;
		buf = clientName.getBytes();
		buf[0] = 1;
		buf[1] = (byte) name.length();
		
	    DatagramPacket packet = new DatagramPacket(buf, buf.length,
	    									m_serverAddress,m_serverPort);
	    try{
	    	System.out.println("Trying handshake");
	    	m_socket.send(packet);
	    } catch(IOException e)
	    {
	    	System.err.println("Not able to handshake");
	    }
	    
	    byte[] returnBuf = new byte[8];
	    DatagramPacket reply = new DatagramPacket(returnBuf, returnBuf.length);
	    try{
	    	System.out.println("Waiting for answer");
	    	m_socket.receive(reply);
	    } catch(IOException e)
	    {
	    	System.err.println("Not able to receive message");
	    }
	    
	    if(returnBuf[0] == 0)
	    {
	    	boolean replyB = replyMessage(m_serverAddress, m_serverPort);
    		if(replyB == true)
	    	System.err.println("Darn already a user with that name");
	    	return false;
	    	
	    }
	    else
	    {
	    	boolean replyA = replyMessage(m_serverAddress, m_serverPort);
    		if(replyA == true)
	    	System.out.println("Connection established");
	    	return true;
	    }
	    
    }

    public String receiveChatMessage() {
    	byte[] buf = new byte[256];
    	DatagramPacket message = new DatagramPacket (buf, buf.length);
    	try {
			m_socket.receive(message);
		} catch (IOException e) {
			System.err.println("Not able to receive");
		}
    	
    	String received = new String(buf);
    	if(received.contains("ack"))
    	{
    		m_ack = true;
    		return "";
    	}
    	else
    	{
    		boolean reply = replyMessage(m_serverAddress, m_serverPort);
    		if(reply == true)
    		{
		    	received = received.trim();
		    	return received+"\n";
    		}
    		return "";
    	}
    }

    public void sendChatMessage(String message) {
    	// TODO: 
		// * marshal message if necessary
		// * send a chat message to the server
    	byte buf[] = new byte[message.length() + 2];
    		
    	//If it starts with a slash look at what command
    	if(message.startsWith("/") == true)
    	{
    		//Split message so we have the command and the message
    		String[] splinter = message.split(" ",2);
    		System.out.println("Slash command issued");

    		switch(splinter[0])
    		{
	    		case "/tell":
	    			//Tell / whisper to identify it's specifically to you
	    			splinter[1] = m_name + " whispered:  " + splinter[1];
	    			message = 2 + "1" + splinter[1];
	    			buf = message.getBytes();
	    			buf[0] = 3;
	    			buf[1] = (byte) splinter[1].length();
	    			break;

	    		case "/list":
	    			String listMsg = 4 + "2" + m_name;
	    			buf = listMsg.getBytes();
	    			buf[0] = 4;
	    			buf[1] = (byte) m_name.length();
	    			break;
	    				
	    		case "/leave":
	    			String msg = "5" + 2  + m_name;
	    			buf = msg.getBytes();
	    			buf[0] = 5;
	    			buf[1] = (byte) m_name.length();	
	    			break;
	    		
    		}
    	}
    		
    	else
    	{
    		message = 2 + "1" + m_name + " : " + message;
    		buf = message.getBytes();
    		buf[0]  = 2;
    		buf[1]  = (byte) message.length();	
		}
    	
    	DatagramPacket packet = new DatagramPacket(buf, buf.length,
				m_serverAddress,m_serverPort);
    	sendMessage(packet);
 
    }
   
    private void sendMessage(DatagramPacket packet)
    {
    	m_ack = false;
    	Random generator = new Random();
        double failure;	
    	while(m_ack == false)
    	{
    	    failure = generator.nextDouble();
    		if (failure > TRANSMISSION_FAILURE_RATE)
    		{
    			try 
    			{
    				m_socket.send(packet);
    			} catch (IOException e) {
    				System.err.println("Unable to send message");
    			}
    		    		
    		 }
    		else{
    	   		// Message got lost
    			System.err.println("Lost message");
    	    	}
    		 try {
    				Thread.sleep(1);
    			} catch (InterruptedException e) {
    				System.err.println("Thread no sleep");
    			}
    	   }
    }
    
    
    public boolean replyMessage(InetAddress address, int port)
    {
    	byte[] returnBuf = new byte[24];
		String one = 1 + m_name;
		returnBuf = one.getBytes();
		//Message type
		returnBuf[0] = 6;
		DatagramPacket reply = new DatagramPacket(returnBuf, returnBuf.length,
				address,port);
		
		boolean result = false;
		Random generator = new Random();
        
		//So we don't need to ack the ack
        for(int i = 0; i <= 5; i++)
        {
        	double failure = generator.nextDouble();
	        if (failure > TRANSMISSION_FAILURE_RATE)
			{
				try 
				{
					m_socket.send(reply);
					
				} catch (IOException e) {
					System.err.println("Unable to send message");
				}
				result = true;
				break;
			 }
			else{
		   		// Message got lost
				System.err.println("Lost message");
				result =  false;
		    	}
        }
        return result;
    }
}
