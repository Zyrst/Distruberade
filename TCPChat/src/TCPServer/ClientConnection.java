package TCPServer;

import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * 
 * @author brom
 */
public class ClientConnection implements Runnable {
		
	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private ObjectOutputStream m_out;
	private ObjectInputStream m_in;
	private final ServerSocket m_socket;
	private final Server m_server;

	public ClientConnection(String name, InetAddress address, int port, ServerSocket socket, Server server ) 
	{
		m_name 		= name;
		m_address 	= address;
		m_port 		= port;
		m_socket 	= socket;
		m_out 		= null;
		m_in		= null;
		m_server 	= server;
		
	}

	public void acceptClient()
	{
		//New socket for a specific client
		Socket client = null;
		try {
			client = m_socket.accept();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Output stream for client
		try {
			m_out = new ObjectOutputStream(client.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//And input stream
		try {
			m_in = new ObjectInputStream(client.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	//Create a new thread from server which receives messages from client
	//Sends it back to server and do the command contain in it
	@Override
	public void run()
	{
		System.out.print("Starting a new Thread");	
		while(true)
		{
			String received = null;
			try {
				received = (String) m_in.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Create json object from the streamed string
			System.out.println(received);
			JSONParser parser = new JSONParser();
			JSONObject command = new JSONObject();
			try {
				command = (JSONObject) parser.parse(received);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Send it back to main server thread to issue commands
			if(command.equals("disconencted"))
			{
				try {
					m_socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			m_server.serverCommands(command);
			
		}
	}
}