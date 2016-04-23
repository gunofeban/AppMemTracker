package sslab.lova.mem_monitor;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by JEFF on 2015-11-06.
 */

public class TopViewService extends Service {
    private View mViewGroup;
    private long threshold;
    private ActivityManager activityManager;
    private ActivityManager.MemoryInfo memoryInfo;
    private boolean firstStart = true;
    String curPackage, prevPackage;
    String curAppLabel, prevAppLabel;
    private Context mContext;

    private Map<String,AppInfoClass> RTMap  = new HashMap<String,AppInfoClass>();

    long curTime=0;
    long prevTime=0;
    private boolean isScreenOn=true;
    long executionTimeLong=0;
    long exeTime;

    private Thread.UncaughtExceptionHandler mUncaughtExceptionHandler;
    private Calendar calendar;
    private String[] mapKeySet;

    List<ActivityManager.RunningAppProcessInfo> curList, prevList;

    ArrayList<String> beforeKillList;
    ArrayList<String> deadList;

    BeforeKillClass bkc;
    long beforeKillTime;

    boolean isKilling = false;

    ArrayList<String> autoCreateList;
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();

        calendar = Calendar.getInstance();

        mContext = this;
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        Log.d("LOTTE", "TopViewService onCreate 호출");

        init();

        registerComponentCallbacks(mComponentCallbacks);

//        createTopView();

        Thread t1 = new Thread(new Runnable() {
            public void run() {
                detectAutoCreate();
            }
        });
        t1.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub

