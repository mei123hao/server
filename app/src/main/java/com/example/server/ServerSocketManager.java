package com.example.server;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class ServerSocketManager {

    public static String IP;
    public static int PORT;
    public ServerSocket serverSocket = null;
    public DataInputStream inputStream;
    public boolean socketCreated = false;

    private RevMsgPostProcessThread msgPostProcessThread;
    private RevVideoPostProcessThread videoPostProcessThread;

//    private Bitmap revBitmap;

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return PORT;
    }

    public static void getLocalIpAddress(ServerSocket serverSocket) {
        try{
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String mIP = inetAddress.getHostAddress().substring(0, 3);
                    if (mIP.equals("192")) {
                        IP = inetAddress.getHostAddress();    //获取本地IP
                        PORT = serverSocket.getLocalPort();    //获取本地的PORT
                        Log.e("IP", "" + IP);
                        Log.e("PORT", "" + PORT);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


    public void createSocket(Handler mHandler) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();

                /*指明服务器端的端口号*/
                try {
                    serverSocket = new ServerSocket(8000);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                getLocalIpAddress(serverSocket);

                Calculation.calculate(2);

                //消息队列，用于更新UI，显示连接情况
                Message message_1 = mHandler.obtainMessage();
                message_1.what = 1;
                IP = getIP();
                PORT = getPort();
                message_1.obj = "IP:" + IP + " PORT: " + PORT;
                mHandler.sendMessage(message_1);

                socketCreated = true;
            }
        };
        thread.start();
    }

    public void receiveMsgData(Handler msgHandler, StringBuffer rev_Buffer) {
//        if (videoPostProcessThread.isAlive()){
//            videoPostProcessThread.stop();
//        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                /*指明服务器端的端口号*/
//                try {
//                    serverSocket = new ServerSocket(8000);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                getLocalIpAddress(serverSocket);

                while (true) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        inputStream  = new DataInputStream(socket.getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    msgPostProcessThread = new RevMsgPostProcessThread(socket, inputStream, rev_Buffer, msgHandler);
                    msgPostProcessThread.start();
                }
            }
        };
        thread.start();
    }


    class RevMsgPostProcessThread extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private StringBuffer stringBuffer;
        private Handler msgHandler;

        public RevMsgPostProcessThread(Socket socket, InputStream inputStream, StringBuffer buffer, Handler msgHandler) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.stringBuffer = buffer;
            this.msgHandler = msgHandler;
        }

        @Override
        public void run() {
            int len;
            byte[] bytes = new byte[409600];
//            boolean isString = false;

            try {
                while ((len = inputStream.read(bytes)) != -1) {
                    byte[] byteRev = new byte[len];
                    for (int i=0;i<len;i++) {
                        if(bytes[i] != '\0') {
                            byteRev[i] = bytes[i];
                        }
                    }
                    String str = null;
                    try{
                        str = new String(byteRev, "utf-8");
                    }catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    Message message_2 = msgHandler.obtainMessage();
                    message_2.what = 2;
                    message_2.obj = str;
                    msgHandler.sendMessage(message_2);

//                    for(int i=0; i<len; i++) {
//                        if(bytes[i] != '\0') {
//                            stringBuffer.append((char)bytes[i]);
//                        }else {
//                            isString = true;
//                            break;
//                        }
//                    }
//                    if(isString) {
//                        Message message_2 = msgHandler.obtainMessage();
//                        message_2.what = 2;
//                        message_2.obj = stringBuffer;
//                        msgHandler.sendMessage(message_2);
//                        isString = false;
//                    } else {
//
//                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    inputStream.close();
                    socket.close();
                } catch (IOException el) {
                    el.printStackTrace();
                }
            }
        }
    }


    public void receiveVideoData(Handler videoHandler) {
//        if (msgPostProcessThread.isAlive()) {
//            msgPostProcessThread.stop();
//        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();

                /*指明服务器端的端口号*/
//                try {
//                    serverSocket = new ServerSocket(8000);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                getLocalIpAddress(serverSocket);

                while (true) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        inputStream = new DataInputStream(socket.getInputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    videoPostProcessThread = new RevVideoPostProcessThread(socket, inputStream, videoHandler);
                    videoPostProcessThread.start();
//                    new RevVideoPostProcessThread(socket, inputStream, videoHandler).start();
                }
            }
        };
        thread.start();
    }


    class RevVideoPostProcessThread extends Thread {
        private Socket socket;
        private DataInputStream inputStream;
        private Handler videoHandler;
//        private StringBuffer sb;  //将图像编码为hex String,用于调试

        public RevVideoPostProcessThread(Socket socket, DataInputStream inputStream, Handler videoHandler){
            this.socket = socket;
            this.inputStream = inputStream;
            this.videoHandler = videoHandler;
//            this.sb = hex_str;
        }

        @Override
        public void run() {
            super.run();


            //状态机解析接收数据
            long len;
            byte[] buffer = new byte[4096];
            int status = 0;
            byte[] bytes_for_send = new byte[409600];
            int jpg_count = 0;

            try{
                while ((len = inputStream.read(buffer)) != -1) {
                    //解码为hex，查看数据是否正确，用于调试
//                    for (int i=0;i<len;i++) {
//                        String hex = Integer.toHexString((char)bytes[i] & 0xFF);
//                        if(hex.length() < 2){
//                            sb.append(0);
//                        }
//                        sb.append(hex);
//                    }
//                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, (int)len);

                    for (int i=0;i<len;i++) {
                        switch(status) {
                            case 0:
                                if (buffer[i] == (byte) 0xFF) {
                                    status++;
                                }
                                jpg_count = 0;
                                bytes_for_send[jpg_count++] = (byte) buffer[i];
                                break;
                            case 1:
                                if (buffer[i] == (byte) 0xD8) {
                                    status++;
                                    bytes_for_send[jpg_count++] = (byte) buffer[i];
                                }
                                //检错
                                else {
                                    //如果首字符不等于0XFF，返回case 0这个步骤
                                    if (buffer[i] != (byte) 0xFF)
                                        status = 0;
                                }
                                break;
                            case 2:
                                bytes_for_send[jpg_count++] = (byte) buffer[i];
                                //循环接收数据，直到碰到0XFF
                                if (buffer[i] == (byte) 0xFF) {
                                    status++;
                                }
                                if (jpg_count >= bytes_for_send.length)
                                    status = 0;
                                break;
                            case 3:
                                bytes_for_send[jpg_count++] = (byte) buffer[i];
                                if (buffer[i] == (byte) 0xD9) {
                                    status = 0;

                                    BitmapFactory.Options opt = new BitmapFactory.Options();
                                    opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes_for_send,0,jpg_count,opt);
                                    Message message_3 = videoHandler.obtainMessage();
                                    if (bitmap!=null) {
                                        message_3.obj = bitmap;
                                        message_3.what = 3;
                                        videoHandler.sendMessage(message_3);
                                    }
                                }
                                else {
                                    if (buffer[i] != (byte) 0xFF)
                                        status = 2;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    inputStream.close();
                    socket.close();
                } catch (IOException el) {
                    el.printStackTrace();
                }
            }
        }
    }


    public boolean startPing(String ip) {
        Log.e("Ping", "startPing...");
        boolean success=false;
        Process p =null;

        try {
            p = Runtime.getRuntime().exec("ping -c 1 -i 0.2 -W 1 " +ip);
            int status = p.waitFor();
            if (status == 0) {
                success=true;
            } else {
                success=false;
            }
        } catch (IOException e) {
            success=false;
        } catch (InterruptedException e) {
            success=false;
        }finally{
            p.destroy();
        }

        return success;
    }

//    public void stopRevVideoThread() {
//
//    }

}
