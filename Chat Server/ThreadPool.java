import com.sun.org.apache.bcel.internal.ExceptionConstants;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.*;
import javax.xml.transform.Source;

public class ThreadPool {
    int maxCapacity;
    int totalThreads;
    int workerThreads;

    Worker[] pool;
    MainWorker[]mainPool;

    Boolean stopped;
    Boolean mainStopped;

    final int DEFAULT = 5;
    final Object checkLock = new Object();

    CustomMonitor jobQueue;
    CustomMonitor mainQueue;

    Socket phpServer;

    String type;

    int lowCap;
    int highCap;
    int waitTime;

    ConcurrentHashMap<String,Integer>chatUsers;
    ConcurrentHashMap<Integer,ArrayList<Object>>chatOutputs;
    ConcurrentHashMap<Integer,LinkedList<String>>response;
    ConcurrentHashMap<Integer,DatagramSocket>voiceUsers;
    LinkedBlockingQueue<String>messages;
    LinkedBlockingQueue<DatagramPacket>voices;

    Queue<DatagramSocket>voiceConnections;

    SourceDataLine source;

    protected class Worker extends Thread{
        private volatile boolean end = false;
        public void end(){
            end = true;
            interrupt();
        }
        public void run(){
            try{
                CustomJob task;

                while(!end){
                    synchronized (jobQueue){
                        while(jobQueue.isEmpty()){
                            try{
                                jobQueue.wait();
                            }catch(InterruptedException e){
                                return;
                            }
                        }
                        task = jobQueue.get();
                    }
                    if(task != null){
                        try{
                            System.out.println("about to run task from queue");
                            task.task.run();
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }catch(Exception e){
                return;
            }
        }
    }
    protected class MainWorker extends Thread{
        private volatile boolean end = false;
        public void end(){
            end = true;
            interrupt();
        }
        public void run(){
            try{
                CustomJob task;

                while(!end){
                    synchronized (mainQueue){
                        while(mainQueue.isEmpty()){
                            try{
                                mainQueue.wait();
                            }catch(InterruptedException e){
                                return;
                            }
                        }
                        task = mainQueue.get();
                    }

                    if(task != null){
                        try{
                            Thread.currentThread().setName(Integer.toString(task.clientNumber));
                            System.out.println("about to run task from queue");
                            task.task.run();
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }catch(Exception e){
                return;
            }
        }
    }

    public ThreadPool(int capacity,int totalThreads){
        maxCapacity = capacity;
        this.totalThreads = totalThreads;

        pool = new Worker[totalThreads];
        mainPool = new MainWorker[totalThreads];

        stopped = true;
        mainStopped = true;

        jobQueue = new CustomMonitor();
        mainQueue = new CustomMonitor();

        set(5,10,500);

        for(int i=0;i<totalThreads;i++){
            pool[i] = new Worker();
            mainPool[i] = new MainWorker();
        }
        chatUsers = new ConcurrentHashMap<>();
        chatOutputs = new ConcurrentHashMap<>();
        messages = new LinkedBlockingQueue<>();
        response = new ConcurrentHashMap<>();
        voiceUsers = new ConcurrentHashMap<>();
        voices = new LinkedBlockingQueue<>();
        voiceConnections = new LinkedList<>();

        DatagramSocket available = null;
        for(int i=0;i<5;i++){
            try{
                available = new DatagramSocket();
            }catch(Exception e){
                available = null;
            }
            if(available != null){
                voiceConnections.offer(available);
            }
        }
        startTimer();
        startResponder();
        //startMainPool();
    }
    public ThreadPool(){this(50,5);this.type = "default";}
    public ThreadPool(Socket phpServer){
        this(50,5);
        this.phpServer = phpServer;
        this.type = "default";
    }
    public ThreadPool(String poolType){
        this(50,5);
        type = poolType;
    }

    public void startMainPool(){
        for(int i=0;i<mainPool.length;i++){
            if(mainPool[i] == null)continue;

            if(!mainPool[i].isAlive()){
                mainPool[i].start();
            }
        }
        System.out.println("main thread pool started");
    }
    public void startPool(){
        if(stopped){
            for(int i=0;i<pool.length;i++){
                if(pool[i] == null)continue;

                if(!pool[i].isAlive() && pool[i].getState() == Thread.State.NEW){
                    try{
                        pool[i].start();
                    }catch(IllegalThreadStateException e){
                        System.out.println("Can't start thread");
                    }
                }
            }
            stopped = false;
            System.out.println("ThreadPool started");
        }
    }
    public void stopMainPool(){
        int index = Math.min(mainPool.length,totalThreads);
        for(int i=0;i<index;i++){
            if(mainPool[i] == null)continue;
            if(mainPool[i].getId() == Thread.currentThread().getId()){
                mainPool[i].end();
            }
        }
    }
    public void stopPool(){
        if(!stopped){
            int index = Math.min(pool.length,workerThreads);
            for(int i=0;i<index;i++){
                if(pool[i] == null)continue;

                System.out.println("terminating thread"+i);
                pool[i].end();
            }
            stopped = true;
            System.out.println("terminated all threads");
        }
        //System.exit(0);
    }

    public void startTimer(){
        TimerTask timer = new TimerTask() {
            @Override
            public void run() {
                System.out.println("\n~~~~~~~~~~Starting Thread Manager~~~~~~~~~~");
                setMainThreads();
                setTotalThreads();
                System.out.println("THE NUMBER OF CHAT USER IS: "+chatOutputs.mappingCount());
                if(!stopped && numMainThreadsRunning() == 0 && jobQueue.isEmpty() && mainQueue.isEmpty()){
                    stopPool();
                    stopMainPool();
                    System.exit(0);
                }
                System.out.println("~~~~~~~~~~Thread Manager complete~~~~~~~~~~\n");
            }
        };
        Timer time = new Timer(true);
        time.schedule(timer,0,waitTime);
    }
    public void startResponder(){
        Runnable chatResponder = ()-> {
            while(true){
                tellAll();
            }
        };
        Thread thread = new Thread(chatResponder);
        thread.start();
    }

    public void play(InputStream is){
        try{
            AudioFormat format = new AudioFormat(44100,16,2,true,false);
            if(!source.isOpen())source.open(format);

            if(source.isActive()){
                System.out.println("ACTIVE SOURCE");
            }else{
                System.out.println("INACTIVE SOURCE");
            }
            source.addLineListener(event ->  {
                if(event.getType() == LineEvent.Type.STOP){
                    System.out.println("FINISHED RECEIVING AUDIO");
                }else if(event.getType() == LineEvent.Type.START){
                    System.out.println("STARTING TO RECEIVE AUDIO");
                }else {
                    System.out.println("NO INPUT RECEIVED");
                }
            });

            System.out.println("INPUT RECEIVED");
            byte[]buffer = new byte[source.getBufferSize()/5];
            int read;

            AudioInputStream audio = new AudioInputStream(is,format,buffer.length);
            while((read = audio.read(buffer,0,buffer.length)) != -1){
                if(read != 0){
                    source.write(buffer,0,buffer.length);
                    System.out.println("READING FROM SOURCE "+(byte)read);
                }
                //AudioSystem.write(audio,AudioFileFormat.Type.WAVE,file);
            }
            source.drain();
            //audio.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public synchronized void tellAll(){
        try{
            String message = messages.take();
            System.out.println("CONSUMED AND DISTRIBUTING MESSAGE: "+message);
            chatOutputs.forEachValue(10,(ArrayList<Object> p)->{
                ((PrintWriter)p.get(0)).println(message);
                ((PrintWriter)p.get(0)).flush();
            });
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    public synchronized void streamToAll(){
        try{
            DatagramPacket packet = voices.take();
            System.out.println("CONSUMED AND DISTRIBUTING PACKET OF SIZE "+packet.getLength());
            voiceUsers.forEachValue(10,(DatagramSocket s)->{
                try{
                    s.send(packet);
                }catch(IOException i){
                    i.printStackTrace();
                }
            });
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    public void submitVoiceChatServer(Socket socket,DatagramSocket datagramSocket,DataInputStream is,int client){
        try{
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            Runnable voiceServer = ()->{
                try{
                    AudioFormat format = new AudioFormat(44100,16,2,true,false);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class,format);
                    source = (SourceDataLine)AudioSystem.getLine(info);
                    source.open(format);

                    source.start();

                    System.out.println("BEGINNING TO STREAM VOICE SERVICES");
                    while(true){
                        play(is);
                        //System.out.println("PLAYING");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            };
            CustomJob job = new CustomJob(client,voiceServer);
            if(!voiceUsers.containsKey(client)){
                voiceUsers.put(client,datagramSocket);
            }

            synchronized (mainQueue){
                if(mainQueue.size() < 50){
                    mainQueue.add(job);
                }else{
                    out.println("Sorry busy server please try again later");
                    socket.close();
                }
            }
            setMainThreads();
        }catch(Exception e){
            e.printStackTrace();
        }
        if(mainStopped){
            startMainPool();
            mainStopped = false;
        }
    }
    public void submitChatServerTCP(Socket socket,int client){
        try{
            final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            final BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
            final DataInputStream is = new DataInputStream(socket.getInputStream());
            final DataOutputStream os = new DataOutputStream(socket.getOutputStream());

            Runnable chatServer = ()->{
                try{
                    out.println("Hello welcome to the chat server please enter your user name");
                    final String userName = in.readLine().toUpperCase();
                    if(!chatUsers.containsKey(userName)){
                        chatUsers.put(userName,client);
                        System.out.println("PLACED INTO USER LIST "+userName);
                    }
                    out.println("You may begin chatting "+userName+">>\n");
                    out.println(userName);

                    String input;
                    String directChatRecipient = "";
                    boolean streaming = false;
                    boolean directChat = false;

                    while(true){
                        input = in.readLine();
                        if(input == null || input.isEmpty())continue;
                        if(input.equalsIgnoreCase("quit")){
                            out.println("Bye user#"+userName+"!");
                            out.println(".@kill");
                            chatOutputs.remove(client);
                            chatUsers.remove(userName);
                            break;
                        }
                        if(input.startsWith("@!")){
                            directChat = false;
                            continue;
                        }
                        if(input.startsWith("@@")){//@@-user
                            String[]parse = input.split("-");
                            directChatRecipient = parse[1].toUpperCase();
                            directChat = true;
                            continue;
                        }
                        if(directChat){
                            String message = "[Priv] "+userName+": "+input;
                            out.println(message);
                            ((PrintWriter)chatOutputs.get(chatUsers.get(directChatRecipient)).get(0)).println(message);
                            continue;
                        }
                        if(input.equalsIgnoreCase("%list")){
                            chatUsers.forEachKey(10,(String s)->{
                                try{
                                    messages.put("\tUser:"+s);
                                }catch(InterruptedException e){
                                    e.printStackTrace();
                                }
                            });
                            continue;
                        }
                        if(input.equalsIgnoreCase("%stream")){
                            if(streaming)continue;
                            DatagramSocket toConnect = new DatagramSocket(9999);

                            out.println(input);
                            out.println(toConnect.getLocalSocketAddress());
                            out.println(toConnect.getPort());

                            submitVoiceChatServer(socket,toConnect,is,client);

                            streaming = true;
                            System.out.println("SUBMITTING VOICE SERVICES TO JOB QUEUE");
                            continue;
                        }
                        if(input.startsWith("%@")){//%@-name-message
                            String[]parse = input.split("-");
                            String name = parse[1];
                            String message = parse[2];
                            System.out.println(name+": "+message);
                            ((PrintWriter)chatOutputs.get(chatUsers.get(name.toUpperCase())).get(0)).println("[Priv] "+userName+": "+message);
                            continue;
                        }

                        if(input.startsWith("%*file ")){
                            String fileName = input.substring("%*file ".length());
                            String recipient = "";
                            File file = new File("serverCopy-"+fileName);
                            FileOutputStream writer = new FileOutputStream(file);

                            int length = Integer.parseInt(in.readLine());
                            recipient = in.readLine();

                            System.out.println("SENDING TO "+recipient.toUpperCase());
                            if(length > 0){
                                byte[]inFile = new byte[length];
                                byte[]aux = inFile;

                                ((PrintWriter)chatOutputs.get(chatUsers.get(recipient.toUpperCase())).get(0)).println("[file] "+userName+": accept file "+file.getName());
                                ((PrintWriter)chatOutputs.get(chatUsers.get(recipient.toUpperCase())).get(0)).println(length);

                                is.readFully(inFile);
                                writer.write(inFile);

                                BufferedInputStream fileReader = new BufferedInputStream(new FileInputStream(file));
                                fileReader.read(aux,0,aux.length);
                                ((DataOutputStream)chatOutputs.get(chatUsers.get(recipient.toUpperCase())).get(1)).write(aux,0,aux.length);
                                fileReader.close();
                                file.deleteOnExit();
                                System.out.println("GETTING FILE DONE FILE SIZE IS "+file.length());
                                continue;
                            }
                        }
                        input = userName+": "+input;
                        System.out.println("AT THE START OF THE LOOP WITH MESSAGE: "+input);
                        try{
                            messages.put(input);
                            System.out.println("PLACED MESSAGE");
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }

                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            };

            CustomJob job = new CustomJob(client,chatServer);
            if(!response.containsKey(client)){
                response.put(client,new LinkedList<>());
            }
            if(!chatOutputs.containsKey(client)){
                ArrayList<Object>streams = new ArrayList<>();
                streams.add(0,out);//printwriter
                streams.add(1,os);//inputstream

                chatOutputs.put(client,streams);
                System.out.println("ADDED TO USERS");
            }

            System.out.println("submitting main and periodic checker job to mainPool");

            synchronized (mainQueue){
                if(mainQueue.size() < 50){
                    mainQueue.add(job);
                }else{
                    out.println("Sorry busy server please try again later");
                    socket.close();
                }
            }
            setMainThreads();
        }catch(IOException e){
            e.printStackTrace();
        }
        if(mainStopped){
            startMainPool();
            mainStopped = false;
        }
    }
    public void submitChatServerMulticast(Socket socket,int client){
        Runnable chatServer = ()->{
            try{
                BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                MulticastSocket multicastSocket = new MulticastSocket(socket.getPort());
                multicastSocket.joinGroup(socket.getInetAddress());

                out.println("Hello welcome to the chat server you are client# "+client);

                while(true){
                    String input = in.readLine();
                    if(input.equalsIgnoreCase("quit")){
                        multicastSocket.leaveGroup(socket.getInetAddress());
                        multicastSocket.close();
                        break;
                    }
                    input = "Client# "+client+": "+input;
                    DatagramPacket packet = new DatagramPacket(input.getBytes(),input.length(),socket.getInetAddress(),socket.getPort());
                    multicastSocket.send(packet);

                    byte[] buffer = new byte[1027];
                    DatagramPacket returnPacket = new DatagramPacket(buffer,buffer.length,socket.getInetAddress(),socket.getPort());
                    multicastSocket.receive(returnPacket);

                    String reply = new String(buffer,0,returnPacket.getLength(),"UTF-8");

                    if(!reply.startsWith("Client# "+client+":")){
                        out.println(reply);
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        };

        try{
            BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            CustomJob job = new CustomJob(socket,in,out,client,chatServer);

            System.out.println("submitting main and periodic checker job to mainPool");

            synchronized (mainQueue){
                if(mainQueue.size() < 50){
                    mainQueue.add(job);
                }else{
                    out.println("Sorry busy server please try again later");
                    socket.close();
                }
            }
            setMainThreads();
        }catch(IOException e){
            e.printStackTrace();
        }
        if(mainStopped){
            startMainPool();
            mainStopped = false;
        }
    }
    public void submitDefault(Socket socket,int client){
        Runnable main = ()->{
            try{
                BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());

                System.out.println("Running main with total threads at "+numMainThreadsRunning());

                out.println("Hello, you are client #" + client + ".");
                out.println("enter your commands above [kill] to quit\n");

                CustomJob toDo = null;
                boolean isWeb = false;
                while (true) {
                    String input = in.readLine();
                    if(input.contains("GET")){
                        String aux = input;
                        isWeb = true;
                        while(!aux.contains("Cookie:")){
                            aux = in.readLine();
                            System.out.println(aux);
                            //input += aux;
                        }
                    }
                    if(input.equalsIgnoreCase("kill")){
                        socket.close();
                        if(numMainThreadsRunning() == 0){
                            stopPool();
                            stopMainPool();
                            System.exit(0);
                        }
                        break;
                    }else{
                        toDo = new CustomJob(socket,phpServer,in,os,out,client,input);
                        if(toDo != null){
                            System.out.println("submitting job "+toDo.toString());
                            synchronized (jobQueue){
                                jobQueue.add(toDo);
                            }
                        }

                        if(stopped){
                            startPool();
                        }
                    }
                    //input = "";
                }
            }catch(IOException e){
                e.printStackTrace();
                //stopPool();
            }finally {
                try{
                    socket.close();
                    //stopPool();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        };
        try{
            BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            CustomJob job = new CustomJob(socket,in,out,client,main);

            System.out.println("submitting main and periodic checker job to mainPool");

            synchronized (mainQueue){
                if(mainQueue.size() < 50){
                    mainQueue.add(job);
                }else{
                    out.println("Sorry busy server please try again later");
                    socket.close();
                }
            }
            setMainThreads();
        }catch(IOException e){
            e.printStackTrace();
        }
        if(mainStopped){
            startMainPool();
            mainStopped = false;
        }
    }

    public void submit(Socket socket,int client){
        if(type.equalsIgnoreCase("default")){
            submitDefault(socket,client);
        }else if(type.equalsIgnoreCase("multicast")){
            submitChatServerMulticast(socket,client);
        }else if(type.equalsIgnoreCase("chat")){
            submitChatServerTCP(socket,client);
        }
    }
    public int numThreadsRunning(){
        synchronized (checkLock){
            int count = 0;
            for(int i=0;i<pool.length;i++){
                if(pool[i] == null)continue;

                if(pool[i].getState() != Thread.State.WAITING && pool[i].getState() != Thread.State.BLOCKED)count++;
            }
            return count;
        }
    }
    public int numMainThreadsRunning(){
        synchronized (checkLock){
            int count = 0;
            for(int i=0;i<mainPool.length;i++){
                if(mainPool[i] == null)continue;

                if(mainPool[i].getState() != Thread.State.WAITING && mainPool[i].getState() != Thread.State.BLOCKED)count++;
            }
            return count;
        }
    }

    public void setTotalThreads(){
        synchronized (checkLock){
            System.out.println("=================CHECKING AUX THREADS==================");
            System.out.println("The number of elements in the auxiliary queue is "+jobQueue.size());
            int temp = totalThreads;
            if(numThreadsRunning() != 0 && jobQueue.size() >= 1){
                if(jobQueue.size() < lowCap && workerThreads != DEFAULT){
                    workerThreads = DEFAULT;
                    System.out.println("ThreadPool has set the number of workers the thread count is "+workerThreads);
                    System.out.println("The number of auxiliary threads in use is "+numThreadsRunning());
                }else if(jobQueue.size() > lowCap && jobQueue.size() < highCap && workerThreads != lowCap+5){
                    workerThreads = lowCap+5;
                    System.out.println("ThreadPool has increased the number of workers the thread count is "+workerThreads);
                    if(numThreadsRunning() < workerThreads/2){
                        workerThreads /= 2;
                        System.out.println("The number of auxiliary threads in use is "+numThreadsRunning());
                        System.out.println("ThreadPool has decreased the number of workers the thread count is "+workerThreads);
                    }
                }else if(jobQueue.size() > highCap && jobQueue.size() < maxCapacity && workerThreads != highCap+5){
                    workerThreads = highCap+5;
                    System.out.println("ThreadPool has increased the number of workers the thread count is "+workerThreads);
                    if(numThreadsRunning() < workerThreads/2){
                        workerThreads /= 2;
                        System.out.println("The number of auxiliary threads in use is "+numThreadsRunning());
                        System.out.println("ThreadPool has decreased the number of workers the thread count is "+workerThreads);
                    }
                }else{
                    if(numThreadsRunning()  > workerThreads/2){
                        workerThreads *= 2;
                        if(workerThreads > maxCapacity){
                            workerThreads = maxCapacity;
                        }
                        System.out.println("ThreadPool has increased the number of auxiliary threads the thread count is "+workerThreads);
                    }else{
                        System.out.println("STABLE POOL");
                    }
                    if((numThreadsRunning() < jobQueue.size()/2) || (numThreadsRunning() < workerThreads/4)){
                        workerThreads /= 2;
                        if(workerThreads <= 2){
                            workerThreads = 5;
                        }
                        System.out.println("The number of auxiliary threads in use is "+numThreadsRunning());
                        System.out.println("ThreadPool has decreased the number of threads the thread count is "+workerThreads);
                    }
                }
                if(temp != workerThreads){
                    System.out.println("re-assigning auxiliary pool");
                    Worker[]aux = new Worker[workerThreads];
                    int index = Math.min(pool.length,workerThreads);
                    for(int i=0;i<index;i++){
                        aux[i] = pool[i];
                    }
                    pool = new Worker[workerThreads];
                    pool = aux;

                    //startPool();
                }
            }
            System.out.println("======================COMPLETE=========================");
        }
    }
    public void setMainThreads(){
        synchronized (checkLock){
            int temp = totalThreads;
            System.out.println("=================CHECKING MAIN THREADS==================");
            System.out.println("--The number of elements in the main queue is "+mainQueue.size()+"--");
            if(mainQueue.size() != 0 || numMainThreadsRunning() > 1){
                if(mainQueue.size() < lowCap && totalThreads != DEFAULT){
                    totalThreads = DEFAULT;
                    System.out.println("--ThreadPool has set the number of main threads the thread count is "+totalThreads+"--");
                    System.out.println("--The number of threads in use is "+numMainThreadsRunning()+"--");
                }else if(mainQueue.size() > lowCap && mainQueue.size() < highCap && totalThreads != lowCap+5){
                    totalThreads = lowCap+5;
                    System.out.println("--ThreadPool has increased the number of main threads the thread count is "+totalThreads+"--");
                    if(numMainThreadsRunning() < mainQueue.size()/2){
                        totalThreads /= 2;
                        System.out.println("--The number of threads in use is "+numMainThreadsRunning()+"--");
                        System.out.println("--ThreadPool has decreased the number of main threads the thread count is "+totalThreads+"--");
                    }
                }else if(mainQueue.size() > highCap && mainQueue.size() < maxCapacity && totalThreads != highCap+5){
                    totalThreads = highCap+5;
                    System.out.println("--ThreadPool has increased the number of main threads the thread count is "+totalThreads+"--");
                    if(numMainThreadsRunning() < mainQueue.size()/2){
                        totalThreads /= 2;
                        System.out.println("--The number of threads in use is "+numMainThreadsRunning()+"--");
                        System.out.println("--ThreadPool has decreased the number of main threads the thread count is "+totalThreads+"--");
                    }
                }else{
                    if(numMainThreadsRunning()  > totalThreads/2){
                        totalThreads *= 2;
                        if(totalThreads > maxCapacity){
                            totalThreads = maxCapacity;
                        }
                        System.out.println("--ThreadPool has increased the number of main threads the thread count is "+totalThreads+"--");
                    }else{
                        System.out.println("--STABLE POOL--");
                    }
                    if((numMainThreadsRunning() < mainQueue.size()/2) || (numMainThreadsRunning() < totalThreads/4)){
                        totalThreads /= 2;
                        if(totalThreads <= 2){
                            totalThreads = 5;
                        }
                        System.out.println("--The number of main threads in use is "+numMainThreadsRunning()+"--");
                        System.out.println("--ThreadPool has decreased the number of threads the thread count is "+totalThreads+"--");
                    }
                }
                if(temp != totalThreads){
                    System.out.println("--re-assigning main pool--");
                    MainWorker[]aux = new MainWorker[totalThreads];
                    int index = Math.min(mainPool.length,totalThreads);
                    for(int i=0;i<index;i++){
                        if(mainPool[i] == null)continue;

                        aux[i] = mainPool[i];
                    }
                    mainPool = new MainWorker[aux.length];
                    for(int i=0;i<mainPool.length;i++){
                        mainPool[i] = aux[i];
                    }
                    startMainPool();
                }
            }
            System.out.println("======================COMPLETE=========================");
        }
    }

    public void set(int low,int high,int wait){
        this.lowCap = low;
        this.highCap = high;
        this.waitTime = wait;
    }
}