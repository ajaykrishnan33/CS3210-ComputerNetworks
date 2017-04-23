import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.net.*;

// periodic task, continuously runs in the background and fills the buffer.
class PacketGenerator extends TimerTask{

	DataSenderThread dst;

	public PacketGenerator(DataSenderThread dst){
		this.dst = dst;
	}

	int gen_count = 0;
	public byte[] generatePacket(int seq, int len){ // Check packet length
		
		String x = "";
		for(int i=1;i<len/2;i++){
			x+=gen_count;
		}
		gen_count = (gen_count+1)%10;

		x = (char)(seq) + x;

		byte[] buf = x.getBytes();
		return buf;
	}

	public void addNewPacket(){
		this.dst.lock.lock();
		int seq;
		int base = this.dst.window_size+1;
		try{
			// get last sequence number in the queue;
			seq = (int)new String(((LinkedList<byte[]>)this.dst.packetQ).getLast()).charAt(0);		
			
		}
		catch(NoSuchElementException e){
			seq = (base + this.dst.window_start - 1)%base;
		}
		if(this.dst.packetQ.size()<this.dst.MAX_BUF_SIZE){
			seq = (seq+1)%base;
			this.dst.packetQ.add(this.generatePacket(seq, this.dst.len));
		}
		this.dst.lock.unlock();
	}

	public void run(){
		this.addNewPacket();
	}

}

class DataSenderThread extends Thread{

	DatagramSocket socket;
	InetAddress address;
	int portno;
	int len;

	int last_sent=-1;
	int first_not_acknowledged=0;
	int window_size=255;
	int window_start = 0;
	int window_end = this.window_size-1;
	int MAX_BUF_SIZE = this.window_size + 10; 

	ReentrantLock lock = new ReentrantLock(true); // fair lock
	Queue<Timer> timeoutQ = new LinkedList<Timer>();
	Queue<byte[]> packetQ = new LinkedList<byte[]>();
	
	public DataSenderThread(DatagramSocket socket, InetAddress address, int portno, int len){
		this.socket = socket;
		this.address = address;
		this.portno = portno;
		this.len = len;
	}

	class TimeOutTask extends TimerTask{
		public long TIMEOUT = 2*1000;

		public int seq;

		public TimeOutTask(int seq){
			this.seq = seq;
		}

		// timer that gets triggered will be the first element of the queue
		// because in the queue, elements are added after the timer has been started
		// so they will fire in the order in which they are pushed into the queue
		public void run(){
			
			System.out.println("Timeout: "+this.seq+" "+this.scheduledExecutionTime());

			DataSenderThread.this.lock.lock();

			// remove all the old timers
			Timer t;
			while(DataSenderThread.this.timeoutQ.peek()!=null){
				t = DataSenderThread.this.timeoutQ.poll();
				t.cancel();
			}

			// Shift the window to restart transmission from the first unacknowledged packet
			DataSenderThread.this.window_start = DataSenderThread.this.first_not_acknowledged;

			int base = DataSenderThread.this.window_size + 1;

			// no need to remove anything packetQ since it only contains the unacknowledged packets
			int new_end = (DataSenderThread.this.window_start + DataSenderThread.this.window_size-1)%base;
			DataSenderThread.this.window_end = new_end;

			DataSenderThread.this.last_sent = (base + DataSenderThread.this.window_start - 1)%base;

			DataSenderThread.this.lock.unlock(); // release lock

			// transmission will restart automatically now

		}
	}

	public void addNewTimer(int seq){
		Timer t = new Timer();
		TimeOutTask tot = new TimeOutTask(seq);

		try{
			Thread.sleep(10);
		}
		catch(Exception e){}
		
		t.schedule(tot, tot.TIMEOUT);

		this.lock.lock();
		this.timeoutQ.add(t);
		this.lock.unlock();
	}

