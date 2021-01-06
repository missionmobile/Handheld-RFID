package de.mindsquare.rfid;

import android.app.Activity;
import android.os.SystemClock;

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
        this.cbCtx = cbCtx;

        try {
            // Enable port and UART
            // required starting with OS B031 - see http://kb.handheldgroup.com/22882 for more details on this change
            SerialPort.setUart3Enabled(true);
            SystemClock.sleep(200);
            // "/dev/ttyMT3" for Nautiz X2
            mSerialPort = new SerialPort(new File("/dev/ttyMT3"), 9600, 0);
            
            //TK add X6 support
            if ("NAUTIZ_X6".equals(Build.MODEL)) {
            // "/dev/ttyHSL1" for new Nautiz X6
             mSerialPort = new SerialPort(new File("/dev/ttyHSL1"), 9600, 0);
            }
            
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            /* Create a receiving thread */
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (SecurityException e) {
            cbCtx.error("Securiry Error");
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
        SerialPort.setUart3Enabled(false);
        super.onDestroy();
    }
}