        if(intent != null){
            if(intent.getBooleanExtra("isKillingServiceCall",false)){
                Log.d("LOTTE", "isKillingService 에서 TopViewService onStartCommand 호출");

                beforeKillList = intent.getStringArrayListExtra("currentProcessList");
                beforeKillTime = System.currentTimeMillis();
                isKilling = false;

                bkc = new BeforeKillClass(beforeKillTime, beforeKillList);

                Log.i("LOTTE", "---------------- before --------------------------");
                for (int k=0;k<beforeKillList.size();k++) {
                    Log.i("LOTTE", beforeKillList.get(k));
                }
                Log.i("LOTTE", "beforeKillTime" + beforeKillTime);
                Log.i("LOTTE", "---------------- before --------------------------");

                try{
                    FileOutputStream fileout = new FileOutputStream(Environment.getExternalStorageDirectory()+"/SSLAB/bkc.ser");
                    ObjectOutputStream objOut = new ObjectOutputStream(fileout);
                    objOut.writeObject(bkc);
                    objOut.close();
                    fileout.close();

                }catch(Exception e)
                {
                    e.printStackTrace();
                }
            }else if(intent.getBooleanExtra("isBootCompleteReceiver",false)){
                isKilling = true;
            }else{
                Log.d("LOTTE", "Receiver에서 TopViewService onStartCommand 호출");
                try{
                    if(!isKilling){
                        Log.d("LOTTE", "여기 나오니 ??");
                        FileInputStream fileIn =  new FileInputStream(Environment.getExternalStorageDirectory()+"/SSLAB/bkc.ser");
                        ObjectInputStream in = new ObjectInputStream(fileIn);
                        Log.d("LOTTE", "여기 나오니 ??");
                        Object obj = in.readObject();
                        bkc = (BeforeKillClass)obj;
                        in.close();
                        fileIn.close();

                        beforeKillList = bkc.getList();
                        beforeKillTime = bkc.getTime();
                        isKilling = bkc.getIsFinish();

                        Log.i("LOTTE", "---------------- before --------------------------");
                        for (int k=0;k<beforeKillList.size();k++) {
                            Log.i("LOTTE", beforeKillList.get(k));
                        }
                        Log.i("LOTTE", "beforeKillTime" + beforeKillTime);
                        Log.i("LOTTE", "---------------- before --------------------------");
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }else{
            Log.d("LOTTE", "자동 재시작!!");
            Log.d("LOTTE", "진짜 여기야 ????");

            try{
                if(!isKilling){
                    Log.d("LOTTE", "여기 나오니 ??");
                    FileInputStream fileIn =  new FileInputStream(Environment.getExternalStorageDirectory()+"/SSLAB/bkc.ser");
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    Log.d("LOTTE", "여기 나오니 ??");
                    Object obj = in.readObject();
                    bkc = (BeforeKillClass)obj;
                    in.close();
                    fileIn.close();

                    beforeKillList = bkc.getList();
                    beforeKillTime = bkc.getTime();
                    isKilling = bkc.getIsFinish();

                    Log.i("LOTTE", "---------------- before --------------------------");
                    for (int k=0;k<beforeKillList.size();k++) {
                        Log.i("LOTTE", beforeKillList.get(k));
                    }
                    Log.i("LOTTE", "beforeKillTime" + beforeKillTime);
                    Log.i("LOTTE", "---------------- before --------------------------");
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }


    public void init(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

        registerReceiver(screenOnOff,intentFilter);
        mUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                try {
                    ex.printStackTrace();
                    Log.d("LOTTE", "UnCaughtException 비정상 종료");
                    FileOutputStream fileout = new FileOutputStream(Environment.getExternalStorageDirectory() + "/SSLAB/RTMap.ser");
                    ObjectOutputStream objOut = new ObjectOutputStream(fileout);
                    objOut.writeObject(RTMap);
                    objOut.close();
                    fileout.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        try{
            FileInputStream fileIn =  new FileInputStream(Environment.getExternalStorageDirectory()+"/SSLAB/RTMap.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            RTMap = (Map<String,AppInfoClass>) in.readObject();
            in.close();
            fileIn.close();

        }catch(Exception e)
        {
            e.printStackTrace();
        }

        deadList = new ArrayList<String>();

        prevTime = System.currentTimeMillis();
        curTime = System.currentTimeMillis();

        firstStart = true;
        mContext=  this;
    }

    public synchronized void detectAutoCreate() {
        long startTime, endTime;
        long beforeMemorySize = 0;
        int memory = 0;
/**
 * TODO : API LEVEL 21 이하에서 Task 가져오기 수정
 *
 */

        List<ActivityManager.RunningAppProcessInfo> mRunningAppProcessInfo = activityManager.getRunningAppProcesses();
        List<ActivityManager.RecentTaskInfo> info= activityManager.getRecentTasks(1, Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.d("LOTTE", "onTouch");
        while(true) {
            if (firstStart) {
                if (Build.VERSION.SDK_INT < 20) {

                    ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.RunningTaskInfo> Info = am.getRunningTasks(1);
                    ComponentName topActivity = Info.get(0).topActivity;

                    curPackage = topActivity.getPackageName();
                    prevPackage = topActivity.getPackageName();


                    curAppLabel = Util.findPackageLabelByUid(mContext, Util.findUidByPackageName(mContext, curPackage));
                    prevAppLabel = Util.findPackageLabelByUid(mContext, Util.findUidByPackageName(mContext, curPackage));
                    curAppLabel = curAppLabel.replace("\n", "");
                    prevAppLabel = prevAppLabel.replace("\n","");
                } else {
                    curPackage = mRunningAppProcessInfo.get(0).processName;
                    prevPackage = mRunningAppProcessInfo.get(0).processName;
                    
                    curAppLabel = Util.findPackageLabelByUid(mContext, mRunningAppProcessInfo.get(0).uid);
                    prevAppLabel = Util.findPackageLabelByUid(mContext, mRunningAppProcessInfo.get(0).uid);
                }
                Log.d("LOTTE", curPackage);
                Log.d("LOTTE", prevPackage);

                curList = activityManager.getRunningAppProcesses();
                prevList = activityManager.getRunningAppProcesses();

                firstStart = false;
            } else {
                startTime = System.currentTimeMillis();
                endTime = startTime + 1;

                while ((endTime - startTime) < 400) {
                    mRunningAppProcessInfo = activityManager.getRunningAppProcesses();
                    if (Build.VERSION.SDK_INT < 20) {

                        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
                        List<ActivityManager.RunningTaskInfo> Info = am.getRunningTasks(1);
                        ComponentName topActivity = Info.get(0).topActivity;

                        prevPackage = curPackage;
                        curPackage = topActivity.getPackageName();

                        prevAppLabel = curAppLabel;
                        curAppLabel = Util.findPackageLabelByUid(mContext, Util.findUidByPackageName(mContext, curPackage));
//                        Log.d("LOTTE", curPackage);
//                        Log.d("LOTTE", prevPackage);
                    } else {
                        prevPackage = curPackage;
                        curPackage = mRunningAppProcessInfo.get(0).processName;

                        prevAppLabel = curAppLabel;
                        curAppLabel = Util.findPackageLabelByUid(mContext, mRunningAppProcessInfo.get(0).uid);
                    }

                    prevList.clear();
                    for (ActivityManager.RunningAppProcessInfo br : curList) {
                        prevList.add(br);
                    }

                    curList = activityManager.getRunningAppProcesses();

                    for (ActivityManager.RunningAppProcessInfo br : prevList) {
                        for (ActivityManager.RunningAppProcessInfo ar : mRunningAppProcessInfo) {

                            if (br.pid == ar.pid) {
                                mRunningAppProcessInfo.remove(ar);
                                break;
                            }
                        }
                    }
                    //-----------------------------------------------------------------------------------------
                    /**
                     * 현재 리스트에서 과거 리스트를 뺀거. 그럼 다시 살아난거임
                     */
                    if (mRunningAppProcessInfo.size() > 0) {
                        autoCreateList = new ArrayList<String>();
                        for (ActivityManager.RunningAppProcessInfo ar : mRunningAppProcessInfo) {
                            if (!ar.processName.equals(curPackage)) {
                                String tempLabel;
                                tempLabel =  Util.findPackageLabelByUid(mContext, ar.uid);
                                tempLabel = tempLabel.replace(System.getProperty("line.separator"), "");
                                autoCreateList.add(Util.dateToStringYMDHMS(System.currentTimeMillis()) + "/" + tempLabel + "/" + ar.processName + "/" + Util.importanceToString(ar.importance));
                            }
                        }
                        if (autoCreateList.size() != 0) {
                            Intent i = new Intent(mContext, CollectBroadcastMessageService.class);
                            i.putExtra("TopViewService", true);
                            i.putExtra("autoCreateList", autoCreateList);
                            startService(i);
                        }
                    }
                    //-----------------------------------------------------------------------------------------
                    /**
                     * 과거리스트-현재리스트
                     * 과거리스트가 남으면 현재리스트에서 앱이 종료된 것. LMK에 의해 종료됬는지 검사하기위해 만든 소스
                     */
                    for (ActivityManager.RunningAppProcessInfo br : curList) {
                        for (ActivityManager.RunningAppProcessInfo ar : prevList) {
                            if (br.pid == ar.pid) {
                                prevList.remove(ar);
                                break;
                            }
                        }
                    }
//
//                    Log.i("JEFF", "------------------------------------------");
//                    if (prevList.size() > 0) {
//                        Log.d("JEFF", "어플리케이션 죽음");
//                        for (ActivityManager.RunningAppProcessInfo pl : prevList) {
//                            Log.d("JEFF", pl.processName);
//                        }
//                    }
//                    Log.i("JEFF", "------------------------------------------");
                    //-----------------------------------------------------------------------------------------

                    /**
                     * 죽은 프로세스들이 다시 전부 살아나는 시간을 체킹 하기 위해서 만든 소스
                     * createTopView를 시작 하기 전 살아나기 이전의 프로세스 상태를 다 가지고 있는 상태에서
                     * 사용자 터치가 있을 때마다 그 리스트를 지우기 시작함. 그래서 그 리스트 사이즈가 0이 되는 순간 다 살아난거니까
                     * 그 시간을 체킹함
                     */
                    if (!isKilling&&bkc!=null) {
                        deadList.clear();
                        if (beforeKillList.size() > 0) {
                            for (ActivityManager.RunningAppProcessInfo br : curList) {
                                for (int k = 0; k < beforeKillList.size(); k++) {
                                    if (br.processName.equals(beforeKillList.get(k))) {
                                        deadList.add(beforeKillList.get(k));
                                        beforeKillList.remove(k);
                                        break;
                                    }
                                }
                            }
//                            Log.i("LOTTE", "------------------------------------------");
//                            for (int k = 0; k < beforeKillList.size(); k++) {
//                                Log.i("LOTTE", beforeKillList.get(k));
//                            }
//                            Log.i("LOTTE", "------------------------------------------");
                        }

                        // 여기에서 다시 살아난 애들의 사이즈를 bkc 객체에다 집어 넣어줌
                        // 그 이유는 다시 살아나는 애들의 비율을 유지하면서 계산하기 위함
                        if(deadList.size()>0){
                            bkc.setDeadListSize(deadList.size());
                            int percent = (bkc.getDeadListSize() * 100)/bkc.originalListSize;
                            long time = (System.currentTimeMillis() - beforeKillTime)/1000;

                            for(int k=0;k<deadList.size();k++){
                                Util.saveLogToFile(Util.dateToStringYMDHMS(beforeKillTime) + "/" + Util.dateToStringYMDHMS(System.currentTimeMillis()) + "/" + String.valueOf(percent)+"%" + "/" + deadList.get(k) + "/" + String.valueOf(time) + "\n", "/SSLAB/recreatetime.txt");
                                Log.d("LOTTE", String.valueOf(bkc.getDeadListSize()) + "," + String.valueOf(bkc.originalListSize));
                                Log.d("LOTTE", "앱 죽인 시간:" + Util.dateToStringYMDHMS(beforeKillTime) + ", 재복구 시간 : " + Util.dateToStringYMDHMS(System.currentTimeMillis()) + ", 퍼센트 : " + percent + ", 살아난 앱 " + deadList.get(k) + "\n");
                            }
                        }

//                        if (beforeKillList.size() <= 1) {
//                            isKilling = true;
//                            beforeKillList.clear();
//
//                            long recreateTime = System.currentTimeMillis() - beforeKillTime;
//                            Util.saveLogToFile(Util.dateToStringYMDHMS(beforeKillTime) + "/" + Util.dateToStringYMDHMS(System.currentTimeMillis()) + "/" + recreateTime +"/NULL" +"\n", "/SSLAB/recreatetime.txt");
//                            Log.d("LOTTE", "앱 죽인 시간:" + Util.dateToStringYMDHMS(beforeKillTime) + ", 재복구 시간 : " + Util.dateToStringYMDHMS(System.currentTimeMillis()) + ", 걸린 시간 : " + recreateTime + "\n");
//                        }

                        try {
                            bkc.setIsFinish(isKilling);
                            bkc.setList(beforeKillList);

                            FileOutputStream fileout = new FileOutputStream(Environment.getExternalStorageDirectory() + "/SSLAB/bkc.ser");
                            ObjectOutputStream objOut = new ObjectOutputStream(fileout);
                            objOut.writeObject(bkc);
                            objOut.close();
                            fileout.close();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }


                    if (!prevPackage.equals(curPackage)) {
                        break;
                    }

                    endTime = System.currentTimeMillis();
                }
            }

            if (!prevPackage.equals(curPackage)) {
                Log.d("LOTTE", "curPackage: " + curPackage + " 실행됨" + Util.dateToStringYMDHMS(System.currentTimeMillis()));
                calExecutionTime(prevPackage,prevAppLabel);
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }
        }
    }

    /**
     * 앱의 실행 시간을 계산한다.
     * @param packageName 실행된 앱 패키지 이름
     * @param appLabel 실행된 앱 라벨 이름
     */
    private void calExecutionTime(String packageName, String appLabel){
        int memory = 0;

        curTime = System.currentTimeMillis();
        exeTime = curTime-prevTime;
        prevTime = System.currentTimeMillis();

        final List<ActivityManager.RunningAppProcessInfo> ps = activityManager.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : ps) {
            if (runningAppProcessInfo.processName.equals(packageName)) {
                int memInfo[] = {runningAppProcessInfo.pid, 0};
                Debug.MemoryInfo debugMeminfo[] = activityManager.getProcessMemoryInfo(memInfo);
                for (android.os.Debug.MemoryInfo pidMemoryInfo : debugMeminfo) {
                    if (pidMemoryInfo.getTotalPss() != 0) {
                        memory = pidMemoryInfo.getTotalPss();
                    }
                }
            } else
                continue;
        }

        if (RTMap.containsKey(packageName)) {
            AppInfoClass temp = RTMap.get(packageName);
            long savedTime = temp.totalTime;
            temp.totalTime = savedTime + exeTime;
            temp.lastTime = curTime;
            RTMap.put(packageName, temp);

            saveFile();
        } else {

            AppInfoClass temp = new AppInfoClass(appLabel, curTime, exeTime, memory);
            RTMap.put(packageName, temp);

            saveFile();
        }
    }
    BroadcastReceiver screenOnOff = new BroadcastReceiver(){
        public static final String ScreenOff = "android.intent.action.SCREEN_OFF";
        public static final String ScreenOn = "android.intent.action.SCREEN_ON";

        public void onReceive(Context context, Intent intent)
        {
            if(intent.getAction().equals(ScreenOff)){
                calExecutionTime(curPackage,curAppLabel);

                Log.d("MAIN", "SCREENOFF");
            }else if(intent.getAction().equals(ScreenOn)){
                prevTime = System.currentTimeMillis();
                curTime = System.currentTimeMillis();

                Log.d("MAIN", "SCREENON");
            }
        }
    };

    public void saveFile()
    {
        try{
            FileOutputStream fileout = new FileOutputStream(Environment.getExternalStorageDirectory()+"/SSLAB/RTMap.ser");
            ObjectOutputStream objOut = new ObjectOutputStream(fileout);
            objOut.writeObject(RTMap);
            objOut.close();
            fileout.close();

            Log.d("SAVELOG", "FILE SAVED");
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }



    /**
     * lmk를 알기 위해 걸어놓은 callback class
     * onTrimMemory가 자동으로 호출되면 lmk가 발생한 것
     */
    ComponentCallbacks2 mComponentCallbacks = new ComponentCallbacks2() {
        @Override
        public void onTrimMemory(int level) {
            String str="";

            if(level != ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN){
                if(level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE){
                    Log.e("JEFF", "die TRIM_MEMORY_COMPLETE");
                    str = "TRIM_MEMORY_COMPLETE";
                }else if(level == ComponentCallbacks2.TRIM_MEMORY_BACKGROUND){
                    Log.e("JEFF", "die TRIM_MEMORY_BACKGROUND");
                    str = "TRIM_MEMORY_BACKGROUND";

                }else if(level == ComponentCallbacks2.TRIM_MEMORY_MODERATE){
                    Log.e("JEFF", "die TIME_MEMORY_MODERATE");
                    str = "TIME_MEMORY_MODERATE";

                }else if(level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL){
                    Log.e("JEFF", "die TRIM_MEMORY_RUNNING_CRITICAL");
                    str = "TRIM_MEMORY_RUNNING_CRITICAL";

                }else if(level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW){
                    Log.e("JEFF", "die TRIM_MEMORY_RUNNING_LOW");
                    str = "TRIM_MEMORY_RUNNING_LOW";

                }else if(level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE){
                    Log.e("JEFF", "die TRIM_MEMORY_RUNNING_MODERATE");
                    str = "TRIM_MEMORY_RUNNING_MODERATE";
                }
                final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                List<ActivityManager.RunningAppProcessInfo> lmkCurList = activityManager.getRunningAppProcesses();
                List<ActivityManager.RunningAppProcessInfo> lmkPrevList = activityManager.getRunningAppProcesses();

                lmkPrevList.clear();
                lmkPrevList.addAll(prevList);

                for (ActivityManager.RunningAppProcessInfo br : lmkCurList) {
                    for (ActivityManager.RunningAppProcessInfo ar : lmkPrevList) {
                        if (br.pid == ar.pid) {
                            lmkPrevList.remove(ar);
                            break;
                        }
                    }
                }

                Log.e("LOTTE", "LMK 발생!!");

                Util.saveLogToFile(Util.dateToStringYMDHMS(System.currentTimeMillis()) + "/" + str + "/" + curAppLabel+ "/" + String.valueOf(memoryInfo.availMem / 1048576L) + "/NULL\n", "/SSLAB/lmk.txt");

                if (lmkPrevList.size() > 0) {

                    for (ActivityManager.RunningAppProcessInfo pl : lmkPrevList) {

                        Log.e("LOTTE", pl.processName);
                        curAppLabel = curAppLabel.replace(System.getProperty("line.separator"), "");
                        Util.saveLogToFile(Util.dateToStringYMDHMS(System.currentTimeMillis()) + "/" + str + "/" + curAppLabel+ "/" + String.valueOf(memoryInfo.availMem / 1048576L) + "/" +pl.processName +"\n", "/SSLAB/lmk.txt");
                    }
                }

            }
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
            // call it here to support old operating systems
        }

    };
}
