import java.io.*;
import java.util.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Collections;

class GlobalVariables{
	public static int NODE_ID;
	public static String INPUT_FILE;
	public static String OUTPUT_FILE;
	public static int HELLO_INTERVAL = 1;
	public static int LSA_INTERVAL = 5;
	public static int SPF_INTERVAL = 20;
	public static int NODE_COUNT;
	public static int LINK_COUNT;
		
	public static int LSA_SEQ = 0;

	public static HashMap<Integer, Edge> neighbours = new HashMap<Integer, Edge>();
	
	public static HashMap<Integer, Integer> lsaMap = new HashMap<Integer, Integer>();

	public static HashMap<Integer, ArrayList<Edge>> network = new HashMap<Integer, ArrayList<Edge>>();

	public static HashMap<Integer, ArrayList<Integer>> forwardingTable = new HashMap<Integer, ArrayList<Integer>>();

	public static DatagramSocket socket;
	public static InetAddress address;

	public static ReentrantLock lock = new ReentrantLock(true); // fair lock

	public static int MAX_COST;

	public static int TIME_STEP = 0;

}

class HelloSender extends TimerTask{
	public void run(){
		try{
			GlobalVariables.lock.lock();
			String msg = "HELLO "+GlobalVariables.NODE_ID;
			byte[] buf = msg.getBytes();
			for(Map.Entry<Integer, Edge> e:GlobalVariables.neighbours.entrySet()){
				DatagramPacket pkt = new DatagramPacket(buf, buf.length, GlobalVariables.address, 10000+e.getKey());
				try{
					GlobalVariables.socket.send(pkt);
				}
				catch(Exception ex){
					System.out.println("Hello Send exception: " + ex);
					System.exit(0);
				}
			}	
		}
		catch(Exception ex){
			System.out.println("Hello Sender exception: " + ex);
		}
		finally{
			GlobalVariables.lock.unlock();
		}
	}
}

class LSASender extends TimerTask{
	public void run(){
		try{
			GlobalVariables.lock.lock();
			String msg = "LSA "+GlobalVariables.NODE_ID+" "+GlobalVariables.LSA_SEQ++;
			String entries = "";
			int ct = 0;
			for(Map.Entry<Integer,Edge> e:GlobalVariables.neighbours.entrySet()){
				if(e.getValue().cost>=0){
					entries += " " + e.getKey() + " " +e.getValue().cost;
					ct++;
				}
			}

			msg += " " + ct + entries;

			byte[] buf = msg.getBytes();
			for(Map.Entry<Integer, Edge> e:GlobalVariables.neighbours.entrySet()){
				DatagramPacket pkt = new DatagramPacket(buf, buf.length, GlobalVariables.address, 10000+e.getKey());
				try{
					GlobalVariables.socket.send(pkt);
				}
				catch(Exception ex){
					System.out.println("LSA Packet send exception: " + ex);
					System.exit(0);
				}
			}
			//Sending to myself	
			DatagramPacket pkt = new DatagramPacket(buf, buf.length, GlobalVariables.address, 10000+GlobalVariables.NODE_ID);
			try{
				GlobalVariables.socket.send(pkt);
			}
			catch(Exception ex){
				System.out.println("LSA Packet self send exception: " + ex);
				System.exit(0);
			}
		}
		catch(Exception ex){
			System.out.println("LSASender exception " + ex);
		}
		finally{
			GlobalVariables.lock.unlock();
		}
	}
}

class Node{
	int id;
	int dist;
	int prev = -1;

	public Node(int id){
		this.id = id;
		this.dist = GlobalVariables.MAX_COST + 100;
	}
}

class NodeComparator implements Comparator<Node>{
	@Override
	public int compare(Node x, Node y){
		return x.dist - y.dist;
	}
}

class SPFComputer extends TimerTask{

