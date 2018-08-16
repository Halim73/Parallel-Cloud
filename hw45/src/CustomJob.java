import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomJob{
    String service;
    String time;
    Runnable task;

    Socket socket;

    int clientNumber;

    public CustomJob(Socket socket, BufferedReader in, PrintWriter out, int client, String service){
        this.clientNumber = client;
        this.service = service;

        this.socket = socket;

        DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        time = format.format(date);

        if(service.equalsIgnoreCase("kill")){
            return;
        }else if(service.matches("[a-zA-z]+")) {
            this.service = "capitalize,"+service;
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
            }else{
                task = errorMsg(in,out,clientNumber);
            }
        }
    }
    public CustomJob(Socket socket,BufferedReader in,PrintWriter out,int client,Runnable task){
        this(socket,in,out,client,"'main'");
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
    public String toString(){
        DateFormat format =  new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date cal = new Date();
        time = format.format(cal);

        String ret = "id=0 with service '"+service+"' and time stamp "+time;
        return ret;
    }
}
