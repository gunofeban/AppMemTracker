package sslab.lova.mem_monitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import jxl.Workbook;
import jxl.format.Colour;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

/**
 * Created by JEFF on 2015-11-07.
 */
public class TestActivity extends Activity {
    Button mButton;
    Context mContext;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testlayout);

        mContext = this;

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, KillingAllProcessService.class );
                startService(intent);
            }
        });
    }

    private void makeExcelOfServiceInfo(){
        try {
            int row = 0 ,column = 0 ;

            File xmlFile = new File(Environment.getExternalStorageDirectory()+"/SSLAB/CollectServiceData.xls");
            xmlFile.getParentFile().mkdirs();
            WritableWorkbook workbook = Workbook.createWorkbook(new File(Environment.getExternalStorageDirectory() + "/SSLAB/CollectServiceData.xls"));

            jxl.write.WritableCellFormat classifyFormat = new WritableCellFormat();
            jxl.write.WritableCellFormat format = new WritableCellFormat();
            classifyFormat.setBackground(Colour.BLUE_GREY);
            jxl.write.Label label = null;
            BufferedReader in;
            String s;
            //-------------------------------------------------------------------------------
            WritableSheet sheet1 = workbook.createSheet("자동 실행된 어플리케이션",0);

            label = new jxl.write.Label(0,row,"시간",classifyFormat);
            sheet1.addCell(label);
            label = new jxl.write.Label(1,row,"프로세스 이름 ",classifyFormat);
            sheet1.addCell(label);
            label = new jxl.write.Label(2,row,"IMPORTANCE",classifyFormat);
            sheet1.addCell(label);

            row ++;
            in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/SSLAB/autocreate.txt")));

            while ((s = in.readLine()) != null) {
                String[] data = s.split("/");

                label = new jxl.write.Label(0,row,data[0],format);
                sheet1.addCell(label);
                label = new jxl.write.Label(1,row,data[1],format);
                sheet1.addCell(label);
                label = new jxl.write.Label(2,row,data[2],format);
                sheet1.addCell(label);
                row++;
            }
            in.close();
            //-------------------------------------------------------------------------------
//            WritableSheet sheet2 = workbook.createSheet("앱 실행 시간",0);
//            row = 0;
//            label = new jxl.write.Label(0,row,"",classifyFormat);
//            sheet1.addCell(label);
//            label = new jxl.write.Label(1,row,"프로세스 이름 ",classifyFormat);
//            sheet1.addCell(label);
//            label = new jxl.write.Label(2,row,"IMPORTANCE",classifyFormat);
//            sheet1.addCell(label);
//
//            row ++;
//            in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/autocreate.txt")));
//
//            while ((s = in.readLine()) != null) {
//                String[] data = s.split("/");
//
//                label = new jxl.write.Label(0,row,data[0],format);
//                sheet1.addCell(label);
//                label = new jxl.write.Label(1,row,data[1],format);
//                sheet1.addCell(label);
//                label = new jxl.write.Label(2,row,data[2],format);
//                sheet1.addCell(label);
//                row++;
//            }
//            in.close();
            //-------------------------------------------------------------------------------
            WritableSheet sheet3 = workbook.createSheet("모든 앱을 죽이고 다시 살아나는데 걸리는 시간",0);
            row = 0;
            label = new jxl.write.Label(0,row,"모든 앱을 죽인 시간",classifyFormat);
            sheet3.addCell(label);
            label = new jxl.write.Label(1,row,"다시 살아나는 시간",classifyFormat);
            sheet3.addCell(label);
            label = new jxl.write.Label(2,row,"걸린 시간",classifyFormat);
            sheet3.addCell(label);

            row ++;
            in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/SSLAB/recreatetime.txt")));

            while ((s = in.readLine()) != null) {
                String[] data = s.split("/");

                label = new jxl.write.Label(0,row,data[0],format);
                sheet3.addCell(label);
                label = new jxl.write.Label(1,row,data[1],format);
                sheet3.addCell(label);
                label = new jxl.write.Label(2,row,data[2],format);
                sheet3.addCell(label);
                row++;
            }
            in.close();
            //-------------------------------------------------------------------------------
            WritableSheet sheet4 = workbook.createSheet("브로드캐스트 메시지 로깅",0);
            row = 0;
            label = new jxl.write.Label(0,row,"시간",classifyFormat);
            sheet4.addCell(label);
            label = new jxl.write.Label(1,row,"메시지 Action ",classifyFormat);
            sheet4.addCell(label);

            row ++;
            in = new BufferedReader(new FileReader(new File(Environment.getExternalStorageDirectory() + "/SSLAB/broadcastmessage.txt")));

            while ((s = in.readLine()) != null) {
                String[] data = s.split("/");

                label = new jxl.write.Label(0,row,data[0],format);
                sheet4.addCell(label);
                label = new jxl.write.Label(1,row,data[1],format);
                sheet4.addCell(label);
                row++;
            }
            in.close();

            // 빌트인 목록 추가
            workbook.write();
            workbook.close();

        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
