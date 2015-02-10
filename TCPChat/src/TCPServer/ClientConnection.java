package TCPServer;

import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;

/**
 * 
 * @author brom
 */
public class ClientConnection {
		
	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private final ObjectOutputStream m_out;

	public ClientConnection(String name, InetAddress address, int port, ObjectOutputStream stream) {
		m_name = name;
		m_address = address;
		m_port = port;
		m_out = stream;
	}

	public String getName()
	{
		return m_name;
	}
	
	public void sendMessage(JSONObject message) {
		// TODO: send a message to this client using socket
				
		String msg = message.toString();
		try {
			m_out.writeObject(msg);
		} catch (IOException e) {
			System.err.println("Unable to write object to client");
			e.printStackTrace();
		}
		    		
		   	
	}
	
	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}
	
}