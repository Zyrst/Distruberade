/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//Student Simon Hedström(c13simhe)

package TCPClient;

import java.io.*;
import java.net.*;
import java.util.Random;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 *
 * @author brom
 */
public class ServerConnection 
{
	
	//Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;
	
	private int handPort				= -1;
    private Socket m_socket 			= null;
    private InetAddress m_serverAddress = null;
    private int m_serverPort 			= -1;
    private String m_name				= null;
    private ObjectOutputStream m_out 	= null;
    private ObjectInputStream m_in		= null;

    public ServerConnection(String hostName, int port) 
    {
		m_serverPort = port;
		handPort = port;
		
		try {
			m_serverAddress = InetAddress.getByName(hostName);
			System.out.println("Found host");
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			System.err.println("Failure to retrieve server");
		}
		createSocket();
    }
    public void createSocket()
    {
    	try{
			m_socket = new Socket(m_serverAddress, m_serverPort);
			System.out.println("Made socket with port" + m_socket.getPort());
		    m_in = new ObjectInputStream(m_socket.getInputStream());
		    m_out = new ObjectOutputStream(m_socket.getOutputStream());
    	}
		catch (UnknownHostException e)
		{
			System.err.println("Don't know about host ");
	        System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to create streams and socket client side");
		}
    }
    @SuppressWarnings("unchecked")
	public boolean handshake(String name) throws IOException 
    {
    	m_name = name;
   	// TODO:
   	// * marshal connection message containing user name
   	// * send message via socket
   	// * receive response message from server
   	// * unmarshal response message to determine whether connection was successful
   	// * return false if connection failed (e.g., if user name was taken)    
    	JSONObject obj = new JSONObject();
    	
    	//Create our connection message
    	obj.put("command", "handshake");
    	obj.put("name", name);
    	
    	String message = obj.toString();
    	
    	//Send handshake message
		m_out.writeObject(message);
    	String returnDec = null;
    	
    	try {
			returnDec = (String) m_in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Not able to read an object");
		}

    	JSONParser parser = new JSONParser();
    	obj = new JSONObject();
    	try {
			obj = (JSONObject) parser.parse(returnDec);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to parse an JSONObject in handshake");
		}
    	
    	String decision = obj.get("decision").toString();
    	System.out.println("First serverport"+ m_serverPort);
	    if(decision == "true")
	    {
	    	//New socket which we shall communicate over
	    	String Port = obj.get("port").toString();
	    	m_serverPort = Integer.parseInt(Port);
	    	//Close old one
	    	m_socket.close();
	    	//Create new one
	    	createSocket();
	    	System.out.println("Connection established");
	    	System.out.println("Second port" + m_serverPort);
	    	return true;
	    	
	    }
	    else
	    {
	    	System.err.println("Darn already a user with that name");
	    	m_socket.close();
	    	return false;
	    }
	    
    }

    public String receiveChatMessage() 
    {
    	//Receive a message
    	String received = new String();
    	System.out.println(m_socket.getLocalPort());
    	try {
			received = (String) m_in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	JSONParser parser = new JSONParser();
    	JSONObject message = new JSONObject();
    	try {
			message = (JSONObject) parser.parse(received);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		received = message.get("message").toString();
		return received+"\n";
    	
    }

    @SuppressWarnings("unchecked")
	public void sendChatMessage(String message) 
    {
    	// TODO: 
		// * marshal message if necessary
		// * send a chat message to the server
    	
    	JSONObject obj = new JSONObject();
    	obj.put("name", m_name);
    	//If it starts with a slash look at what command
    	if(message.startsWith("/") == true)
    	{
    		//Split message so we have the command and the message
    		String[] splinter = message.split(" ",3);
    		//System.out.println(splinter[1]);

    		switch(splinter[0])
    		{
	    		case "/tell":
	    			//Tell / whisper to identify it's specifically to you
	    			message = m_name + " whispered:  " + splinter[2];
	    			obj.put("command", "tell");
	    			obj.put("target", splinter[1]);
	    			obj.put("message", message);
	    			break;

	    		case "/list":
	    			obj.put("command", "list");
	    			break;
	    				
	    		case "/leave":
	    			obj.put("command", "disconnect");	
	    			break;
	    		case "/help":
	    			obj.put("command", "help");
	    			if(splinter.length != 1)
	    			{
	    				obj.put("second command", splinter[1]);
	    				System.out.println("was something");
	    			}
	    			else
	    			{
	    				obj.put("second command", "helper");
	    				System.out.println("nothing else");
	    			}
	    			break;
	    		case "/zoidberg":
	    		{
	    			obj.put("command", "zoidberg");
	    			break;
	    		}
	    		case "/join":
	    		{
	    			System.out.println("Want to join server again");
	    			m_serverPort = handPort;
	    			createSocket();
	    			try {
						handshake(m_name);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.out.println("Dun goofed in handshaking at a rejoin");
					}
	    			break;
	    		}
    		}
    	}
    		
    	else{
    		obj.put("command", "broadcast");
    		obj.put("message", message);
    		System.out.println("Broadcast message client");
    		}
    	
    	String msg = obj.toString();
    	//Send message
    	try {
    		System.out.println("Current port " + m_serverPort);
			m_out.writeObject(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }
   
}