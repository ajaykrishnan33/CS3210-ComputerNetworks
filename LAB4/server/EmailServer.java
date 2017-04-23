import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;

public class EmailServer {

	int clientNumber = 0;
	EmailSystemHandler esh;

	public EmailServer() {
		esh = new EmailSystemHandler();
	}

	public boolean createThread(ServerSocket listener) {
		try {
			new ClientControlThread(listener.accept(), clientNumber++).start();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("The email server is running.");
		ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));

		EmailServer es = new EmailServer();

		try {
			while (true) {
				es.createThread(listener);
			}
		} finally {
			listener.close();
		}
	}

	private class ClientControlThread extends Thread {
		
		private Socket socket;
		private int clientNumber;

		private UserEmailHandler ueh;

		public ClientControlThread(Socket socket, int clientNumber) {
			this.socket = socket;
			this.clientNumber = clientNumber;
			this.ueh = new UserEmailHandler(EmailServer.this.esh);
			log("New connection with client# " + clientNumber + " at " + socket);
		}

		public String dispatchActions(String input){

			StringTokenizer st = new StringTokenizer(input, " ");
			String command = st.nextToken();

			if(command.equals("LSTU")){
				return "1"+EmailServer.this.esh.getUserList();
			}
			else
			if(command.equals("ADDU")){
				String userid;
				if(st.hasMoreTokens())
					userid = st.nextToken();
				else{
					return "0Error: Userid not provided";
				}
				return "1"+EmailServer.this.esh.addUser(userid);
			}
			else
			if(command.equals("USER")){
				String userid;
				if(st.hasMoreTokens())
					userid = st.nextToken();
				else{
					return "0Error: Userid not provided";
				}
				if(this.ueh.setUser(userid)){
					return "1Set " + userid + " as the current user";
				}
				else
				{					
					return "0Error: Can't find userid " + userid;
				}
			}
			else
			if(command.equals("READM")){
				String ret = this.ueh.readNextMail();
				if(ret==null){
					ret = "0Error: User not set";
				}
				else
					ret = "1"+ret;
				return ret;
			}
			else
			if(command.equals("DELM")){
				String ret = this.ueh.deleteNextMail();
				if(ret==null){
					ret = "0Error: User not set";
				}
				else
					ret = "1"+ret;
				return ret;
			}
			else
			if(command.equals("SEND")){
				String userid;
				if(st.hasMoreTokens())
					userid = st.nextToken();
				else{
					return "0Error: Receiver id not provided";
				}
				String subject = st.nextToken("###");
				String message = st.nextToken();

				if(this.ueh.newMail(userid, subject, message)){
					return "1Mail sent successfully";
				}
				else
				{					
					return "0Error in sending mail";
				}
			}
			else
			if(command.equals("DONEU")){
				this.ueh.doneOperations();
				return "1Done";
			}
			else
			if(command.equals("QUIT")){
				this.ueh.doneOperations();
				return "1\n###";
			}

			return "0Unknown Command";

		}

		public void run() {
			try{
				BufferedReader in = new BufferedReader(
				    new InputStreamReader(socket.getInputStream()));
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

				// Send a welcome message to the client.
				out.println("Hello, you are client #" + clientNumber + ".");
				String input, temp;
				while (true) {
					input = in.readLine();

					if(input.equals("###")){
						out.println("No command received\n###");
	                    continue;
	                }

	                while(!(temp = in.readLine()).equals("###")){
	                    if(temp==null){
	                        break;
	                    }
	                    input += "\n" + temp;
	                }

					log("Received command : " + input);

					String output = this.dispatchActions(input);
					log(output);
					out.println(output+"\n###");
					if(input == null || input.equals("QUIT")) {
						break;
					}
				}
			} 
			catch(IOException e){
				log("Error handling client# " + clientNumber + ": " + e);
			} 
			finally{
				try{
					socket.close();
				}
				catch(IOException e){
					log("Couldn't close a socket, what's going on?");
				}
				log("Connection with client# " + clientNumber + " closed");
			}
		}

		private void log(String message) {
			System.out.println(message);
		}
	}
}

