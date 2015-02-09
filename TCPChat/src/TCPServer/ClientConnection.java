package TCPServer;

import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ClientConnection {
	
	static double TRANSMISSION_FAILURE_RATE = 0.3;
	
	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private boolean m_ack = false;
	private int triesConnecet = 0;

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public String getName()
	{
		return m_name;
	}
	
	public void setAck(boolean value)
	{
		m_ack = value;
		if(value == true) {triesConnecet = 0;}
	}
	
	public void sendMessage(String message, ServerSocket socket) {
		m_ack = false;
		Random generator = new Random();
    	double failure;
    	
    	for(int i = 0; i <= 5; i++)
    	{
    		failure = generator.nextDouble();
    		triesConnecet += 1;
		   	if (failure > TRANSMISSION_FAILURE_RATE){
		    	// TODO: send a message to this client using socket.
		    	if(m_ack == false)
			   
		    		break;
		   	} else {
		    	// Message got lost
		    	System.err.println("Message got lost");
		   	}
    	}
	}

	public int triedConnect()
	{
		return triesConnecet;
	}
	
	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}
	
}