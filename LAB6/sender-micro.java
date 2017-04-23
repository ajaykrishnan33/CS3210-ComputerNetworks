import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;

class GlobalVariables{

	public static boolean debug = false;

	public static DatagramSocket socket;
	public static InetAddress address;
	public static int portno;
	public static int PACKET_LENGTH; // input parameter
	public static int PACKET_GEN_RATE = 10; // input parameter

	public static int last_sent = -1;
	public static int first_not_acknowledged = 0;
	public static int WINDOW_SIZE = 255;	// input parameter
	public static int window_start = 0;
	public static int window_end = WINDOW_SIZE-1;

	public static int base = 256;

	public static int unacked_packet_count = 0;

	public static int MAX_BUF_SIZE = WINDOW_SIZE + 10; 
	public static int MAX_PACKETS = 25;

	public static double avgRTT;
	public static Queue<Packet> last10 = new LinkedList<Packet>();

	public static int sent_count = 0;
	public static int total_transmission_count = 0;

	public static ReentrantLock lock = new ReentrantLock(true); // fair lock

	public static ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

	public static Queue<Packet> packetQ = new LinkedList<Packet>();

	public static ReentrantLock timeoutLock = new ReentrantLock(true);

	public static long startTime;

	public static void exit(){
		System.out.println("PACKET_GEN_RATE: " + PACKET_GEN_RATE);
		System.out.println("PACKET_LENGTH: " + PACKET_LENGTH);
		System.out.println("Retransmission ratio: " +(double)total_transmission_count/(double)sent_count);
		System.out.println("Average RTT (in ms): " + avgRTT);
		System.exit(0);
	}
}

class Packet{
	public long TIMEOUT = 100*1000; // microsecond
	byte[] buf;
	// Timer timer;
	ScheduledFuture<?> timer;
	int retrans_count = -1;
	int seq;

	double sent;
	double acked;

	double RTT;

	String generationTime;
	String sentTime;
	String ackedTime;

	public Packet(byte[] buf){
		
		this.generationTime = this.getTimeString(System.nanoTime());

		this.buf = buf;

		if(buf[0]<0)
			this.seq = 256 + (int)buf[0];
		else
			this.seq = buf[0];
	}

	public String getTimeString(long ctime){
		long diff = ctime - GlobalVariables.startTime; // ns
		long ms = diff/1000000; // ms
		long us = (diff%1000000)/1000; // microseconds

		return ms+":"+us;
	}

	public void setTimer(){

		if(GlobalVariables.sent_count>10){
			this.TIMEOUT = (long)(2*GlobalVariables.avgRTT * 1000);
			System.out.println("Timeout "+this.TIMEOUT + " "+this.seq);
		}

		this.timer = GlobalVariables.timeoutScheduler.schedule(new TimeOutTask(this), TIMEOUT, TimeUnit.MICROSECONDS);
	}

	public void setSentTime(long val){
		this.sent = val;
		this.sentTime = this.getTimeString(val);
	}

	public void setAckedTime(long val){
		this.acked = val;
		this.ackedTime = this.getTimeString(val);
		this.RTT = (this.acked - this.sent)/1000000;
	}

	public void printDebug(){
		System.out.println("Seq #: "+this.seq+" Time Generated: " + this.generationTime + " RTT: "+this.RTT + " Number of Attempts: " + (this.retrans_count+1));
	}

}

// periodic task, continuously runs in the background and fills the buffer.
class PacketGenerator extends TimerTask{

	int gen_count = 0;
	public Packet generatePacket(int seq, int len){ // Check packet length
		
		byte[] buf = new byte[len];
		for(int i=1;i<len;i++){
			buf[i] = (byte)gen_count;
		}
		gen_count = (gen_count+1)%10;

		buf[0] = (byte)seq;

		return new Packet(buf);
	}