class EmailSystemHandler {

	public HashMap<String, File> userFileMap = new HashMap<String, File>();

	public EmailSystemHandler() {
		init();
	}

	private void init() {
		File folder = new File("./data");
		File[] fileList = folder.listFiles();

		String filename;

		for (int i = 0; i < fileList.length; i++) {
			if (fileList[i].isFile()) {
				filename = fileList[i].getName();
				userFileMap.put(filename, fileList[i]);
			}
		}
	}

	public String getUserList() {
		String ret = "";
		for (Map.Entry<String, File> entry : userFileMap.entrySet()) {
			ret += entry.getKey() + " ";
		}
		if(ret.equals(""))
			ret = "No users present yet";
		return ret;
	}

	public String addUser(String id) {
		if (userFileMap.containsKey(id)) {
			return "Error: Userid already present";
		}
		File newUserFile = new File("./data/" + id);
		try{
			if(newUserFile.createNewFile()){
				userFileMap.put(id, newUserFile);
				return "New user added successfully";
			}
			else{
				return "Error occurred in adding new user";
			}
		} 
		catch(IOException e){
			return "Error occurred in adding new user";
		}

	}

	public RandomAccessFile getUser(String id) {
		if (!userFileMap.containsKey(id)) {
			return null;
		}
		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(userFileMap.get(id), "rw");
		} catch (FileNotFoundException fe) {
			return null;
		}
		return raf;
	}

}

class UserEmailHandler {

	String userid;
	RandomAccessFile raf;
	EmailSystemHandler esh;

	public UserEmailHandler(EmailSystemHandler esh) {
		this.esh = esh;
	}

	public UserEmailHandler(EmailSystemHandler esh, String id) {
		this.esh = esh;
		setUser(id);
	}

	public boolean setUser(String id) {
		doneOperations();
		raf = esh.getUser(id);
		if(raf!=null)
			this.userid = id;
		return raf!=null;
	}

	public String readNextMail() {
		String mail_contents = "", temp;
		while (true) {
			try {
				temp = raf.readLine();
				if (temp == null) {
					return "Error: Reached end of mail list";
				}
			} catch (Exception e) {
				return null;
			}

			if (temp.equals("###"))
				break;
			mail_contents += temp + "\n";
		}
		return mail_contents;
	}

	public String deleteNextMail() {
		long currentPointer, originalLength, mailLength;
		try {
			currentPointer = raf.getFilePointer();
			originalLength = raf.length();
			mailLength = 0;
		} catch (Exception e) {
			return null;
		}

		String temp;
		while (true) {
			try {
				temp = raf.readLine();
				if (temp == null) {
					return "Error: Reached end of mail list";
				}
			} catch (Exception e) {
				return null;
			}
			mailLength += temp.length()+1;
			if (temp.equals("###"))
				break;
		}

		String remaining = "";
		while (true) {
			try {
				temp = raf.readLine();
				if (temp == null) {
					break;
				}
			} catch (Exception e) {
				System.out.println("Here");
				return null;
			}
			remaining += temp + "\n";
		}

		try {
			raf.seek(currentPointer);
			raf.writeBytes(remaining);
			raf.setLength(originalLength - mailLength + 1);
			raf.seek(currentPointer);
		} catch (Exception e) {
			return null;
		}

		return "Deleted current mail";
	}

	public boolean newMail(String receiver, String subject, String body) {
		String mail = "\nFrom: " + userid + "\nTo: " + receiver + "\nDate: " + new Date().toString() + "\nSubject: " + subject + "\n" + body + "\n###";
		RandomAccessFile writer = esh.getUser(receiver);
		if (writer == null) {
			return false;
		}
		try {
			writer.seek(writer.length());
			writer.writeBytes(mail);
			writer.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean doneOperations() {
		userid = null;
		if (raf != null) {
			try {
				raf.close();
				raf=null;
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}

}