	public void Compute(){
		int src = GlobalVariables.NODE_ID;
		Comparator<Node> comparator = new NodeComparator();

		PriorityQueue<Node> queue = new PriorityQueue<Node>(GlobalVariables.NODE_COUNT, comparator);
		HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

		for(Map.Entry<Integer, ArrayList<Edge>> entry:GlobalVariables.network.entrySet()){
			Node n = new Node(entry.getKey());
			if(n.id==GlobalVariables.NODE_ID)
				n.dist = 0;
			queue.add(n);
			nodes.put(n.id, n);
		}

		int nodeCount = GlobalVariables.network.entrySet().size();

		Set<Integer> foundSet = new HashSet<Integer>();

		while(foundSet.size()<nodeCount){
			Node min = queue.poll();
			foundSet.add(min.id);
			for(Edge e:GlobalVariables.network.get(min.id)){
				Node nei = nodes.get(e.dest);
				if(e.cost>=0 && (nei.dist > e.cost + min.dist)){
					nei.dist = e.cost + min.dist;
					nei.prev = min.id;
					// force reordering
					queue.remove(nei);
					queue.add(nei);
				}
			}
		}

		// pw.println("\nDistances:");
		// make forwarding table
		for(Map.Entry<Integer, Node> entry:nodes.entrySet()){
			Node n = entry.getValue();

			// pw.println(n.id+":"+n.dist);

			ArrayList<Integer> temp = new ArrayList<Integer>();

			if(n.id==GlobalVariables.NODE_ID)
				continue;
			int next=-1;
			while(n.id!=GlobalVariables.NODE_ID){
				next = n.id;
				temp.add(n.id);
				if(n.prev==-1){
					next = -1;
					break;
				}
				n = nodes.get(n.prev);
			}
			if(next==-1)
				continue;
			if(GlobalVariables.neighbours.get(next).cost==-1)
				continue;
			Collections.reverse(temp);
			GlobalVariables.forwardingTable.put(entry.getKey(), temp);
		}


		PrintWriter pw;
		try{
			pw = new PrintWriter(new BufferedWriter(new FileWriter(GlobalVariables.OUTPUT_FILE, true)));

			pw.println("Forwarding Table for Node " + GlobalVariables.NODE_ID + " at TIME_STEP " + GlobalVariables.TIME_STEP++ + ":");
			pw.println("Destination\tPath\tCost");
			// print stuff
			for(Map.Entry<Integer, ArrayList<Integer>> entry: GlobalVariables.forwardingTable.entrySet()){
				Node n = nodes.get(entry.getKey());
				pw.println(entry.getKey() + "\t\t" + entry.getValue() + "\t" + n.dist);
			}
			pw.println("\n");

			pw.close();
		}
		catch(IOException e){
			System.out.println("print writer exception");
		}
	}

	public void run(){
		try{
			GlobalVariables.lock.lock();
			// perform dijkstra to find the single-source shortest path to each node
			this.Compute();
		}
		catch(Exception ex){
			System.out.println("Compute exception: " +ex);
		}
		finally{
			GlobalVariables.lock.unlock();
		}
	}
}

class Listener extends Thread{
	public void run(){
		byte[] buf;
		DatagramPacket pkt;
		String msg, header;
		Scanner sc;
		int src_id;
		while(true){
			buf = new byte[100];
			pkt = new DatagramPacket(buf, buf.length);
			try{
				GlobalVariables.socket.receive(pkt);
			}
			catch(Exception ex){
				System.out.println("Packet receive exception: " + ex);
				System.exit(0);
			}
			msg = new String(pkt.getData());
			msg = msg.trim();
			sc = new Scanner(msg);
			header = sc.next();

			if(header.equals("HELLO")){  // then send a reply
				src_id = sc.nextInt();

				String reply = "HELLOREPLY " + GlobalVariables.NODE_ID + " " + src_id + " " + GlobalVariables.neighbours.get(src_id).getCost();
				byte[] ret = reply.getBytes();
				DatagramPacket replyPkt = new DatagramPacket(ret, ret.length, pkt.getAddress(), pkt.getPort());
				try{
					GlobalVariables.socket.send(replyPkt);
				}
				catch(Exception ex){
					System.out.println("HelloReply send exception: " + ex);
					System.exit(0);
				}
			}
			else
			if(header.equals("HELLOREPLY")){ // on receiving reply, record link cost;
				src_id = sc.nextInt();
				if(GlobalVariables.NODE_ID != sc.nextInt()){ // my id
					System.out.println("Hello reply receive source exception");
					System.exit(0);
				}
				try{
					GlobalVariables.lock.lock();
					GlobalVariables.neighbours.get(src_id).cost = sc.nextInt(); // link cost from me to neighbour
				}
				catch(Exception ex){
					System.out.println("HelloReply exception: "+ ex);
				}
				finally{
					GlobalVariables.lock.unlock();
				}
			}
			else
			if(header.equals("LSA")){ // add nodes and edges to network based on LSA data

				try{
					GlobalVariables.lock.lock();

					src_id = sc.nextInt();
					int seq = sc.nextInt();
					if(!GlobalVariables.lsaMap.containsKey(src_id)){
						GlobalVariables.lsaMap.put(src_id, -1);
					}
					if(seq > GlobalVariables.lsaMap.get(src_id)){ // not seen yet

						// updating lsaMap sequence no
						GlobalVariables.lsaMap.put(src_id, seq);
												
						// extract data
						ArrayList<Edge> edges = new ArrayList<Edge>();
						GlobalVariables.network.put(src_id, edges);

						int count = sc.nextInt();
						for(int i=0;i<count;i++){
							edges.add(new Edge(src_id, sc.nextInt(), sc.nextInt()));
						}

						// System.out.println("Graph:"+GlobalVariables.NODE_ID);

						// for(Map.Entry<Integer,ArrayList<Edge>> entry:GlobalVariables.network.entrySet()){
						// 	System.out.print(entry.getKey()+":");
						// 	for(Edge e : entry.getValue()){
						// 		if(e.cost>=0)
						// 			System.out.print(e.dest + " ");
						// 	}
						// 	System.out.println("");
						// }
						// System.out.println("");

						// forward packet
						int last = pkt.getPort()-10000;
						for(int i=0;i<GlobalVariables.NODE_COUNT;i++){
							if(i==GlobalVariables.NODE_ID || i==src_id || i==last) // don't forward to myself, src, and to last sender
								continue;
							byte[] forwardBuf = msg.getBytes();
							DatagramPacket forward = new DatagramPacket(forwardBuf, forwardBuf.length, GlobalVariables.address, 10000+i);
							try{
								GlobalVariables.socket.send(forward);
							}
							catch(Exception ex){
								System.out.println("LSA Packet forward exception: " + ex);
								System.exit(0);
							}
						}
					} // else ignore LSA packet
					// else
					// 	System.out.println("Rejected:"+msg+"\n");
				}
				catch(Exception ex){
					System.out.println("LSA Receive exception: " + ex);
				}
				finally{
					GlobalVariables.lock.unlock();
				}
			}
		}
	}
}

