package sslab.lova.mem_monitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import jxl.Workbook;
import jxl.format.Colour;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * Created by JEFF on 2015-11-07.
 */
public class AlarmService extends Service {

    private Map<String, AppInfoClass> RTMap = new HashMap<String, AppInfoClass>();

    private String[] mapKey;
    private int startId;
    private Context mContext;
    private String killingMessage="";
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d("LOTTE","AlarmService onCreate()");
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("LOTTE","AlarmService onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mContext = this;
        killingMessage = intent.getStringExtra("killingMessage");
        Log.d("LOTTE","AlarmService killingMessage : " + killingMessage );

        makeExcelOfServiceInfo();
        Log.i("LOTTE", "AlarmService 호출!!");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                GMailSender sender = new GMailSender("sslab.dev", "sslab5760"); // SUBSTITUTE HERE
                try {
                    sender.sendMail(
                            "[NEW 서비스에서 모인 정보]",   //subject.getText().toString(),
                            "메일 본문입니다..~~ ",           //body.getText().toString(),
                            "sslab.dev@gmail.com",          //from.getText().toString(),
                            "sslab.dev@gmail.com",            //to.getText().toString()
                            "/SSLAB/CollectServiceData.xls"
                    );
                    Util.removeDir("SSLAB");
                } catch (Exception e) {
                    Log.e("SendMail", e.getMessage(), e);
                }
            }
        });

        thread.start();

        stopSelf();

        return START_NOT_STICKY;
    }

    /**
     * TopViewService에서 모은 3개의 정보 (자동 실행된 어플리케이션, 재복구 걸리는 시간, 앱 사용시간) 과
     * CollectBroadcastMessageService 에서 모은 1개의 정보 (브로드캐스트 메시지 발생 )
     * 을 각 시트별로 만들어서 엑셀 파일을 만드는 함수
     * Sheet1 : 자동 실행된 어플리케이션 <시간, 이름, importance>
     * Sheet2 : 앱 실행 시간  <이름, 총 사용 시간, 최근 사용 시간>
     * Sheet3 : 재복구 시 걸리는 시간 <모든 앱을 죽인 시간, 재복구가 완료된 시점의 시간, 걸린 시간>
     * Sheet4 : 브로드캐스트 메시지 로깅 <시간, action>
     */
    private void makeExcelOfServiceInfo() {
        BufferedReader in;
        String s;

        try {
            int row = 0, column = 0;

            File xmlFile = new File(Environment.getExternalStorageDirectory() + "/SSLAB/CollectServiceData.xls");
            xmlFile.getParentFile().mkdirs();
            WritableWorkbook workbook = Workbook.createWorkbook(new File(Environment.getExternalStorageDirectory() + "/SSLAB/CollectServiceData.xls"));

            jxl.write.WritableCellFormat classifyFormat = new WritableCellFormat();
            classifyFormat.setBackground(Colour.BLUE_GREY);
            jxl.write.WritableCellFormat format = new WritableCellFormat();
            jxl.write.Label label = null;

            //-------------------------------------------------------------------------------
            try {
                WritableSheet sheet1 = workbook.createSheet("자동 실행된 어플리케이션", 0);

                label = new jxl.write.Label(0, row, "시간", classifyFormat);
                sheet1.addCell(label);
                label = new jxl.write.Label(1, row, "앱 이름", classifyFormat);
                sheet1.addCell(label);
                label = new jxl.write.Label(2, row, "패키지 이름 ", classifyFormat);
                sheet1.addCell(label);
                label = new jxl.write.Label(3, row, "IMPORTANCE", classifyFormat);
                sheet1.addCell(label);
                label = new jxl.write.Label(4, row, "발생한 Broadcast action", classifyFormat);
                sheet1.addCell(label);

                row++;

                in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/SSLAB/autocreate.txt")));

                while ((s = in.readLine()) != null) {
                    String[] data = s.split("/");

                    label = new jxl.write.Label(0, row, data[0], format);
                    sheet1.addCell(label);
                    label = new jxl.write.Label(1, row, data[1], format);
                    sheet1.addCell(label);
                    label = new jxl.write.Label(2, row, data[2], format);
                    sheet1.addCell(label);
                    label = new jxl.write.Label(3, row, data[3], format);
                    sheet1.addCell(label);
                    label = new jxl.write.Label(4, row, data[4], format);
                    sheet1.addCell(label);
                    row++;
                }
                in.close();
            } catch (Exception e2) {
                Log.d("ALARM", "EXCEPTION\n" + e2.getMessage());
                e2.printStackTrace();
            }
            //-------------------------------------------------------------------------------
            try {
                WritableSheet sheet2 = workbook.createSheet("앱 실행 시간", 1);
                row = 0;
                label = new jxl.write.Label(0, row, "앱 이름", classifyFormat);
                sheet2.addCell(label);
                label = new jxl.write.Label(1, row, "총 사용 시간", classifyFormat);
                sheet2.addCell(label);
                label = new jxl.write.Label(2, row, "최근 접근 시간", classifyFormat);
                sheet2.addCell(label);
                label = new jxl.write.Label(3, row, "메모리 사용량", classifyFormat);
                sheet2.addCell(label);

                row++;

                loadRTMapAndTimeMap();
                mapKey = RTMap.keySet().toArray(new String[0]);


                for (int i = 0; i < RTMap.size(); i++) {
                    AppInfoClass RTmapInfo = RTMap.get(mapKey[i]);

                    label = new jxl.write.Label(0, row, mapKey[i], format);
                    sheet2.addCell(label);
                    label = new jxl.write.Label(1, row, Util.dateToStringHMS(RTmapInfo.totalTime), format);
                    sheet2.addCell(label);
                    label = new jxl.write.Label(2, row, Util.dateToStringYMDHMS(RTmapInfo.lastTime), format);
                    sheet2.addCell(label);
                    label = new jxl.write.Label(3, row, RTmapInfo.cur_memory + " KB", format);
                    sheet2.addCell(label);
                    row++;
                }
            } catch (Exception e2) {
                Log.d("ALARM", "EXCEPTION\n" + e2.getMessage());
                e2.printStackTrace();
            }
            //-------------------------------------------------------------------------------
            try {
                WritableSheet sheet3 = workbook.createSheet("모든 앱을 죽이고 다시 살아나는데 걸리는 시간", 2);
                row = 0;
                label = new jxl.write.Label(0, row, "앱을 죽인 시간 ", classifyFormat);
                sheet3.addCell(label);
                label = new jxl.write.Label(1, row, "다시 살아나는 시간", classifyFormat);
                sheet3.addCell(label);
                label = new jxl.write.Label(2, row, "퍼센트", classifyFormat);
                sheet3.addCell(label);
                label = new jxl.write.Label(3, row, "앱이름", classifyFormat);
                sheet3.addCell(label);
                label = new jxl.write.Label(4, row, "걸린시간", classifyFormat);
                sheet3.addCell(label);

                row++;
                in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/SSLAB/recreatetime.txt")));

                while ((s = in.readLine()) != null) {
                    String[] data = s.split("/");

                    label = new jxl.write.Label(0, row, data[0], format);
                    sheet3.addCell(label);
                    label = new jxl.write.Label(1, row, data[1], format);
                    sheet3.addCell(label);
                    label = new jxl.write.Label(2, row, data[2], format);
                    sheet3.addCell(label);
                    label = new jxl.write.Label(3, row, data[3], format);
                    sheet3.addCell(label);
                    label = new jxl.write.Label(4, row, data[4], format);
                    sheet3.addCell(label);
                    row++;
                }
                in.close();
            } catch (Exception e2) {
                Log.d("ALARM", "EXCEPTION\n" + e2.getMessage());
                e2.printStackTrace();
            }
            //-------------------------------------------------------------------------------
            try {
                WritableSheet sheet4 = workbook.createSheet("브로드캐스트 메시지 로깅", 3);
                row = 0;
                label = new jxl.write.Label(0, row, "시간", classifyFormat);
                sheet4.addCell(label);
                label = new jxl.write.Label(1, row, "메시지 Action ", classifyFormat);
                sheet4.addCell(label);

                row++;
                in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/SSLAB/broadcastmessage.txt")));

                while ((s = in.readLine()) != null) {
                    String[] data = s.split("/");

                    label = new jxl.write.Label(0, row, data[0], format);
                    sheet4.addCell(label);
                    label = new jxl.write.Label(1, row, data[1], format);
                    sheet4.addCell(label);
                    row++;
                }
                in.close();
            } catch (Exception e2) {
                Log.d("ALARM", "EXCEPTION\n" + e2.getMessage());
                e2.printStackTrace();
            }


            try {    //-------------------------------------------------------------------------------
                WritableSheet sheet5 = workbook.createSheet("lmk", 4);
                row = 0;
                label = new jxl.write.Label(0, row, "시간", classifyFormat);
                sheet5.addCell(label);
                label = new jxl.write.Label(1, row, "level", classifyFormat);
                sheet5.addCell(label);
                label = new jxl.write.Label(2, row, "lmk를 발생시킨 앱", classifyFormat);
                sheet5.addCell(label);
                label = new jxl.write.Label(3, row, "사용가능 메모리 사이즈", classifyFormat);
                sheet5.addCell(label);
                label = new jxl.write.Label(4, row, "죽은 앱", classifyFormat);
                sheet5.addCell(label);
                row++;
                in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/SSLAB/lmk.txt")));

                while ((s = in.readLine()) != null) {
                    String[] data = s.split("/");

                    label = new jxl.write.Label(0, row, data[0], format);
                    sheet5.addCell(label);
                    label = new jxl.write.Label(1, row, data[1], format);
                    sheet5.addCell(label);
                    label = new jxl.write.Label(2, row, data[2], format);
                    sheet5.addCell(label);
                    label = new jxl.write.Label(3, row, data[3], format);
                    sheet5.addCell(label);
                    label = new jxl.write.Label(4, row, data[4], format);
                    sheet5.addCell(label);
                    row++;
                }
                in.close();
            } catch (Exception e2) {
                Log.d("ALARM", "EXCEPTION\n" + e2.getMessage());
                e2.printStackTrace();
            }

            try {    //-------------------------------------------------------------------------------
                WritableSheet sheet6 = workbook.createSheet("빌드번호", 5);
                row = 0;
                label = new jxl.write.Label(0, row, "빌드번호", classifyFormat);
                sheet6.addCell(label);
                label = new jxl.write.Label(1, row, "테스트방식", classifyFormat);
                sheet6.addCell(label);
                row++;

                label = new jxl.write.Label(0, row, Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID), format);
                sheet6.addCell(label);
                label = new jxl.write.Label(1, row, killingMessage, format);
                sheet6.addCell(label);

            } catch (Exception e2) {
                Log.d("ALARM", "EXCEPTION\n" + e2.getMessage());
                e2.printStackTrace();
            }

            // 빌트인 목록 추가
            workbook.write();
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRTMapAndTimeMap() {
        try {
            FileInputStream fileIn = new FileInputStream(Environment.getExternalStorageDirectory() + "/SSLAB/RTMap.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            RTMap = (Map<String, AppInfoClass>) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception e) {

        }
    }
}