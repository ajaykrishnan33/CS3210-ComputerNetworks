import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

public class EmailClient {
    private BufferedReader in;
    private PrintWriter out;
    private int mode = 0; //System- 0; user- 1;

    public void connectToServer(String ip, String port) throws IOException {
        // Make connection and initialize streams
        Socket socket = new Socket(ip, Integer.parseInt(port));
        in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        System.out.print(in.readLine() + "\n");

    }

    public String processCommands(String input){
        StringTokenizer st = new StringTokenizer(input, " ");
        String command = st.nextToken();
        if(command.equals("Listusers")){
            if(mode==1){
                return null;
            }
            return "LSTU\n###";
        }
        else
        if(command.equals("Adduser")){
            if(mode==1){
                return null;
            }
            String userid;
            if(st.hasMoreTokens())
                userid = st.nextToken();
            else{
                System.out.println("Error: Userid not provided");
                return null;
            }
            return "ADDU "+userid+"\n###";
        }
        else
        if(command.equals("SetUser")){
            if(mode==1){
                return null;
            }
            String userid;
            if(st.hasMoreTokens())
                userid = st.nextToken();
            else{
                System.out.println("Error: Userid not provided");
                return null;
            }
            return "USER "+userid+"\n###";
        }
        else
        if(command.equals("Done")){
            if(mode==0){
                return null;
            }
            return "DONEU\n###";
        }
        else
        if(command.equals("Read")){
            if(mode==0){
                return null;
            }
            return "READM\n###";
        }
        else
        if(command.equals("Delete")){
            if(mode==0){
                return null;
            }
            return "DELM\n###";
        }
        else
        if(command.equals("Send")){
            if(mode==0){
                return null;
            }
            String userid;
            if(st.hasMoreTokens())
                userid = st.nextToken();
            else{
                System.out.println("Error: Userid not provided");
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Type subject:");
            String subject="";
            try{
                subject = br.readLine();
            }
            catch(Exception e){
                return null;
            }

            System.out.println("Type message:");
            String msg_contents = "", temp;
            while(true){
                try{
                    temp = br.readLine();
                    if(temp.endsWith("###")){
                        msg_contents += temp.substring(0, temp.length()-3);
                        break;
                    }
                    msg_contents += temp + "\n";
                }
                catch(Exception e){
                    return null;
                }
            }
            return "SEND " + userid + " " + subject + "###" + msg_contents + "\n###";
        }
        else
        if(command.equals("Quit")){
            if(mode==1){
                return null;
            }
            return "QUIT\n###";
        }

        return null;
    }

    public void onSuccess(String input, StringBuffer prompt){
        StringTokenizer st = new StringTokenizer(input, " ");
        String command = st.nextToken();
        if(command.equals("SetUser")){
            mode = 1;
            String userid = "";
            if(st.hasMoreTokens())
                userid = st.nextToken();
            String new_prompt = "Sub-Prompt-"+ userid +"> ";
            prompt.replace(0,prompt.length(), new_prompt);
            prompt.setLength(new_prompt.length());
        }
        else
        if(command.equals("Done")){
            mode = 0;
            String new_prompt = "Main-Prompt> ";
            prompt.replace(0,prompt.length(), new_prompt);
            prompt.setLength(new_prompt.length());
        }
    }

    public void takeInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String response, temp;
        StringBuffer prompt = new StringBuffer("Main-Prompt> ");
        while (true) {
            System.out.print(prompt.toString());

            String input;
            try {
                input = br.readLine();
                if (input == null || input.equals("")) {
                    System.exit(0);
                }
            } catch (IOException ex) {
                input = "Error: " + ex;
            }

            String command = processCommands(input);

            if(command==null){
                System.out.println("Incorrect command.");
                continue;
            }

            out.println(command);

            response = null;
            try{
                temp = in.readLine();
                int status = (int)temp.charAt(0)-48;
                if(status==1){
                    onSuccess(input, prompt);
                }

                response = temp.substring(1);
                if(response.equals("###")){
                    System.exit(0);
                }

                while(!(temp = in.readLine()).equals("###")){
                    if(temp==null){
                        break;
                    }
                    response += "\n" + temp;
                }
                if(response.equals("\n")||response.equals(""))
                    System.exit(0);
            } 
            catch(IOException ex){
                response = "Error: " + ex;
            }

            System.out.println(response);
        }
    }

    public static void main(String[] args) throws Exception {
        EmailClient client = new EmailClient();
        client.connectToServer(args[0], args[1]);
        client.takeInput();
    }

}