	public byte[] getNextPacket(){

		this.lock.lock();
		byte[] ret = null;
		int val = (this.last_sent+1)%(this.window_size+1);

		for(byte[] buf: this.packetQ){
			if((int)(new String(buf).charAt(0))==val){
				ret = buf;
				break;
			}
		}

		if(ret!=null){
			this.last_sent = val;
		}

		this.lock.unlock();

		return ret;
	}

	public void run() {

		while(true){

			this.lock.lock();
			
			if(this.last_sent==this.window_end){
				this.lock.unlock();
				continue;
			}
			
			byte[] buf = this.getNextPacket();
			
			if(buf==null){
				this.lock.unlock();
				continue;
			}

			DatagramPacket packet = new DatagramPacket(buf, buf.length, this.address, this.portno);
			try{
				this.socket.send(packet);
				// start timeout timer
				this.addNewTimer((int)new String(buf).charAt(0));

				System.out.println("Sent: "+(int)new String(buf).charAt(0));
			}
			catch(Exception e){
				System.err.println("Failure in sending packet");
			}

			this.lock.unlock();

		}

	}

}

class AckReceiverThread extends Thread{

	DatagramSocket socket;	
	InetAddress address;
	int portno;
	int len;
	DataSenderThread dst;

	public AckReceiverThread(DataSenderThread dst, DatagramSocket socket, InetAddress address, int portno, int len){
		this.dst = dst;
		this.socket = socket;
		this.address = address;
		this.portno = portno;
		this.len = len;
	}

	public void cancelAllPreviousTimersAndPackets(){
		this.dst.lock.lock(); // acquire lock since this will read from and write to global vars.
		int i = this.dst.window_start;
		int base = this.dst.window_size + 1;
		int count = 0;
		if(i==this.dst.first_not_acknowledged){ // sending first packet failed
			this.dst.lock.unlock();
			return; // simply return, wait for timeout to occur and then start retransmission
		}

		int last_acked = (base + this.dst.first_not_acknowledged - 1)%base;
		for(Timer t:this.dst.timeoutQ){
			count++;
			t.cancel();
			if(i==last_acked){
				break;
			}
			i = (i + 1)%base;
		}

		while(count>0){
			this.dst.timeoutQ.remove();
			this.dst.packetQ.poll();
			count--;
		}
		this.dst.lock.unlock();
	}
			
	public void run(){
		byte[] buf = new byte[10];
		String received;
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while(true){
			try{
				socket.receive(packet);
				received = new String(packet.getData(), 0, packet.getLength());
				int val = (int)received.charAt(0);

				this.dst.lock.lock(); //acquire lock before making changes
			
				// probably need to check if val > first_not_acknowleged		
				this.dst.first_not_acknowledged = val;
				this.cancelAllPreviousTimersAndPackets();
				this.dst.window_start = val;
				int base = this.dst.window_size + 1;
				int new_end = (this.dst.window_start + this.dst.window_size-1)%base;
				this.dst.window_end = new_end;

				this.dst.lock.unlock();// release lock

				System.out.println("Ack received: "+val);
			}
			catch(Exception e){
				System.err.println("Packet receive failed.");
			}
		}
	}

}

public class Sender{

	DatagramSocket socket;	
	InetAddress address;
	int portno;
	int len;

	public Sender(String port_number, String len) throws Exception{
		System.out.println("started...");
		this.address = InetAddress.getByName("localhost");
		this.portno = Integer.parseInt(port_number);
		this.len = Integer.parseInt(len);
		this.socket = new DatagramSocket();
	}

	public void init() throws Exception{
		DataSenderThread dst = new DataSenderThread(this.socket, this.address, this.portno, this.len);
		new AckReceiverThread(dst, this.socket, this.address, this.portno, this.len).start();

		Timer t = new Timer();
		PacketGenerator pcktgen = new PacketGenerator(dst);
		Thread.sleep(10);
		t.schedule(pcktgen, 0, 300);
		Thread.sleep(1000);
		dst.start();
	}

	public static void main(String[] args) throws Exception{

		if(args.length<1){
			return;
		}

		new Sender(args[0], "100").init();
	}
}