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
	
    private Socket m_socket 			= null;
    private InetAddress m_serverAddress = null;
    private int m_serverPort 			= -1;
    private String m_name				= null;
    private ObjectOutputStream m_out 	= null;
    private ObjectInputStream m_in		= null;

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
		
		createSocket();
		 
    }
    public void createSocket()
    {
    	try{
			m_socket = new Socket(m_serverAddress, m_serverPort);
		    m_in = new ObjectInputStream(m_socket.getInputStream());
		    m_out = new ObjectOutputStream(m_socket.getOutputStream());
    	}
		catch (UnknownHostException e)
		{
			System.err.println("Don't know about host ");
	        System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
    	//Try to send handshake message
		m_out.writeObject(message);
    	String returnDec = null;
    	
    	try {
			returnDec = (String) m_in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	JSONParser parser = new JSONParser();
    	obj = new JSONObject();
    	try {
			obj = (JSONObject) parser.parse(returnDec);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	String decision = obj.get("decision").toString();
    	
	    if(decision == "true")
	    {
	    	String Port = obj.get("port").toString();
	    	m_serverPort = Integer.parseInt(Port);
	    	m_socket.close();
	    	createSocket();
	    	System.out.println("Connection established");
	    	return true;
	    	
	    }
	    else
	    {
	    	System.err.println("Darn already a user with that name");
	    	m_socket.close();
	    	return false;
	    }
	    
    }

    public String receiveChatMessage() {
    	
    	
    	//Receive a message
    	String received = new String();
    	
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
	public void sendChatMessage(String message) {
    	// TODO: 
		// * marshal message if necessary
		// * send a chat message to the server
    	
    	JSONObject obj = new JSONObject();
    /*	byte buf[] = new byte[message.length() + 2];
    		
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
    	}*/
    		
    	//else{}
    		obj.put("command", "broadcast");
    		obj.put("name", m_name);
    		obj.put("message", message);
    		System.out.println("Put stuff in to an object");
    		String msg = obj.toString();
    		
    	//Send message
    	try {
			m_out.writeObject(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    		    		
    	
    	
 
    }
   
}