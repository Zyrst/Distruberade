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
    private PrintWriter m_out 			= null;
    private BufferedReader m_in 		= null;

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

		try{
				m_socket = new Socket(m_serverAddress, m_serverPort);
			    m_out = new PrintWriter(m_socket.getOutputStream(), true);
			    m_in = new BufferedReader( new InputStreamReader(m_socket.getInputStream()));
		}
		catch (UnknownHostException e)
		{
			System.err.println("Don't know about host " + hostName);
            System.exit(1);
		}
		 catch (IOException e) {
	            System.err.println("Couldn't get I/O for the connection to " + hostName);
	            //System.exit(1);
	        } 
    }

    @SuppressWarnings("unchecked")
	public boolean handshake(String name) 
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
    	obj.put("id", 1).toString();
    	obj.put("name", name).toString();
    	
    	m_out.println(obj);

	    byte[] returnBuf = new byte[8];

	    if(returnBuf[0] == 0)
	    {
	    	System.err.println("Darn already a user with that name");
	    	return false;
	    	
	    }
	    else
	    {
	    	System.out.println("Connection established");
	    	return true;
	    }
	    
    }

    public String receiveChatMessage() {
    	byte[] buf = new byte[256];
    	
    	//Receive a message
    	
    	String received = new String(buf);

		received = received.trim();
		return received+"\n";
    	
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
    	
    	
    	Random generator = new Random();
        double failure;	
    	failure = generator.nextDouble();
    	if (failure > TRANSMISSION_FAILURE_RATE)
    	{
    		//Send message
    		    		
    	}
    	else{
    	   	// Message got lost
    		System.err.println("Lost message");
    	    }
 
    }
   
}