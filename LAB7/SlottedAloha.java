import java.util.*;
import java.io.*;

class Packet{
	int generated_slot;
	int transmitted_slot;

	public Packet(int slot){
		this.generated_slot = slot;
	}
}

class User{
	int userid;
	int msg_count = 0;

	Queue<Packet> msgQ = new LinkedList<Packet>();

	int collision_window = 2;
	int back_off = 0;
	int retrans_count = -1;
	double prob;

	public User(int userid, int cw, double prob){
		this.userid = userid;
		if(cw>2)
			this.collision_window = cw;
		this.prob = prob;
	}

	public void setBackOff(){		
		this.back_off = (int)(Math.random()*this.collision_window);
		this.collision_window = Math.min(256, this.collision_window*2);
	}

	public void decrementBackOff(){
		if(this.back_off>0)
			this.back_off--;
	}

	public void addMsg(int slot){
		if(this.msg_count<2 && Math.random()<this.prob){
			this.msg_count++;
			this.msgQ.add(new Packet(slot));
		}
	}

	public void dropPacket(){
		this.msgQ.remove();
		this.retrans_count = -1;
		SlottedAloha.drop_count++;
	}

	public Packet removeMsg(int slot){
		this.msg_count--;
		Packet p = this.msgQ.remove();
		p.transmitted_slot = slot;		
		this.collision_window = Math.max(2, (int)(this.collision_window*0.75));
		this.retrans_count = -1;
		return p;
	}
}

class SlottedAloha{

	public static int drop_count=0;

	public static void main(String args[]) throws Exception{

		int usercount = 0;
		int initial_collision_window = 2;
		double PACKET_GEN_PROB = 0;
		int MAX_PACKETS = 0;
		User[] users;
		int i;
		int maxRetries = 10;

		boolean machineReadable = false;

		for(i=0;i<args.length;i++){
			if(args[i].charAt(0)!='-'){
				System.out.println("Error. Incorrect option specification.");
				return;
			}
			switch(args[i].charAt(1)){
				case 'N':
					usercount = Integer.parseInt(args[i+1]);
					break;
				case 'W':
					initial_collision_window = Integer.parseInt(args[i+1]);
					break;
				case 'p':
					PACKET_GEN_PROB = Double.parseDouble(args[i+1]);
					break;
				case 'M':
					MAX_PACKETS = Integer.parseInt(args[i+1]);
					break;
				case 'A':
					machineReadable = true;
					i--;
					break;
				case 'R':
					maxRetries = Integer.parseInt(args[i+1]);
					break;
				default:
					System.out.println("Unknown option " + args[i]);
					return;
			}
			i++;
		}
		if(usercount==0||MAX_PACKETS==0||PACKET_GEN_PROB==0){
			System.out.println("All options not specified");
			return;
		}

		users = new User[usercount];
		for(i=0;i<usercount;i++){
			users[i] = new User(i, initial_collision_window, PACKET_GEN_PROB);
		}

		int total_sent_count = 0;
		int slot_sent_count = 0;

		int slot_number = 1;
		double throughput = 0;
		double avgdelay = 0;

		User u;

		Queue<User> attempters = new LinkedList<User>();

outer:	while(total_sent_count<MAX_PACKETS){
			slot_sent_count = 0;
			for(i=0;i<usercount;i++){
				u = users[i];
				u.addMsg(slot_number); // adds message with some probability
				if(u.msg_count>0 && u.back_off==0){ // sending message
					u.retrans_count++;
					slot_sent_count++;
					attempters.add(u);
				}
				else
					u.decrementBackOff();
			}
			if(slot_sent_count==1){ // sent successfully
				total_sent_count++;
				u = attempters.remove();
				Packet p = u.removeMsg(slot_number+1); // including one slot for transmission
				avgdelay = (avgdelay*(total_sent_count-1) + (p.transmitted_slot-p.generated_slot))/total_sent_count;
			}
			else
			if(slot_sent_count>1){ // collided
				while(attempters.size()>0){
					u = attempters.remove();
					if(u.retrans_count>=maxRetries){
						break outer;
					}
					else
						u.setBackOff();
				}
			}
			slot_number++;
		}

		throughput = total_sent_count/(double)slot_number;

		if(!machineReadable){
			// System.out.println("Quitting after " + total_sent_count);
			System.out.println("Nodes: "+usercount + " W:" +initial_collision_window + " p:"+ PACKET_GEN_PROB +" Utilization: "+throughput+" Avg. Packet delay: "+avgdelay);
		}
		else{
			System.out.println(throughput+" "+avgdelay);
		}

	}
}