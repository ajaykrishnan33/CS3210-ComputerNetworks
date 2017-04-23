import java.util.*;
import java.io.*;
import java.net.*;

class Receiver{

	int portno;
	int len; // input parameter
	int window_size = 255; // input parameter
	int next_needed = 0;
	double RANDOM_DROP_PROB = 0.001;
	int MAX_PACKETS = 25;
	boolean debug = false;

	long startTime;

	public void run(){
		DatagramSocket socket;
		try{
			socket = new DatagramSocket(this.portno);
			byte[] buf = new byte[this.len];

			int x;
			int y;
			byte[] ackbuf = new byte[4];
			byte z;
			DatagramPacket ackpacket;

			DatagramPacket packet;

			int ack_count = 0;

			long diff, ms, us;
			String rTime;

			packet = new DatagramPacket(buf, buf.length);
			while(true){
				if(ack_count==MAX_PACKETS){
					// System.out.println("Ack count: " + ack_count);
					return;
				}
				try{
					socket.receive(packet);

					z = packet.getData()[0];
					if(z<0){
						y = 256 + (int)z;
					}
					else
						y = z;

					if(this.debug){
						// compute time
						diff = System.nanoTime() - this.startTime; // ns
						ms = diff/1000000; // ms
						us = (diff%1000000)/1000; // microseconds

						rTime = ms+":"+us;
						System.out.print("Seq #:" + y + " Time Received: "+rTime);
					}

					if(Math.random()<=RANDOM_DROP_PROB){
						if(this.debug)
							System.out.println(" Packet dropped: true");
						continue;
					}
					if(this.debug)
						System.out.println(" Packet dropped: false");

					x = (y+1)%(this.window_size+1);

					if(y == this.next_needed){
						ack_count++;
						this.next_needed = (this.next_needed+1)%(this.window_size+1);
					}
					else
						x = this.next_needed;

					ackbuf[0] = (byte)x;

					ackpacket = new DatagramPacket(ackbuf, ackbuf.length, packet.getAddress(), packet.getPort());
				
					socket.send(ackpacket);
				}
				catch(IOException e){
					System.err.println("Packet receive error");
				}
			}
		}
		catch(SocketException e){
			System.err.println("Socket error");
			System.exit(1);
		}

	}

	public static void main(String[] args) throws Exception{
		
		Receiver r = new Receiver();

		for(int i=0;i<args.length;i++){
			if(args[i].equals("-d")){
				r.debug = true;
				continue;
			}
			if(args[i].charAt(0)!='-'){
				System.err.println("Error in arguments");
				return;
			}
			switch(args[i].charAt(1)){
				case 'p':
					r.portno = Integer.parseInt(args[i+1]);
					break;
				case 'l':
					r.len = Integer.parseInt(args[i+1]);
					break;
				case 'n':
					r.MAX_PACKETS = Integer.parseInt(args[i+1]);
					break;
				case 'e':
					r.RANDOM_DROP_PROB = Float.parseFloat(args[i+1]);
					break;
				default:
					System.err.println("Invalid option "+args[i]);
					return;
			}
			i++;
		}

		r.startTime = System.nanoTime();

		r.run();
	}
}