import java.util.ArrayDeque;
import java.util.Queue;

public class CustomMonitor {
    protected Queue<CustomJob> jobQueue;

    final int MAX = 50;

    public CustomMonitor(){
        jobQueue = new ArrayDeque<>();
    }

    public void add(CustomJob job) {
        if(jobQueue.size() < MAX){
            if(jobQueue.offer(job)){
                System.out.println("Monitor successfully submitted job "+job.toString());
            }else{
                System.out.println("Monitor failed to submit job "+job.toString());
            }
        }else{
            System.out.println("Full job queue now waiting");
        }
        notify();
    }
    public CustomJob get(){
        CustomJob ret = jobQueue.poll();
        if(ret != null){
            System.out.println("Monitor successfully retrieved job "+ret.toString());
        }else{
            System.out.println("Monitor failed to retrieve job ");
        }
        notify();
        return ret;
    }
    public boolean isEmpty(){return jobQueue.isEmpty();}
    public int size(){
        return jobQueue.size();
    }
}