	public void addNewPacket(){
		GlobalVariables.lock.lock();
		int seq;
		int base = GlobalVariables.base;
		try{
			// get last sequence number in the queue;
			seq = ((LinkedList<Packet>)GlobalVariables.packetQ).getLast().seq;
		}
		catch(NoSuchElementException e){
			seq = (base + GlobalVariables.window_start - 1)%base;
		}
		if(GlobalVariables.packetQ.size()<GlobalVariables.MAX_BUF_SIZE){
			seq = (seq+1)%base;
			GlobalVariables.packetQ.add(this.generatePacket(seq, GlobalVariables.PACKET_LENGTH));
		}
		GlobalVariables.lock.unlock();
	}

	public void run(){
		this.addNewPacket();
	}

}

class TimeOutTask implements Runnable{

	public Packet packet;

	public TimeOutTask(Packet p){
		this.packet = p;
	}

	// timer that gets triggered will be the first element of the queue
	// because in the queue, elements are added after the timer has been started
	// so they will fire in the order in which they are pushed into the queue
	public void run(){
		
		// to ensure that multiple timeouts do not occur simultaneously
		boolean acquired = GlobalVariables.timeoutLock.tryLock();
		if(!acquired)
			return;

		GlobalVariables.lock.lock();

		// cancel all the old timers
		for(Packet p:GlobalVariables.packetQ){
			if(p.timer!=null){
				p.timer.cancel(false);
			}
			else
				break;
		}

		// Shift the window to restart transmission from the first unacknowledged packet
		GlobalVariables.window_start = GlobalVariables.first_not_acknowledged;

		int base = GlobalVariables.base;

		// no need to remove anything from packetQ since it only contains the unacknowledged packets
		int new_end = (GlobalVariables.window_start + GlobalVariables.WINDOW_SIZE-1)%base;
		GlobalVariables.window_end = new_end;

		GlobalVariables.last_sent = (base + GlobalVariables.window_start - 1)%base;

		GlobalVariables.unacked_packet_count = 0; // reset count to 0

		GlobalVariables.lock.unlock(); // release lock

		GlobalVariables.timeoutLock.unlock();

		// transmission will restart automatically now
	}
}

class DataSenderThread extends Thread{

	public Packet getNextPacket(){

		GlobalVariables.lock.lock();
		Packet ret = null;
		int val = (GlobalVariables.last_sent+1)%(GlobalVariables.base);

		for(Packet p: GlobalVariables.packetQ){
			if(p.seq==val){
				ret = p;
				break;
			}
		}

		if(ret!=null){
			GlobalVariables.last_sent = val;
		}

		GlobalVariables.lock.unlock();

		return ret;
	}

	public void run() {

		while(true){

			GlobalVariables.lock.lock();
			if(GlobalVariables.last_sent==GlobalVariables.window_end || GlobalVariables.unacked_packet_count>=GlobalVariables.WINDOW_SIZE){
				GlobalVariables.lock.unlock();
				continue;
			}
			
			Packet p = this.getNextPacket();

			if(p==null){
				GlobalVariables.lock.unlock();
				continue;
			}

			byte[] buf = p.buf;

			DatagramPacket packet = new DatagramPacket(buf, buf.length, GlobalVariables.address, GlobalVariables.portno);
			try{

				if(p.retrans_count>=5){
					System.err.println("Number of retransmissions exceeded 5 for packet with sequence number: "+p.seq + "--------------------");
					// GlobalVariables.exit();
				}

				GlobalVariables.unacked_packet_count++;
				p.setSentTime(System.nanoTime());

				GlobalVariables.socket.send(packet);

				GlobalVariables.total_transmission_count++;

				p.retrans_count++;
				// start timeout timer
				p.setTimer();

			}
			catch(Exception e){
				System.err.println("Failure in sending packet");
			}

			GlobalVariables.lock.unlock();

		}

	}

}

class AckReceiverThread extends Thread{

	public void computeRTT(){
		double x = 0;
		int count = 0;
		for(Packet p: GlobalVariables.packetQ){
			if(p.acked>=0.0001){
				x += (p.acked-p.sent);
				count++;
			}
			else
				break;
		}
		x = x/1000000;
		GlobalVariables.avgRTT = (GlobalVariables.avgRTT*(GlobalVariables.sent_count-count) + x)/GlobalVariables.sent_count;
	}

