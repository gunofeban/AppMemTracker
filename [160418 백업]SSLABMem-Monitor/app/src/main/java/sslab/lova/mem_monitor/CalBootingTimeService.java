package sslab.lova.mem_monitor;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

/**
 * Created by JEFF on 2015-11-20.
 */
public class CalBootingTimeService extends Service {

    ActivityManager activityManager;
    Boolean firstStart = true;
    List<ActivityManager.RunningAppProcessInfo> curList, prevList, mRunningAppProcessInfo;

    long startTime=0;
    long endTime=0;
    int pollingCount = 0;
    Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LOTTE", "CalBootingTimeSerivce 시작");
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        mContext = this;
        startTime = System.currentTimeMillis();

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    pollingProcessList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    public void pollingProcessList() {

        while (true) {
            if (firstStart) {
                curList = activityManager.getRunningAppProcesses();
                prevList = activityManager.getRunningAppProcesses();

                firstStart = false;
            } else {
                prevList.clear();
                for (ActivityManager.RunningAppProcessInfo br : curList) {
                    prevList.add(br);
                }

                curList = activityManager.getRunningAppProcesses();
                mRunningAppProcessInfo = activityManager.getRunningAppProcesses();

                for (ActivityManager.RunningAppProcessInfo br : prevList) {
                    for (ActivityManager.RunningAppProcessInfo ar : mRunningAppProcessInfo) {
                        if (br.pid == ar.pid) {
                            mRunningAppProcessInfo.remove(ar);
                            break;
                        }
                    }
                }

                if (mRunningAppProcessInfo.size() != 0) {
                    pollingCount = 0;
                }else{
                    pollingCount++;
                }
            }

            if(pollingCount == 20){
                break;
            }
            Log.d("LOTTE", String.valueOf(pollingCount));
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        endTime = System.currentTimeMillis();
        Log.d("LOTTE", "부팅 완료 " + Util.dateToStringHMS(endTime - startTime));
        Util.saveLogToFileOverride(Util.dateToStringHMS(endTime-startTime) + "\n", "/SSLAB/bootcompleteTime.txt");

        Intent intent = new Intent(mContext,KillingAllProcessService.class);
        intent.putExtra("BootTime",Util.dateToStringHMS(endTime - startTime));
        //여기에 부팅 시작했던 시간 넣었으면 좋겠고
        startService(intent);

        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LOTTE", "CalBootingTimeSerivce 종료");
    }
}
