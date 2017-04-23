import java.io.*;
import java.util.*;

class HammingCode{

	HashMap<Integer, ArrayList<Integer>> numberSets = new HashMap<Integer, ArrayList<Integer>>();

	int codeLen;
	int msgLen;

	public HammingCode(int finalLen, int msgLen){
		this.msgLen = msgLen;
		this.codeLen = finalLen - msgLen;
		this.constructSets();
	}

	public void constructSets(){
		int i = 1, j, count = 0;
		
		int len = this.msgLen + this.codeLen;

		while(count<this.codeLen){

			ArrayList<Integer> temp;
			if(!numberSets.containsKey(i)){
				temp = new ArrayList<Integer>();
				numberSets.put(i, temp);
			}
			else
			{
				temp = numberSets.get(i);
			}
			
			for(j=1;j<=len;j++){

				if(j!=i && (j&i)==i){
					temp.add(j);
				}			

			}			

			i*=2;
			count++;
		}

	}

	public HashMap<Integer, Integer> computeParities(String input){
		int i, j=0, k, count;

		HashMap<Integer, Integer> res = new HashMap<Integer, Integer>();

		for(Map.Entry<Integer, ArrayList<Integer>> entry:numberSets.entrySet()){
			ArrayList<Integer> temp = entry.getValue();
			count = 0;
			for(j=0;j<temp.size();j++){
				if((int)input.charAt(temp.get(j)-1)-48==1){
					count++;
				}
			}
			res.put(entry.getKey(), count%2);
		}

		return res;
	}

	public String encodeSEC(String input){

		int i, j=0, k, count;

		StringBuffer sbuf = new StringBuffer();
		int len = this.codeLen+this.msgLen;

		sbuf.setLength(len);

		for(i=0;i<len;i++){
			if(!numberSets.containsKey(i+1)){
				sbuf.setCharAt(i, input.charAt(j++));
			}
			else
				sbuf.setCharAt(i, '0');
		}

		HashMap<Integer, Integer> parities = computeParities(sbuf.toString());
		
		for(Map.Entry<Integer, Integer> entry:parities.entrySet()){
			sbuf.setCharAt(entry.getKey()-1, (char)(entry.getValue() + 48));
		}

		return sbuf.toString();	
	}

	public String encodeSECDED(String input){
		String output = encodeSEC(input);
		int i, count=0;
		for(i=0;i<output.length();i++){
			if(output.charAt(i)-48==1){
				count++;
			}
		}
		output += (char)(count%2 + 48);
		return output;
	}

	public int findErrorSEC(String input){
		int i, j = 0;

		HashMap<Integer, Integer> parities = computeParities(input);

		int res = 0;
		for(Map.Entry<Integer, Integer> entry: parities.entrySet()){
			if(input.charAt(entry.getKey()-1)-48!=entry.getValue()) {
				res += entry.getKey();
			}
		}
		return res;
	}

	public boolean findErrorDED(String input){
		int i, count=0;
		for(i=0;i<input.length()-1;i++){
			if(input.charAt(i)-48==1){
				count++;
			}
		}

		HashMap<Integer, Integer> parities = computeParities(input);

		int res = 0;
		System.out.print("Parity bits that failed are at: ");
		for(Map.Entry<Integer, Integer> entry: parities.entrySet()){
			if(input.charAt(entry.getKey()-1)-48!=entry.getValue()) {
				System.out.print(entry.getKey()+" ");
			}
		}
		System.out.println("");

		return count%2!=input.charAt(input.length()-1);
	}

}