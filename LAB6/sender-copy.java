import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.net.*;


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

	Timer windowTimer;

	ReentrantLock lock = new ReentrantLock();
	Queue<Timer> timeoutQ = new LinkedList<Timer>();
	Queue<byte[]> packetQ = new LinkedList<byte[]>();
	
	ReentrantLock running = new ReentrantLock();

	public DataSenderThread(DatagramSocket socket, InetAddress address, int portno, int len){
		this.socket = socket;
		this.address = address;
		this.portno = portno;
		this.len = len;

		int i;
		for(i=0;i<this.window_size;i++){
			this.packetQ.add(this.generatePacket(i, this.len));
		}
	}

	int gen_count = 0;
	public byte[] generatePacket(int seq, int len){ // Check packet length
		this.lock.lock();
		
		String x = "";
		for(int i=1;i<len/2;i++){
			x+=gen_count;
		}
		gen_count = (gen_count+1)%10;

		x = (char)(seq) + x;
		
		this.lock.unlock();

		byte[] buf = x.getBytes();
		return buf;
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
		else{
			this.lock.unlock();
			System.err.println("Could not find sequence number");
			System.exit(1);
		}

		this.lock.unlock();

		return ret;
	}

	public void addNewPackets(int new_end){
		this.lock.lock();
		int i = this.window_end;
		// get last sequence number in the queue;
		try{
			int seq = (int)new String(((LinkedList<byte[]>)this.packetQ).getLast()).charAt(0);		
			while(i!=new_end){
				seq = (seq+1)%(this.window_size+1);
				this.packetQ.add(this.generatePacket(seq, this.len));
				i = (i+1)%(this.window_size+1);
			}
			this.lock.unlock();
		}
		catch(NoSuchElementException e){
			this.lock.unlock();
			System.err.println("Empty packet list");
			System.exit(1);
		}
	}

	class TimeOutTask extends TimerTask{
		public long TIMEOUT = 2*1000;

		// timer that gets triggered will be the first element of the queue
		// because in the queue, elements are added after the timer has been started
		// so they will fire in the order in which they are pushed into the queue
		public void run(){
			// first cancel all the other timers
			DataSenderThread.this.windowTimer.cancel();

			DataSenderThread.this.running.lock(); // wait for last msg task to finish executing
			
			DataSenderThread.this.lock.lock();

			System.out.println("Timeout: "+DataSenderThread.this.window_start);

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
			// but need to add new packets to fill the window
			int new_end = (DataSenderThread.this.window_start + DataSenderThread.this.window_size-1)%base;
			DataSenderThread.this.addNewPackets(new_end);

			DataSenderThread.this.window_end = new_end;

			DataSenderThread.this.last_sent = (base + DataSenderThread.this.window_start - 1)%base;

			DataSenderThread.this.lock.unlock(); // release lock

			DataSenderThread.this.running.unlock();

			// restart transmission
			DataSenderThread.this.startTransmission();

		}
	}

	class MessageTask extends TimerTask{		

		public void addNewTimer(){
			Timer t = new Timer();
			TimeOutTask tot = new TimeOutTask();

			t.schedule(tot, tot.TIMEOUT);

			DataSenderThread.this.lock.lock();
			DataSenderThread.this.timeoutQ.add(t);
			DataSenderThread.this.lock.unlock();
		}

		public void run() {

			// try to acquire lock. If failed, then return.
			boolean acquired = DataSenderThread.this.running.tryLock();
			if(!acquired)
				return;

			DataSenderThread.this.lock.lock();
			
			if(DataSenderThread.this.last_sent==DataSenderThread.this.window_end){
				DataSenderThread.this.windowTimer.cancel();
				DataSenderThread.this.lock.unlock();
				return;
			}
			
			byte[] buf = DataSenderThread.this.getNextPacket();
			
			DatagramPacket packet = new DatagramPacket(buf, buf.length, DataSenderThread.this.address, DataSenderThread.this.portno);
			try{
				DataSenderThread.this.socket.send(packet);
				// start timeout timer
				this.addNewTimer();

				System.out.println("Sent: "+(int)new String(buf).charAt(0));
			}
			catch(Exception e){
				System.err.println("Failure in sending packet");
			}

			DataSenderThread.this.lock.unlock();

			DataSenderThread.this.running.unlock();

		}

	}

	public void startTransmission(){
		this.windowTimer = new Timer();

		MessageTask msgtask = new MessageTask();

		this.windowTimer.scheduleAtFixedRate(msgtask, 0, 1*1000);		
	}

	public void run(){
		this.startTransmission();
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
			this.dst.packetQ.remove();
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
				this.dst.first_not_acknowledged = val;
				this.cancelAllPreviousTimersAndPackets();
				this.dst.window_start = val;
				int base = this.dst.window_size + 1;
				int new_end = (this.dst.window_start + this.dst.window_size-1)%base;
				// add new packets to queue
				this.dst.addNewPackets(new_end);

				this.dst.window_end = new_end;
				this.dst.lock.unlock();				

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

	public void init(){
		DataSenderThread dst = new DataSenderThread(this.socket, this.address, this.portno, this.len);
		new AckReceiverThread(dst, this.socket, this.address, this.portno, this.len).start();
		dst.start();
	}

	public static void main(String[] args) throws Exception{

		if(args.length<1){
			return;
		}

		new Sender(args[0], "100").init();
	}
}