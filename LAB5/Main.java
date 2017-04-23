import java.io.*;
import java.util.*;

class Main{

	public static void main(String[] args){
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		// System.out.print("Input: ");
		String input="", output;
		
		File file = new File("inputs");

		RandomAccessFile raf;
		try{
			raf = new RandomAccessFile(file, "r");
		}
		catch(Exception e){
			return;
		}

		HammingCode hc = new HammingCode(31, 26);

		while(true){
			try{
				input = raf.readLine();
				if(input==null)
					break;
				System.out.println("Input: "+input);
				output = hc.encodeSEC(input);
				System.out.println("Output : "+output+"\n");	
			}
			catch(Exception e){
				return;
			}
		}
	
	}

}