class Edge{
	int source;
	int dest;
	int min;
	int max;
	int cost = -1;
	
	public Edge(int source, int dest, int min, int max){
		this.source = source;
		this.dest = dest;
		this.min = min;
		this.max = max;
	}

	public Edge(int source, int dest, int cost){
		this.source = source;
		this.dest = dest;
		this.cost = cost;
	}
	
	public int getCost(){
		return min + (int)(Math.random()*(max-min+1));
	}
}

public class OSPFRouter{
	
	public static void findNeighbours() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(GlobalVariables.INPUT_FILE));
		String currLine;
		Scanner sc;
		currLine = br.readLine();
		sc = new Scanner(currLine);
		GlobalVariables.NODE_COUNT = sc.nextInt();
		GlobalVariables.LINK_COUNT = sc.nextInt();
		int n1, n2, min, max;

		while((currLine = br.readLine())!=null){
			sc = new Scanner(currLine);
			n1 = sc.nextInt();
			n2 = sc.nextInt();
			min = sc.nextInt();
			max = sc.nextInt();
			if(n1==GlobalVariables.NODE_ID){
				Edge e = new Edge(n1, n2, min, max);
				GlobalVariables.neighbours.put(e.dest, e);
				// edges.add(e);
				// GlobalVariables.network.put(e.dest, new ArrayList<Edge>());
			}
			else
			if(n2==GlobalVariables.NODE_ID){
				Edge e = new Edge(n2, n1, min, max);
				GlobalVariables.neighbours.put(e.dest, e);
				// edges.add(e);
				// GlobalVariables.network.put(e.dest, new ArrayList<Edge>());
			}
			GlobalVariables.MAX_COST += max+1;
		}
		br.close();
	}

	public static void main(String[] args) throws IOException{

		for(int i=0;i<args.length;i++){
			if(args[i].charAt(0)!='-'){
				System.err.println("Error in arguments");
				return;
			}
			switch(args[i].charAt(1)){
				case 'i':
					GlobalVariables.NODE_ID = Integer.parseInt(args[i+1]);
					break;
				case 'f':
					GlobalVariables.INPUT_FILE = args[i+1];
					break;
				case 'o':
					GlobalVariables.OUTPUT_FILE = args[i+1];
					break;
				case 'h':
					GlobalVariables.HELLO_INTERVAL = Integer.parseInt(args[i+1]);
					break;
				case 'a':
					GlobalVariables.LSA_INTERVAL = Integer.parseInt(args[i+1]);
					break;
				case 's':
					GlobalVariables.SPF_INTERVAL = Integer.parseInt(args[i+1]);
					break;
				default:
					System.err.println("Invalid option "+args[i]);
					return;
			}
			i++;
		}

		// read file and find neighbours
		OSPFRouter.findNeighbours();
		GlobalVariables.socket = new DatagramSocket(10000+GlobalVariables.NODE_ID);
		GlobalVariables.address = InetAddress.getByName("localhost");

		// start listener
		new Listener().start();

		// start periodic hello sender
		Timer t1 = new Timer();
		t1.schedule(new HelloSender(), 0, (long)1000*GlobalVariables.HELLO_INTERVAL);

		// start periodic LSA pckt sender
		Timer t2 = new Timer();
		t2.schedule(new LSASender(), 1000*GlobalVariables.LSA_INTERVAL, (long)1000*GlobalVariables.LSA_INTERVAL);

		// start periodic SPF computer
		Timer t3 = new Timer();
		t3.schedule(new SPFComputer(), 1000*GlobalVariables.SPF_INTERVAL, (long)1000*GlobalVariables.SPF_INTERVAL);

	}

}