	public void cancelAllPreviousTimersAndPackets(long currTime){
		GlobalVariables.lock.lock(); // acquire lock since this will read from and write to global vars.
		int i = GlobalVariables.window_start;
		int base = GlobalVariables.base;
		int count = 0;
		if(i==GlobalVariables.first_not_acknowledged){ // sending first packet failed
			GlobalVariables.lock.unlock();
			return; // simply return, wait for timeout to occur and then start retransmission
		}

		int last_acked = (base + GlobalVariables.first_not_acknowledged - 1)%base;
		for(Packet p:GlobalVariables.packetQ){
			if(p.timer!=null){
				count++;
				p.timer.cancel(false);
				
				GlobalVariables.unacked_packet_count--; // packet assumed acknowledged

				GlobalVariables.sent_count++;
				p.setAckedTime(currTime);

				if(GlobalVariables.debug){
					p.printDebug();
				}

				if(i==last_acked){
					this.computeRTT();
					break;
				}
				i = (i + 1)%base;
			}
			else
				break;
		}

		while(count>0){
			GlobalVariables.packetQ.poll();
			count--;
		}
		GlobalVariables.lock.unlock();
	}
			
	public void run(){
		byte[] buf = new byte[10];
		String received;
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while(true){
			try{
				GlobalVariables.socket.receive(packet);

				int val;
				byte z = packet.getData()[0];
				if(z<0){
					val = 256 + (int)z;
				}
				else{
					val = z;
				}

				long currTime = System.nanoTime();

				GlobalVariables.lock.lock(); //acquire lock before making changes

				if(GlobalVariables.first_not_acknowledged==val){
					System.out.println("\nReceived : "+ val+"\n");
				}

				GlobalVariables.first_not_acknowledged = val;
				this.cancelAllPreviousTimersAndPackets(currTime);
				GlobalVariables.window_start = val;
				int base = GlobalVariables.base;
				int new_end = (GlobalVariables.window_start + GlobalVariables.WINDOW_SIZE-1)%base;
				GlobalVariables.window_end = new_end;

				if(GlobalVariables.sent_count==GlobalVariables.MAX_PACKETS){
					System.out.println("MAX_PACKETS sent. Terminating...");
					GlobalVariables.exit();
				}

				GlobalVariables.lock.unlock();// release lock
			}
			catch(Exception e){
				System.err.println("Packet receive failed.");
			}
		}
	}

}

public class Sender{

	public void init() throws Exception{
		new AckReceiverThread().start();

		Timer t = new Timer();
		PacketGenerator pcktgen = new PacketGenerator();
		t.schedule(pcktgen, 0, (long)1000/GlobalVariables.PACKET_GEN_RATE);

		new DataSenderThread().start();
	}

	public static void main(String[] args) throws Exception{

		for(int i=0;i<args.length;i++){
			if(args[i].equals("-d")){
				GlobalVariables.debug = true;
				continue;
			}
			if(args[i].charAt(0)!='-'){
				System.err.println("Error in arguments");
				return;
			}
			switch(args[i].charAt(1)){
				case 'l':
					GlobalVariables.PACKET_LENGTH = Integer.parseInt(args[i+1]);
					break;
				case 's':
					GlobalVariables.address = InetAddress.getByName(args[i+1]);
					break;
				case 'p':
					GlobalVariables.portno = Integer.parseInt(args[i+1]);
					break;
				case 'n':
					GlobalVariables.MAX_PACKETS = Integer.parseInt(args[i+1]);
					break;
				case 'w':
					GlobalVariables.WINDOW_SIZE = Integer.parseInt(args[i+1]);
					break;
				case 'b':
					GlobalVariables.MAX_BUF_SIZE = Integer.parseInt(args[i+1]);
					break;
				case 'r':
					GlobalVariables.PACKET_GEN_RATE = Integer.parseInt(args[i+1]);
					break;
				default:
					System.err.println("Invalid option "+args[i]);
					return;
			}
			i++;
		}

		GlobalVariables.socket = new DatagramSocket();

		GlobalVariables.startTime = System.nanoTime();

		new Sender().init();
	}
}