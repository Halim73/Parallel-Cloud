import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

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

    int lowCap;
    int highCap;
    int waitTime;

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
                    /*try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }*/
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
        startTimer();
        //startMainPool();
    }
    public ThreadPool(){this(50,5);}

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
    public void submit(Socket socket,int client){
        Runnable main = ()->{
            try{
                BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                System.out.println("Running main with total threads at "+numMainThreadsRunning());

                out.println("Hello, you are client #" + client + ".");
                out.println("enter your commands above [kill] to quit\n");

                CustomJob toDo = null;
                while (true) {
                    String input = in.readLine();
                    if(input.equalsIgnoreCase("kill")){
                        socket.close();
                        if(numMainThreadsRunning() == 0){
                            stopPool();
                            stopMainPool();
                            System.exit(0);
                        }
                        break;
                    }else{
                        toDo = new CustomJob(socket,in,out,client,input);
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