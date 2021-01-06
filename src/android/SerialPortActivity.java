package de.mindsquare.rfid;

import android.app.Activity;
import android.os.SystemClock;
import android.os.Build;

import com.handheldgroup.serialport.SerialPort;
import com.lovdream.ILovdreamDevice;

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
            
            File port = new File(SerialPort.getSerialPath()); 
			SerialPort.setDevicePower(this, true); 
			mSerialPort = new SerialPort(port, 9600, 0); 
			mOutputStream = mSerialPort.getOutputStream(); 
			mInputStream = mSerialPort.getInputStream(); 
            
            /*
            //TK add X6 support
	        File port = new File("/dev/ttyMT3");
	        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
	            // "/dev/ttyS0" for new Nautiz X2-C version
	            port = new File("/dev/ttyS0");
	        }
	        if ("NAUTIZ_X6".equals(Build.MODEL)) {
	            // "/dev/ttyHSL1" for new Nautiz X6
	            port = new File("/dev/ttyHSL1");
	        }
            
            setPower(false);
            SystemClock.sleep(100);
            setPower(true);
            SystemClock.sleep(100);
            mSerialPort = new SerialPort(port, 9600, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            */
            
            /*
            if ("NAUTIZ_X6".equals(Build.MODEL)) {
            // "/dev/ttyHSL1" for new Nautiz X6
            	mSerialPort = new SerialPort(new File("/dev/ttyHSL1"), 9600, 0);
            } else {
            // "/dev/ttyMT3" for Nautiz X2
            	mSerialPort = new SerialPort(new File("/dev/ttyMT3"), 9600, 0);
            }
            
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
			*/

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

    private void setPower(boolean enabled) {
        if ("NAUTIZ_X6".equals(Build.MODEL)) {
            setNx6PowerState(enabled);
        } else {
            setNx2PowerState(enabled);
        }
    }

    public static void setNx2PowerState(boolean enabled){
        SerialPort.setPower(enabled ? 0x17 : 0x18);
        // The port debilitation has change on NX2-C so we have to enable both UART ports for either to work
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SerialPort.setPortPower(2, enabled);
        }
        SerialPort.setPortPower(3, enabled);
    }

    private void setNx6PowerState(boolean powerOn) {
        try {
            ILovdreamDevice service = ILovdreamDevice.Stub.asInterface((IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", new Class[]{String.class}).invoke(null, new Object[]{"lovd_device"}));
            service.writeToFile("/sys/class/ext_dev/function/ext_dev_3v3_enable", powerOn ? "1" : "0");
            service.writeToFile("/sys/class/ext_dev/function/ext_dev_5v_enable", powerOn ? "1" : "0");
        } catch (Exception localException) {
            localException.printStackTrace();
            Log.e("IQUE", "can not get system service,is System ready?");
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
