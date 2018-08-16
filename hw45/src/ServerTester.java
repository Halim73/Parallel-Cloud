import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerTester {
    public static void main(String[]args){
        if(args.length == 0){
            Thread[]threads = new Thread[100];
            String type = "single";

            Runnable single = ()->{
                try{
                    Socket socket = new Socket("localhost",9898);

                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter output = new PrintWriter(socket.getOutputStream(),true);

                    for(int i=0;i<3;i++){
                        String in = input.readLine();
                        if(in == null){
                            return;
                        }
                        System.out.println("from thread "+Thread.currentThread().getName()+": "+in);
                    }
                    System.out.println("from thread "+Thread.currentThread().getName()+": finished consuming initial message");
                    output.println("hello");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("kill");
                    socket.close();
                    return;
                }catch(IOException e){
                    System.out.println(Thread.currentThread().getName()+" couldn't connect to the server");
                    //e.printStackTrace();
                    return;
                }
            };
            Runnable multi = ()->{
                try{
                    Socket socket = new Socket("localhost",9898);

                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter output = new PrintWriter(socket.getOutputStream(),true);

                    for(int i=0;i<3;i++){
                        System.out.println("from thread "+Thread.currentThread().getName()+": "+input.readLine());
                        if(!socket.isConnected()){
                            return;
                        }
                    }
                    System.out.println("from thread "+Thread.currentThread().getName()+": finished consuming initial message");
                    output.println("hello");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("add,4,5");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("sub,8,2");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("mul,5,7");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("div,8,2");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("kill");
                    socket.close();
                    return;
                }catch(IOException e){
                    System.out.println(Thread.currentThread().getName()+" couldn't connect to the server");
                    //e.printStackTrace();
                    return;
                }
            };
            Runnable erroneous = ()->{
                try{
                    Socket socket = new Socket("localhost",9898);

                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter output = new PrintWriter(socket.getOutputStream(),true);

                    for(int i=0;i<3;i++){
                        System.out.println("from thread "+Thread.currentThread().getName()+": "+input.readLine());
                        if(!socket.isConnected()){
                            return;
                        }
                    }
                    System.out.println("from thread "+Thread.currentThread().getName()+": finished consuming initial message");
                    output.println("nocomman,5,6");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("add,3,");
                    System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                    output.println("kill");
                    socket.close();
                    return;
                }catch(IOException e){
                    System.out.println(Thread.currentThread().getName()+" couldn't connect to the server");
                    //e.printStackTrace();
                    return;
                }
            };

            if(type.equalsIgnoreCase("single")){
                test(threads,single);
            }else if(type.equalsIgnoreCase("multi")){
                test(threads,multi);
            }else{
                test(threads,erroneous);
            }
        }else{
            if(args.length == 2){
                Thread[]threads = new Thread[Integer.parseInt(args[0])];
                String type = args[1];

                Runnable single = ()->{
                    try{
                        Socket socket = new Socket("localhost",9898);

                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter output = new PrintWriter(socket.getOutputStream(),true);

                        for(int i=0;i<3;i++){
                            String in = input.readLine();
                            if(in == null){
                                return;
                            }
                            System.out.println("from thread "+Thread.currentThread().getName()+": "+in);
                        }
                        System.out.println("from thread "+Thread.currentThread().getName()+": finished consuming initial message");
                        output.println("hello");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("kill");
                        socket.close();
                        return;
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                };
                Runnable multi = ()->{
                    try{
                        Socket socket = new Socket("localhost",9898);

                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter output = new PrintWriter(socket.getOutputStream(),true);

                        for(int i=0;i<3;i++){
                            System.out.println("from thread "+Thread.currentThread().getName()+": "+input.readLine());
                            if(!socket.isConnected()){
                                return;
                            }
                        }
                        System.out.println("from thread "+Thread.currentThread().getName()+": finished consuming initial message");
                        output.println("hello");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("add,4,5");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("sub,8,2");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("mul,5,7");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("div,8,2");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("kill");
                        socket.close();
                        return;
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                };
                Runnable erroneous = ()->{
                    try{
                        Socket socket = new Socket("localhost",9898);

                        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter output = new PrintWriter(socket.getOutputStream(),true);

                        for(int i=0;i<3;i++){
                            System.out.println("from thread "+Thread.currentThread().getName()+": "+input.readLine());
                            if(!socket.isConnected()){
                                return;
                            }
                        }
                        System.out.println("from thread "+Thread.currentThread().getName()+": finished consuming initial message");
                        output.println("nocomman,5,6");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("add,3,");
                        System.out.println("from "+Thread.currentThread().getName()+": "+input.readLine());
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                        output.println("kill");
                        socket.close();
                        return;
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                };

                if(type.equalsIgnoreCase("single")){
                    test(threads,single);
                }else if(type.equalsIgnoreCase("multi")){
                    test(threads,multi);
                }else{
                    test(threads,erroneous);
                }
            }else{
                System.out.println("Error! Wrong Usage -> [numThreads][commandType]");
                System.out.println("commandType = 'single - sends single command','multi - sends multiple commands','err - sends erroneous input'");
            }
        }

    }
    public static void test(Thread[]threads,Runnable toDo){
        try{
            for(int i=0;i<threads.length;i++){
                threads[i] = new Thread(toDo);
                threads[i].start();
                //Thread.sleep(500);
            }

            for(int i=0;i<threads.length;i++){
                if(threads[i] == null)continue;
                threads[i].join();
            }
            System.out.println("All users have finished");
            System.exit(0);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
