package de.mindsquare.rfid;

import android.util.Log;
import android.media.AudioManager;
import android.media.ToneGenerator;

import org.apache.cordova.CallbackContext;

import java.io.IOException;

public class SearchTagActivity extends SerialPortActivity {
    private static final String TAG = SearchTagActivity.class.getSimpleName();

    boolean mByteReceivedBack;
    final Object mByteReceivedBackSemaphore = new Object();
    String mTagType, mIDBitCount, mID;
    Integer cmd;

    public SendingThread mSendingThread;
    byte[] txbuffer = new byte[50];
    byte[] rxbuffer_raw = new byte[200];
    byte[] rxbuffer = new byte[200];
    int rxbuffer_Index;

    CallbackContext cbCtx;

    public SearchTagActivity(CallbackContext cbCtx){
        super(cbCtx);

        this.cbCtx = cbCtx;

        if (mSerialPort != null) {
            mSendingThread = new SendingThread(this.cbCtx);
            mSendingThread.start();
        }
        mTagType = null;
        mIDBitCount = null;
        mID = null;
        cmd = 1;
    }

    private class SendingThread extends Thread {

        CallbackContext cbCtx;
        public SendingThread(CallbackContext cbCtx){
            this.cbCtx = cbCtx;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized (mByteReceivedBackSemaphore) {
                    mByteReceivedBack = false;
                    try {
                        if (mOutputStream != null) {
                            if (cmd == 1) {
                                rxbuffer_Index = 0;
                                txbuffer = new byte[50];
                                txbuffer[0] = '0';
                                txbuffer[1] = '5';
                                txbuffer[2] = '0';
                                txbuffer[3] = '0';
                                txbuffer[4] = '1';
                                txbuffer[5] = '0';
                                txbuffer[6] = '\r';
                                mOutputStream.write(new String(txbuffer).getBytes(), 0, 7);
                                Log.d(TAG, "SearchTag-GetTagType");
                                //cmd=0;
                            } else if (cmd == 2) { // SetTagType only LF
                                // TODO: Make sure all LF tags are enabled
                                rxbuffer_Index = 0;
                                txbuffer = new byte[50];
                                txbuffer[0] = '0';
                                txbuffer[1] = '5';
                                txbuffer[2] = '0';
                                txbuffer[3] = '2';
                                for (int i = 0; i < 8; i++)
                                    txbuffer[4 + i] = 'F';
                                for (int i = 0; i < 8; i++)
                                    txbuffer[12 + i] = '0';
                                txbuffer[20] = '\r';
                                for (int i = 0; i < 200; i++)
                                    rxbuffer_raw[i] = 0;
                                mOutputStream.write(new String(txbuffer).getBytes(), 0, 21);
                                Log.d(TAG, "SearchTag-SetTagType");
                            }

                        } else {
                            return;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    // Wait for 100ms before sending next byte, or as soon as
                    // the sent byte has been read back.
                    try {
                        mByteReceivedBackSemaphore.wait(300);
                        if (!mByteReceivedBack) {
                            // Timeout
                            if (cmd > 1)
                                cmd = 1;
                            else
                                cmd = 2;
                        }

                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    @Override
    protected void onDataReceived(byte[] buffer, int size, Thread t) {

        synchronized (mByteReceivedBackSemaphore) {

            int i;
            int temp1, temp2;
            for (i = 0; i < size; i++)                    // read the bytes and save them
            {
                rxbuffer_raw[rxbuffer_Index++] = buffer[i];
            }
            for (i = 0; i < rxbuffer_Index / 2; i++)        // transfer two raw bytes to one byte
            {
                temp1 = (rxbuffer_raw[i * 2] >= 65) ? (rxbuffer_raw[i * 2] - 55) : (rxbuffer_raw[i * 2] - 48);
                temp2 = (rxbuffer_raw[i * 2 + 1] >= 65) ? (rxbuffer_raw[i * 2 + 1] - 55) : (rxbuffer_raw[i * 2 + 1] - 48);
                rxbuffer[i] = (byte) ((temp1 << 4) + temp2);
            }
            if (rxbuffer[1] == 0x01 && size == 9) {                            // transponder found
                int type;
                type = rxbuffer[2];
                if (type == 0x040)
                    mTagType = "EM4x02/CASI-RUSCO";
                else if (type == 0x041)
                    mTagType = "HITAG 1/HITAG S";
                else if (type == 0x042)
                    mTagType = "HITAG 2";
                else if (type == 0x043)
                    mTagType = "EM4x50";
                else if (type == 0x044)
                    mTagType = "T55x7";
                else if (type == 0x045)
                    mTagType = "ISO FDX-B";
                else if (type == 0x046)
                    mTagType = "N/A";
                else if (type == 0x047)
                    mTagType = "N/A";
                else if (type == 0x048)
                    mTagType = "N/A";
                else if (type == 0x049)
                    mTagType = "HID Prox";
                else if (type == 0x04A)
                    mTagType = "ISO HDX/TIRIS";
                else if (type == 0x04B)
                    mTagType = "Cotag";
                else if (type == 0x04C)
                    mTagType = "ioProx";
                else if (type == 0x04D)
                    mTagType = "Indala";
                else if (type == 0x04E)
                    mTagType = "NexWatch";
                else if (type == 0x04F)
                    mTagType = "AWID";
                else if (type == 0x050)
                    mTagType = "G-Prox";
                else if (type == 0x051)
                    mTagType = "Pyramid";
                else if (type == 0x052)
                    mTagType = "Keri";
                else if (type == 0x053)
                    mTagType = "N/A";
                else
                    mTagType = String.format("%02X Hex", rxbuffer[2]);

                mIDBitCount = String.format("%d", rxbuffer[3]);
                mID = String.format("%02X %02X %02X %02X %02X", rxbuffer[5], rxbuffer[6], rxbuffer[7], rxbuffer[8], rxbuffer[9]);

                if(rxbuffer[5] != 0 && rxbuffer[6] != 0 && rxbuffer[7] != 0 && rxbuffer[8] != 0){
                    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);             
                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);  
                    this.cbCtx.success(mID);
                    t.interrupt();
                    mSendingThread.interrupt();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mSendingThread != null)
            mSendingThread.interrupt();
        super.onDestroy();
    }
}
