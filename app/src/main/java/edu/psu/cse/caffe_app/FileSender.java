package edu.psu.cse.caffe_app;

/**
 * Created by zongqing on 10/20/15.
 */
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


public class FileSender {
    private static final String LOG_TAG = "FileSender";

    private int SOCKET_PORT;
    private String SERVER;

    FileSender(String server_ip, int socket_port){
        SERVER = server_ip;
        SOCKET_PORT = socket_port;
    }

    void send(String file_name) throws IOException {

        final File myFile= new File(file_name);
//        byte[] mybytearray = new byte[8192];
        byte[] mybytearray = new byte[16384];
        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DataInputStream dis = new DataInputStream(bis);
        OutputStream os;
        DataOutputStream dos;


        Socket socket = null;
        try {

            socket = new Socket(SERVER, SOCKET_PORT);
            System.out.println("Connecting...");

            long start = System.currentTimeMillis();

            os = socket.getOutputStream();
            dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            int read;
/*
            while((read = dis.read(mybytearray)) != -1){
                dos.write(mybytearray, 0, read);
            }*/

            long acc=0;
            long N=myFile.length();
            while(acc<N){
                read=dis.read(mybytearray, 0, 16384);
                dos.write(mybytearray, 0, read);
                acc=acc+read;
            }


            dos.flush();
            start = System.currentTimeMillis() - start;

            Log.d(LOG_TAG, "Sent file " + file_name + ", spent " + String.valueOf(start) + "ms");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            if (bis != null) bis.close();
            if (socket!=null) socket.close();
        }
    }
}
