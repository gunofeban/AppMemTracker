package sslab.lova.mem_monitor;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by JEFF on 2015-11-09.
 */
public class BeforeKillClass implements Serializable {
    long time;
    ArrayList<String> list;
    ArrayList<String> deadList;
    int originalListSize;
    int deadListSize;
    boolean isFinish;

    public BeforeKillClass(long time, ArrayList<String> list){
        this.time = time;
        this.list = list;
        this.isFinish = false;
        this.originalListSize = list.size();
        this.deadListSize = 0;
        deadList = new ArrayList<String>();
    }

    public boolean isEmpty(){
        boolean flag;

        if(list.size()>0){
            flag = false;
        }else{
            flag = true;
        }
        return flag;
    }

    public void setList( ArrayList<String> list){
        this.list = list;
    }

    public long getTime(){
        return time;
    }
    public ArrayList<String> getList(){
        return list;
    }

    public boolean getIsFinish(){
        return isFinish;
    }

    public void setIsFinish(boolean isFinish){
        this.isFinish = isFinish;
    }

    public void setDeadListSize(int size){ this.deadListSize += size; }

    public int getDeadListSize(){ return deadListSize; }

}