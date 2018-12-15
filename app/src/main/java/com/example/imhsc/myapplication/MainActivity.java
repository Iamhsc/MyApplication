package com.example.imhsc.myapplication;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.imhsc.myapplication.util.StringUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.imhsc.myapplication.util.StringUtil.shortToByteArray;
import static com.example.imhsc.myapplication.util.StringUtil.toHexStr;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private NfcAdapter mNfcAdapter;
    private TextView tv_card_id;
    private TextView tv_money;
    private EditText edit_m;
    private Button btn_write;
    byte[] moneyData=null;
    MifareClassic mfc;
    int bIndex=0;

    //密码
    byte[] bytePwd = {(byte) 0x81, (byte) 0xc7, (byte) 0x5d,
            (byte) 0x2f, (byte) 0x4d, (byte) 0x89};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_card_id = findViewById(R.id.textView);
        tv_money = findViewById(R.id.textView2);
        edit_m=findViewById(R.id.editText2);
        btn_write=findViewById(R.id.write);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (mNfcAdapter == null) {
            Toast.makeText(this, "该设备不支持nfc", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            startActivity(new Intent("android.settings.NFC_SETTINGS"));
            Toast.makeText(this, "设备未开启nfc", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter[] intentFilters = new IntentFilter[]{};
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        btn_write.setVisibility(View.INVISIBLE);
        edit_m.setVisibility(View.INVISIBLE);
        //intent就是onNewIntent方法返回的那个intent
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        mfc = MifareClassic.get(tag);
        //如果当前IC卡不是这个格式的mfc就会为空
        if (null != mfc) {
            try {
                //链接NFC
                mfc.connect();
                //验证扇区密码，否则会报错（链接失败错误）这里验证的是密码B，如果想验证密码A也行，将方法中的B换成A就行
                boolean isOpen = mfc.authenticateSectorWithKeyB(3, bytePwd);
                Tag iTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                tv_card_id.setText("CardID:" + toHexStr(iTag.getId()));
                if (isOpen) {
                    Toast.makeText(MainActivity.this, "Read done",Toast.LENGTH_LONG).show();
                    //获取扇区第一个块对应芯片存储器的位置
                    //（我是这样理解的，因为第0扇区的这个值是4而不是0）
                    bIndex = mfc.sectorToBlock(3);
                    moneyData = mfc.readBlock(bIndex + 1);
                    final byte[] money = {moneyData[2],moneyData[1], moneyData[0]};
                    int m = Integer.valueOf(toHexStr(money), 16);
                    final DecimalFormat df = new DecimalFormat("0.00");
                    tv_money.setText("Money：" + df.format((float) m / 100));
                    btn_write.setVisibility(View.VISIBLE);
                    edit_m.setVisibility(View.VISIBLE);
                    btn_write.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int w_data=0;
                            Toast.makeText(MainActivity.this, "Writing",Toast.LENGTH_LONG).show();
                            double rowMoney=Double.parseDouble(edit_m.getText().toString());
                            double me=rowMoney*100;
                            int money_int=(int)me;
                            if (money_int>99999){
                                Toast.makeText(MainActivity.this, "最大999.99",Toast.LENGTH_LONG).show();
                                return;
                            }
                            byte[] b_money=shortToByteArray(money_int);
                            moneyData[0]=b_money[0];
                            moneyData[1]=b_money[1];

                            if (b_money.length==3) {moneyData[2]=b_money[2]; }

                            //计算校验位
                            for (int i=0;i<moneyData.length-1;i++){
                                w_data+=moneyData[i];
                            }
                            byte[] check=shortToByteArray(w_data);
                            moneyData[moneyData.length-1]=check[0];
                            try {
                                //链接NFC
                                mfc.connect();
                                //验证扇区密码，否则会报错（链接失败错误）这里验证的是密码B，如果想验证密码A也行，将方法中的B换成A就行
                                boolean isOpen = mfc.authenticateSectorWithKeyB(3, bytePwd);
                                if (isOpen) {
                                    mfc.writeBlock(bIndex + 1, moneyData);
                                    mfc.writeBlock(bIndex + 2, moneyData);
                                    tv_money.setText("Money：" + df.format(rowMoney));
                                    Toast.makeText(MainActivity.this, "Write done",Toast.LENGTH_LONG).show();
                                } else {
                                    tv_money.setText("Write Error");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }finally {
                                try {
                                    mfc.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } else {
                    tv_money.setText("Reader Error");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    mfc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
