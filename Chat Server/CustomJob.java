import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomJob{
    String service;
    String time;
    Runnable task;

    Socket socket;
    Socket phpServer;
    int clientNumber;

    public CustomJob(Socket socket,Socket phpServer,BufferedReader in,DataOutputStream os,PrintWriter out, int client, String service){
        this.clientNumber = client;
        this.service = service;

        this.socket = socket;
        this.phpServer = phpServer;

        DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        time = format.format(date);

        if(service.equalsIgnoreCase("kill")){
            return;
        }else if(service.matches("[a-zA-z]+")) {
            if(service.equalsIgnoreCase("chat")){
                this.service = "chat";
                try{
                    os.writeUTF("Beginning chat connection...\n");
                }catch(IOException e){
                    return;
                }
            }else{
                this.service = "capitalize,"+service;
            }
            task = capitalize(in,out,clientNumber,service);
        }else if(!service.equalsIgnoreCase("")){
            String[]command = service.split(",");
            if(command.length == 3){
                if(command[0].equalsIgnoreCase("add")){
                    task = add(command[1],command[2],in,out);
                }else if(command[0].equalsIgnoreCase("sub")){
                    task = subtract(command[1],command[2],in,out);
                }else if(command[0].equalsIgnoreCase("mul")){
                    task = multiply(command[1],command[2],in,out);
                }else if(command[0].equalsIgnoreCase("div")){
                    task = divide(command[1],command[2],in,out);
                }else{
                    task = errorMsg(in,out,clientNumber);
                }
            }else if(service.contains("GET")){
                task = helloWeb(in,os,clientNumber,service);
            }else if(service.startsWith("##")){
                task = chatConnection(in,os,clientNumber,service.substring(2));
                service = "chat connection";
            }else{
                task = errorMsg(in,out,clientNumber);
            }
        }
    }
    public CustomJob(int clientNumber,Runnable task){
        this(null,null,null,clientNumber,null);
        this.task = task;
        this.service = "Anonymous service";
    }
    public CustomJob(Socket socket,BufferedReader in,PrintWriter out,int client,Runnable task){
        this(socket,null,in,null,out,client,"'main'");
        this.task = task;
    }

    public Runnable capitalize(BufferedReader in,PrintWriter out,int clientNumber,String output){
        System.out.println("creating capitalize thread");
        Runnable ret = ()->{
            StringBuilder builder = new StringBuilder(output);
            for(int i=0;i<builder.length();i++){
                if(!Character.isWhitespace(builder.charAt(i))){
                    builder.setCharAt(i,Character.toUpperCase(builder.charAt(i)));
                }
            }
            out.println(builder);

            DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date cal = new Date();
            time = format.format(cal);

            System.out.println("worker thread id="+clientNumber+" processed service request capitalize,"+output+" with time stamp "+time);
            return;
        };
        return ret;
    }
    public Runnable add(final String rhs,final String lhs,BufferedReader in,PrintWriter out){
        Runnable ret = ()->{
            Integer right = Integer.parseInt(rhs);
            Integer left = Integer.parseInt(lhs);

            DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date cal = new Date();
            time = format.format(cal);

            out.println(rhs+" + "+lhs+" = "+(right+left));
            System.out.println("worker id="+clientNumber+" has serviced ADD,"+right+","+left+" and time stamp "+time);
            return;
        };
        return ret;
    }
    public Runnable subtract(final String rhs,final String lhs,BufferedReader in,PrintWriter out){
        Runnable ret = ()->{
            Integer right = Integer.parseInt(rhs);
            Integer left = Integer.parseInt(lhs);
            System.out.println(right+" - "+left+"= "+(right-left));

            DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date cal = new Date();
            time = format.format(cal);

            out.println(rhs+" - "+lhs+" = "+(right-left));
            System.out.println("worker id="+clientNumber+" has serviced SUB,"+right+","+left+" with time stamp "+time);
            return;
        };
        return ret;
    }
    public Runnable multiply(final String rhs,final String lhs,BufferedReader in,PrintWriter out){
        Runnable ret = ()->{
            Integer right = Integer.parseInt(rhs);
            Integer left = Integer.parseInt(lhs);

            DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date cal = new Date();
            time = format.format(cal);

            out.println(rhs+" * "+lhs+" = "+(right*left));
            System.out.println("worker id="+clientNumber+" has serviced MUL,"+right+","+left+" with time stamp "+time);
            return;
        };
        return ret;
    }
    public Runnable divide(final String rhs,final String lhs,BufferedReader in,PrintWriter out){
        Runnable ret = ()->{
            Integer right = Integer.parseInt(rhs);
            Integer left = Integer.parseInt(lhs);

            DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date cal = new Date();
            time = format.format(cal);

            out.println(rhs+" / "+lhs+" = "+(right/left));
            System.out.println("worker id="+clientNumber+" has serviced DIV,"+right+","+left+" with time stamp "+time);
            return;
        };
        return ret;
    }
    public Runnable errorMsg(BufferedReader in,PrintWriter out,int clientNumber){
        Runnable ret = ()->{
            String one = "Error! Wrong Usage";
            String two = " Input must be of the form [command],[number],[number]";
            String three = " OR [String]";
            out.println(one+two+three);

            DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date cal = new Date();
            time = format.format(cal);

            System.out.println("worker thread id="+clientNumber+" processed service 'Error Message!' with time stamp "+time);
            return;
        };
        return ret;
    }
    public Runnable helloWeb(BufferedReader in,final DataOutputStream os,int clientNumber,String service){
        System.out.println("CREATING HELLO SERVER THREAD");
        String path = "C:\\Users\\halim\\Desktop\\School\\quarter4\\Parallel&Cloud\\hw45\\src\\front.html";

        String[]parse = service.split(" ");

        if(parse[1].contains("/?command")){
            path = "C:\\Users\\halim\\Desktop\\School\\quarter4\\Parallel&Cloud\\hw45\\src\\command.html";
        }

        File file = new File(path);

        if(!file.exists()){
            System.out.println("ERROR CANT FIND FILE");
            return null;
        }
        if(socket.isClosed()){
            System.out.println("ERROR CLOSED SOCKET");
            return null;
        }
        Runnable ret = ()->{
            if(parse[1].equalsIgnoreCase("/")){
                doRoot(file,os,in);
            }else if(parse[1].contains("/?command")){
                handleCommand(file,os,in,parse);
            }else if(parse[1].contains("/displayText.php")){
                sendToPHP(parse[1],os);
            }else if(parse[1].endsWith(".css")){
                String css = "C:\\Users\\halim\\Desktop\\School\\quarter4\\Parallel&Cloud\\hw45\\src\\"+parse[1].substring(1);
                File cssFile = new File(css);
                sendCss(cssFile,in,os);
            }

            DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date cal = new Date();
            time = format.format(cal);

            System.out.println("worker thread id="+clientNumber+" processed service HELLO SERVER"+" with time stamp "+time);
            return;
        };
        return ret;
    }
    public Runnable chatConnection(BufferedReader in,final DataOutputStream os,int clientNumber,String service){
        Runnable ret = ()->{
            try{
                String input = service;//in.readLine();
                //while(!input.equalsIgnoreCase("quit")){
                    os.writeUTF("Client "+clientNumber+": "+input);
                    os.flush();
                    //input = in.readLine();
                //}
            }catch(IOException e){
                e.printStackTrace();
            }
        };
        this.service = service;
        return ret;
    }

    private void doRoot(File file,DataOutputStream os,BufferedReader in){
        try{
            String content = "Content-type: "+"text/html"+"\r\n";
            String status = "HTTP/1.1 200 OK"+"\r\n";

            FileInputStream fis = new FileInputStream(file);

            byte[]buffer = new byte[1024];
            int bytes = 0;

            os.writeBytes(status);
            os.writeBytes(content);
            os.writeBytes("\r\n");

            while((bytes = (fis.read(buffer))) != -1){
                os.write(buffer,0,bytes);
            }
            os.writeBytes(status);
            fis.close();
            StringBuffer response = new StringBuffer();
            String aux = "";
            while((aux = in.readLine()).length() != 0){
                response.append(aux);
            }
            System.out.println("SERVER RESPONSE IS "+response);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    private void handleCommand(File file,DataOutputStream os,BufferedReader in,String[]parse){
        int start = parse[1].indexOf('=');
        int end = parse[1].length();
        String command = parse[1].substring(start+1,end);
        System.out.println("COMMAND IS "+command.toUpperCase());
        File cssFile = file;

        if(command.equalsIgnoreCase("chat")){
            String path = "C:\\Users\\halim\\Desktop\\School\\quarter4\\Parallel&Cloud\\hw45\\src\\chatPage.html";
            file = new File(path);
        }

        try{
            String content = "Content-type: "+"text/html"+"\r\n";
            String status = "HTTP/1.1 200 OK"+"\r\n";

            FileInputStream fis = new FileInputStream(file);

            byte[]buffer = new byte[1024];
            int bytes = 0;

            os.writeBytes(status);
            os.writeBytes(content);
            os.writeBytes("\r\n");

            while((bytes = (fis.read(buffer))) != -1){
                os.write(buffer,0,bytes);
            }
            //os.writeBytes(command.toUpperCase());
            fis.close();
            StringBuffer response = new StringBuffer();
            String aux = "";
            while((aux = in.readLine()).length() != 0){
                response.append(aux);
            }
            System.out.println("SERVER RESPONSE IS "+response);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    private void sendToPHP(String phpCode,DataOutputStream backToServer){
        try{
            System.out.println("SENDING TO PHP SERVER");
            String urlName = "http://localhost:9899/displayText.php";
            BufferedReader in;
            DataOutputStream os;

            URL url = new URL(urlName);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.setDoOutput(true);
            connect.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)");
            connect.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            connect.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
            connect.setRequestProperty("Accept-Language", "en-US,en;q=0.8,es;q=0.6");
            connect.setRequestProperty("Connection", "keep-alive");
            connect.connect();

            System.out.println(connect.getURL().toString().toUpperCase());

            os = new DataOutputStream(phpServer.getOutputStream());

            int end = phpCode.indexOf('?');
            String phpFile = phpCode.substring(1,end);
            File file = new File(phpFile);
            System.out.println("SENT FILE "+phpFile.toUpperCase());
            os.writeBytes(phpFile);

            int response = connect.getResponseCode();
            if(Integer.toString(response).startsWith("4") || Integer.toString(response).startsWith("5")){
                in = new BufferedReader(new InputStreamReader(connect.getErrorStream()));
            }else{
                in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            }

            String aux = "";
            while((aux = in.readLine()) != null){
                backToServer.writeBytes(aux);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    private void sendCss(File file,BufferedReader in,DataOutputStream os){
        try{
            String content = "Content-type: "+"html/css"+"\r\n";
            String status = "HTTP/1.1 200 OK"+"\r\n";

            FileInputStream fis = new FileInputStream(file);

            byte[]buffer = new byte[1024];
            int bytes = 0;

            os.writeBytes(status);
            os.writeBytes(content);
            os.writeBytes("\r\n");

            while((bytes = (fis.read(buffer))) != -1){
                os.write(buffer,0,bytes);
            }
            //os.writeBytes(command.toUpperCase());
            fis.close();
            StringBuffer response = new StringBuffer();
            String aux = "";
            while((aux = in.readLine()).length() != 0){
                response.append(aux);
            }
            System.out.println("SERVER RESPONSE IS "+response);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public String toString(){
        DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date cal = new Date();
        time = format.format(cal);

        String ret = "id=0 with service '"+service+"' and time stamp "+time;
        return ret;
    }
}
