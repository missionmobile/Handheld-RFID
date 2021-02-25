package de.mindsquare.rfid;

import android.app.Activity;
import android.os.SystemClock;
import android.os.Build;

//neu
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.content.DialogInterface;
import android.content.Intent;

import com.handheldgroup.serialport.SerialPort;

import org.apache.cordova.CallbackContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public abstract class SerialPortActivity extends Activity {
    
    protected SerialPort mSerialPort;
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private CallbackContext cbCtx;

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[64];
                    if (mInputStream == null) return;
                    size = mInputStream.read(buffer);
                    if (size > 0) {
                        onDataReceived(buffer, size, this);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public SerialPortActivity(CallbackContext cbCtx) {

        Log.i("SerialPortActivity", "app gestartet");
        this.cbCtx = cbCtx;

        try {
            // By adding support for Nautiz X6, we switch over to handheld.com Serialport 1.5.0 library.
            // getSerialPath and setDevicePower do all we did here in the past by long code.
            // Enabling port and UART is done by the Serialport 1.5.0 library internally.
                                
            SerialPort.setDevicePower(this, false);
            SystemClock.sleep(100);
            SerialPort.setDevicePower(this, true);
            SystemClock.sleep(100);

            File port = new File(SerialPort.getSerialPath()); 
			mSerialPort = new SerialPort(port, 9600, 0); 
			mOutputStream = mSerialPort.getOutputStream(); 
			mInputStream = mSerialPort.getInputStream(); 
           

            /* Create a receiving thread */
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (SecurityException e) {
            cbCtx.error("Security Error");
        } catch (IOException e) {
            cbCtx.error("Unknown Error");
        } catch (InvalidParameterException e){
            cbCtx.error("Wrong Config Error");
        }
    }


    protected abstract void onDataReceived(final byte[] buffer, final int size, Thread t);

    @Override
    protected void onDestroy() {
        if (mReadThread != null)
            mReadThread.interrupt();
        mSerialPort.close();
        mSerialPort = null;
        super.onDestroy();
    }
    
}
