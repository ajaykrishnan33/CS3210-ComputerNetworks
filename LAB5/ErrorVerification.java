import java.io.*;
import java.util.*;

class ErrorVerification{

	public void makeInputs(){
		RandomAccessFile f;
		try{
			File file = new File("inputs");
			file.createNewFile();
			f = new RandomAccessFile(file, "rw");
			f.seek(0);
		}
		catch(Exception e){
			// System.out.println("Here");
			return;
		}
		String original1 = "", original2 = "";
		int i;
		for(i=0;i<26;i++)
			original1 += "0";
		for(i=0;i<26;i++)
			original2 += "1";

		for(i=0;i<25;i++){
			try{
				f.writeBytes(original1+"\n");
				original1 = corruptString2(original1);
				f.writeBytes(original2+"\n");
				original2 = corruptString2(original2);
			}
			catch(Exception e){
				// System.out.println("Here2");
				return;
			}
		}
	}

	public int randomInt(int max){
		return (int)(max*Math.random());
	}

	public String corruptString1(String input){
		StringBuffer sbuf = new StringBuffer(input);
		int pos = randomInt(input.length());
		System.out.println("Error introduced at: "+(pos+1));
		sbuf.setCharAt(pos, (char)(49 - (input.charAt(pos)-48)));
		return sbuf.toString();
	}

	public String corruptString2(String input){
		StringBuffer sbuf = new StringBuffer(input);
		int pos1=0, pos2=0;
		while(pos1==pos2){
			pos1 = randomInt(input.length());
			pos2 = randomInt(input.length());
		}

		System.out.println("Error introduced at: "+(pos1+1) + ", "+(pos2+1));
		sbuf.setCharAt(pos1, (char)(49 - (input.charAt(pos1)-48)));
		sbuf.setCharAt(pos2, (char)(49 - (input.charAt(pos2)-48)));
		return sbuf.toString();
	}

	public static void main(String[] args){
		// new ErrorVerification().makeInputs();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input="", output, corruptedString;
		
		HammingCode hc = new HammingCode(31, 26);
		ErrorVerification ev = new ErrorVerification();

		File file = new File("inputs");

		RandomAccessFile raf;
		try{
			raf = new RandomAccessFile(file, "r");
		}
		catch(Exception e){
			return;
		}

		int i;

		while(true){
			try{

				input = raf.readLine();
				if(input==null)
					break;
				System.out.println("Original String: "+input);
				output = hc.encodeSEC(input);
				System.out.println("Original String with Parity: "+output);

				corruptedString = ev.corruptString1(output);
				System.out.println("Corrupted String: "+corruptedString);
				System.out.println("Error found at pos: " + hc.findErrorSEC(corruptedString));
				System.out.println("");

				////////

				for(i=0;i<10;i++){
					System.out.println("Original String: "+input);
					output = hc.encodeSECDED(input);
					System.out.println("Original String with Parity: "+output);

					corruptedString = ev.corruptString2(output);
					System.out.println("Corrupted String: "+corruptedString);
					System.out.println("Error detected yes/no: " + hc.findErrorDED(corruptedString));
					System.out.println("");
				}

			}
			catch(Exception e){
				return;
			}
		}
	}
}