import java.io.*;
import java.util.*;

class TCPcc{

	double MSS = 1; //KB
	double RWS = 1000; // KB
	double CW;
	double Ki;
	double Km;
	double Kn;
	double Kf;
	double Ps;
	double threshold = 512; // KB
	int T = 100;
	String filename;

	// public TCPcc(double i, double m, double n, double f, double p, int t){
	// 	Ki = i;
	// 	Km = m;
	// 	Kn = n;
	// 	Kf = f;
	// 	Ps = p;
	// 	T = t;
	// }

	public void ack_exp(){
		CW = Math.min(CW + Km*MSS, RWS);
	}

	public void ack_lin(){
		CW = Math.min(CW + Kn*MSS*MSS/CW, RWS);
	}

	public void timeout(){
		threshold = CW/2;
		CW = Math.max(1, Kf*CW);
	}

	// assuming that the transmission occurs so fast that the entire window is sent
	// before the first timeout occurs - valid mostly
	// if timeout ever occurs for a packet, then all packets beginning at that point
	// needs to be transmitted again - this doesn't affect this code
	public void simulate() throws IOException{
		int i, packets_sent = 0, num;
		int update_count = 0;

		CW = Ki*MSS;
		PrintWriter pw = new PrintWriter(filename);

		pw.println("0|"+CW);

		int round = 0;

		outer: while(true){
			num = (int)Math.ceil(CW/MSS);
			for(i=0;i<num;i++){
				if(Math.random()<Ps){ // successful transmission
					packets_sent++;

					if(packets_sent>=T)
						break outer;

					if(CW<threshold){  // exponential section
						ack_exp();
						update_count++;
						// System.out.println(update_count+"|"+CW);
					}
					else{
						ack_lin();
						update_count++;
						// System.out.println(update_count+"|"+CW);						
					}

				}
				else{	// timeout
					timeout();
					update_count++;
					// System.out.println(update_count+"|"+CW);
					break;
				}
			}
			round++;

			// if(CW>=threshold){ // linear section
			// 	ack_lin();
			// 	update_count++;
			// 	// System.out.println(update_count+"|"+CW);
			// }

			pw.println(round+"|"+CW);

		}

		pw.close();

	}

	public static void main(String[] args) throws IOException{
		int i;

		TCPcc t = new TCPcc();

		for(i=0;i<args.length;i++){
			if(args[i].charAt(0)!='-'){
				System.out.println("Error. Incorrect option specification.");
				return;
			}
			switch(args[i].charAt(1)){
				case 'i':
					t.Ki = Double.parseDouble(args[i+1]);
					break;
				case 'm':
					t.Km = Double.parseDouble(args[i+1]);
					break;
				case 'n':
					t.Kn = Double.parseDouble(args[i+1]);
					break;
				case 'f':
					t.Kf = Double.parseDouble(args[i+1]);
					break;
				case 's':
					t.Ps = 1 - Double.parseDouble(args[i+1]);
					break;
				case 'T':
					t.T = Integer.parseInt(args[i+1]);
					break;
				case 'o':
					t.filename = args[i+1];
					break;
				default:
					System.out.println("Unknown option " + args[i]);
					return;
			}
			i++;
		}

		t.simulate();
	}
}