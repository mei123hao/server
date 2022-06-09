package com.example.server;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView ip_Port_View;
    private TextView revDataView;
    private EditText send_rev_Text;
    private ImageView video_View;
    private Button send_Video;
    private Button connect;

    private ProgressDialog pd;

    private int mode = 0; //默认接收信息模式
    private int MAX_MODE_NUM = 2;
//    private ChangeCharset changeCharset = new ChangeCharset();

//    private String ip = null;
//    private int port = 0;


//    private ServerSocket serverSocket = null;
    private ServerSocketManager socketManager = new ServerSocketManager();
    private StringBuffer stringBuffer = new StringBuffer();

    private static String[] PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,

    };

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch(msg.what) {
                case 1:
                    pd.dismiss();
                    if (socketManager.startPing(socketManager.getIP())) {
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "连接异常", Toast.LENGTH_SHORT).show();
                    }

                    ip_Port_View.setText(msg.obj.toString());
                    break;
                case 2:
                    Log.i("接收信息", msg.obj.toString());
                    send_rev_Text.setText(msg.obj.toString());
                    stringBuffer.setLength(0);
                    break;
                case 3:
                    video_View.setImageBitmap((Bitmap)msg.obj);
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip_Port_View = (TextView) findViewById(R.id.ip_port_view);
        revDataView = (TextView) findViewById(R.id.send_rev_textView);
        send_rev_Text = (EditText) findViewById(R.id.send_rev_edit);
        video_View = (ImageView) findViewById(R.id.iv_camera_image);

        send_Video = (Button) findViewById(R.id.send_video);
        connect = (Button) findViewById(R.id.connect);

        //权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查权限
            int i = ContextCompat.checkSelfPermission(this, PERMISSIONS[0]);
            //如果权限申请失败，则重新申请权限
            if (i != PackageManager.PERMISSION_GRANTED) {
                //重新申请权限函数
                startRequestPermission();
            }
        }

        send_rev_Text.setVisibility(View.INVISIBLE);
        revDataView.setVisibility(View.INVISIBLE);
        video_View.setVisibility(View.INVISIBLE);


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socketManager.getIP()==null && socketManager.getPort()==0) {
                    pd = ProgressDialog.show(MainActivity.this, "连接", "连接中...");
                    socketManager.createSocket(mHandler);
                }
                if (socketManager.getIP()!=null && socketManager.startPing(socketManager.getIP())){
                    Toast.makeText(MainActivity.this, "已建立连接",Toast.LENGTH_SHORT).show();
                    ip_Port_View.setText("IP:"+socketManager.getIP()+" PORT: "+socketManager.getPort());
                } else {
                    Toast.makeText(MainActivity.this, "未连接",Toast.LENGTH_SHORT).show();
                    ip_Port_View.setText("IP:null PORT:0");
                }
            }
        });

        send_Video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "这个按钮暂时还没功能呢>_<!",Toast.LENGTH_SHORT).show();
                if (socketManager.getIP()==null && socketManager.getPort()==0) {
                    Toast.makeText(MainActivity.this, "请先建立连接", Toast.LENGTH_SHORT).show();
                } else {
                    mode++;
                    if (mode >= MAX_MODE_NUM) {
                        mode = 0;
                    }
                    Log.e("mode",mode+" ");

                    switch (mode) {
                        case 0:
                            send_Video.setText("信息模式");
                            video_View.setVisibility(View.INVISIBLE);
                            revDataView.setVisibility(View.VISIBLE);
                            send_rev_Text.setVisibility(View.VISIBLE);
                            socketManager.receiveMsgData(mHandler, stringBuffer);
                            break;
                        case 1:
                            send_Video.setText("视频模式");
                            revDataView.setVisibility(View.INVISIBLE);
                            send_rev_Text.setVisibility(View.INVISIBLE);
                            video_View.setVisibility(View.VISIBLE);
                            socketManager.receiveVideoData(mHandler);
                            break;
                        default:
                            break;
                    }
                }
            }
        });

    }

    private void startRequestPermission(){
        //321为请求码
        ActivityCompat.requestPermissions(this,PERMISSIONS,321);
    }





    //退出应用提示
    private static Boolean isQuit = false;
    private Timer timer = new Timer();
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isQuit) {
                isQuit = true;
                Toast.makeText(getBaseContext(), R.string.back_more_quit,Toast.LENGTH_LONG).show();
                TimerTask task = null;
                task = new TimerTask() {
                    @Override
                    public void run() {
                        isQuit = false;
                    }
                };
                timer.schedule(task, 2000);
            } else {
                try {
                    socketManager.inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finish();
                System.exit(0);
            }
        }
        return false;
    }



}