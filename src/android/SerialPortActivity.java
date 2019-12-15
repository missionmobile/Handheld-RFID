package de.mindsquare.rfid;

import android.os.SystemClock;

import com.handheldgroup.serialport.SerialPort;

import org.apache.cordova.CallbackContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public abstract class SerialPortActivity {
    protected SerialPort mSerialPort;
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private CallbackContext cbCtx;

    private class ReadThread extends Thread {

        private CallbackContext cbCtx;

        ReadThread(CallbackContext cbCtx){
            this.cbCtx = cbCtx;
        }


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
                        onDataReceived(buffer, size, this.cbCtx);
                    }
                    if(this.cbCtx.isFinished()) {
                        this.interrupt();
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
            mSerialPort = new SerialPort(new File("/dev/ttyMT3"), 9600, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            /* Create a receiving thread */
            mReadThread = new ReadThread(cbCtx);
            mReadThread.start();

        } catch (SecurityException e) {
            this.cbCtx.error("You do not have read/write permission to the serial port.");
        } catch (IOException e) {
            this.cbCtx.error("The serial port can not be opened for an unknown reason.");
        } catch (InvalidParameterException e) {
            this.cbCtx.error("Please configure your serial port first.");
        }
    }

    protected abstract void onDataReceived(final byte[] buffer, final int size, CallbackContext cbCtx);

    /*
    protected void onDestroy() {
        if (mReadThread != null)
            mReadThread.interrupt();
        mSerialPort.close();
        mSerialPort = null;
        SerialPort.setUart3Enabled(false);
        super.onDestroy();
    }*/
}
