package TCPServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
	
	public void sendMessage(String message, DatagramSocket socket) {
		m_ack = false;
		Random generator = new Random();
    	double failure;
    	
    	for(int i = 0; i <= 5; i++)
    	{
    		failure = generator.nextDouble();
    		triesConnecet += 1;
		   	if (failure > TRANSMISSION_FAILURE_RATE){
		    	// TODO: send a message to this client using socket.
		    	byte[] buf = new byte[message.length()];
		    	buf = message.getBytes();
		    	DatagramPacket packet = new DatagramPacket(buf, buf.length, 
		    											m_address, m_port);
		    	if(m_ack == false)
			    	try {
			    		
						socket.send(packet);
						System.out.println(triesConnecet);
					} catch (IOException e) {
						System.err.println("Not able to send message");
					}
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