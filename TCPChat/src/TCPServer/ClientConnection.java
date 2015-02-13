package TCPServer;

import java.io.*;
import java.net.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private int m_port;
	private ObjectOutputStream m_out;
	private ObjectInputStream m_in;
	private final ServerSocket m_socket;
	private final Server m_server;
	private  Socket m_clientSocket;
	private boolean m_alive;
	private final Lock m_mutex = new ReentrantLock(true);

	public ClientConnection(String name, InetAddress address, int port, ServerSocket socket, Server server ) 
	{
		m_name 		= name;
		m_address 	= address;
		m_port 		= port;
		m_socket 	= socket;
		m_out 		= null;
		m_in		= null;
		m_server 	= server;
		m_alive 	= true;
		
	}

	public void acceptClient()
	{
		//New socket for a specific client
		m_clientSocket = null;
		try {
			m_clientSocket = m_socket.accept();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Output stream for client
		try {
			m_out = new ObjectOutputStream(m_clientSocket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//And input stream
		try {
			m_in = new ObjectInputStream(m_clientSocket.getInputStream());
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
		m_mutex.lock();
		String msg = message.toString();
		try {
			m_out.writeObject(msg);
		} catch (IOException e) {
			System.err.println("Unable to write object to client");
			e.printStackTrace();
		}
		m_mutex.unlock();
	}
	
	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}
	
	@SuppressWarnings("unchecked")
	public boolean isAlive()
	{
		JSONObject obj = new JSONObject();
		System.out.println("See if connections are alive");
		obj.put("ping", "are you alive");
	
		m_mutex.lock();
		String msg = obj.toString();
		try {
			m_out.writeObject(msg);
			m_alive = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			m_alive = false;
		}
		m_mutex.unlock();
		return m_alive;
	}
	
	//Create a new thread from server which receives messages from client
	//Sends it back to server and do the command contain in it
	@Override
	public void run()
	{
		boolean connected = true;
		while(connected == true)
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
			JSONParser parser = new JSONParser();
			JSONObject command = new JSONObject();
			try {
				command = (JSONObject) parser.parse(received);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(!command.containsKey("command"))
				command.put("command", "join");
			//Send it back to main server thread to issue commands
			if(command.equals("disconencted"))
			{
				try {
					m_socket.close();
					m_clientSocket.close();
					connected = false;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			m_server.serverCommands(command);
			
		}
	}
}