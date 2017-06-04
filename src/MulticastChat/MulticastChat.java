package MulticastChat;

import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

public class MulticastChat implements Runnable, WindowListener, ActionListener
{	
	protected InetAddress group;
	protected int port;
	protected Frame frame;
	protected TextField input;
	protected TextArea output;
	protected Thread listener;
	protected MulticastSocket socket;
	protected DatagramPacket outgoing, incoming;

	public MulticastChat(InetAddress group, int port){
		this.group = group;
		this.port = port;
		initAWT();
	}
	protected void initAWT(){
		frame = new Frame("멀티캐스트 채팅 [호스트 : "+ group.getHostAddress() + " "+port+" ]");
		frame.addWindowListener(this);
		output = new TextArea();
		output.setEditable(false);
		input = new TextField();
		input.addActionListener(this);
		frame.setLayout(new BorderLayout());
		frame.add(output,"Center");
		frame.add(input,"South");
		frame.pack();
	}

	public synchronized void start() throws IOException{
		if(listener == null)
		{
			initNet();
			listener = new Thread(this);
			listener.start();	//스레드 시작(run()메소드 실행);
			frame.setVisible(true);
		}
	}

	protected void initNet() throws IOException{
		socket = new MulticastSocket(port);
		socket.setTimeToLive(1);
		socket.joinGroup(group);
		outgoing = new DatagramPacket(new byte[1], 1,group,port);
		incoming = new DatagramPacket(new byte[65508], 65508);
	}

	public synchronized void stop() throws IOException{
		frame.setVisible(false);
		if(listener != null){
			listener.interrupt();
			listener = null;
			try{
				socket.leaveGroup(group);
			}finally{
				socket.close();
				System.exit(0);
			}
		}
	}

	public void windowOpened(WindowEvent we){
		input.requestFocus();
	}
	public void windowClosing(WindowEvent we){
		try{
			stop();
		}catch(IOException e){
			System.out.println(e);
		}
	}

	public void windowClosed(WindowEvent we){}
	public void windowIconified(WindowEvent we){}
	public void windowDeiconified(WindowEvent we){}
	public void windowActivated(WindowEvent we){}
	public void windowDeactivated(WindowEvent we){}

	public void actionPerformed(ActionEvent ae){
		try{
			byte[] utf = ae.getActionCommand().getBytes("UTF8");
			outgoing.setData(utf);
			outgoing.setLength(utf.length);
			socket.send(outgoing);
			input.setText("");
		}catch(IOException e){
			System.out.println(e);
			handleIOException(e);
		}
	}
	protected synchronized void handleIOException(IOException e){
		try{
			stop();
		}catch(IOException ie){
			System.out.println(ie);
		}
	}
	public void run(){
		try{
			while(!Thread.interrupted()){
				incoming.setLength(incoming.getData().length);
				socket.receive(incoming);
				String message = new String(incoming.getData(),0,incoming.getLength(),"UTF8");
				output.append(message + "\n");
			}
		}catch(IOException e){
			handleIOException(e);
		}
	}

	public static void main(String args[]) throws IOException{
		if((args.length != 1) || (args[0].indexOf(":") <0))
			throw new IllegalArgumentException("잘못된 멀티캐스트 주소입니다");
		int idx = args[0].indexOf(":");
		InetAddress group = InetAddress.getByName(args[0].substring(0, idx));
		int port = Integer.parseInt(args[0].substring(idx+1));
		MulticastChat chat = new MulticastChat(group, port);
		chat.start();

	}
}

