package de.mindsquare.rfid;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;

import java.io.IOException;

public class SearchTagActivity extends SerialPortActivity {
    private static final String TAG = SearchTagActivity.class.getSimpleName();

    private boolean mByteReceivedBack;
    private final Object mByteReceivedBackSemaphore = new Object();
    private String mTagType, mIDBitCount, mID;
    Integer cmd;

    SendingThread mSendingThread;
    byte[] txbuffer = new byte[50];
    byte[] rxbuffer_raw = new byte[200];
    byte[] rxbuffer = new byte[200];
    int rxbuffer_Index;
    //ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_RING, 80);

    SearchTagActivity(CallbackContext cbCtx){
        super(cbCtx);

        if (mSerialPort != null) {
            mSendingThread = new SendingThread();
            //mSendingThread.start();
        }

        mTagType = null;
        mIDBitCount = null;
        mID = null;
        cmd = 1;
    }

    private class SendingThread extends Thread {
        @Override
        public void run() {
            super.run();

            while (!isInterrupted()) {
                synchronized (mByteReceivedBackSemaphore) {
                    mByteReceivedBack = false;
                    try {
                        if (mOutputStream != null) {
                            if (cmd == 1) {
                                rxbuffer_Index = 0;
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
                                rxbuffer_Index = 0;
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
            Log.d(TAG, "Stopped Thread!");
        }
    }

    @SuppressLint("DefaultLocale")
    protected void onDataReceived(byte[] buffer, int size, CallbackContext cbCtx) {

        synchronized (mByteReceivedBackSemaphore) {

            int i;
            int temp1, temp2;
            for (i = 0; i < size; i++)                    // read the bytes and save them
            {
                if(rxbuffer_Index >= rxbuffer_raw.length) {
                    // Shift array left
                    for(int j=0; j<rxbuffer_raw.length / 2; j++) {
                        rxbuffer_raw[j] = rxbuffer_raw[(rxbuffer_raw.length / 2) + j];
                        rxbuffer_raw[(rxbuffer_raw.length / 2) + j] = 0;
                        Log.d(TAG, "Shortened Array");
                        rxbuffer_Index = rxbuffer_raw.length / 2;
                    }
                }
                rxbuffer_raw[rxbuffer_Index++] = buffer[i];
            }
            for (i = 0; i < rxbuffer_Index / 2; i++)        // transfer two raw bytes to one byte
            {
                temp1 = (rxbuffer_raw[i * 2] >= 65) ? (rxbuffer_raw[i * 2] - 55) : (rxbuffer_raw[i * 2] - 48);
                temp2 = (rxbuffer_raw[i * 2 + 1] >= 65) ? (rxbuffer_raw[i * 2 + 1] - 55) : (rxbuffer_raw[i * 2 + 1] - 48);
                rxbuffer[i] = (byte) ((temp1 << 4) + temp2);
            }
            rxbuffer = rxbuffer_raw;
            for(i=0; i < rxbuffer.length - 9; i++) {
                if (rxbuffer[i] == 0x01 && rxbuffer[i+1] == 0x040 && rxbuffer[i+4] != (byte) 0  && rxbuffer[i+5] != (byte) 0  && rxbuffer[i+6] != (byte) 0 && rxbuffer[i+7] != (byte) 0) {                          // transponder found
                    int type;
                    type = rxbuffer[i+1];
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
                        mTagType = String.format("%02X Hex", rxbuffer[i+1]);
                    mIDBitCount = String.format("%d Bits", rxbuffer[i+2]);
                    mID = String.format("%02X %02X %02X %02X", rxbuffer[i+4], rxbuffer[i+5], rxbuffer[i+6], rxbuffer[i+7]);
                    if (!mSendingThread.isInterrupted()) {
                        Log.d(TAG, mID);
                        cbCtx.success(mID);
                        mSendingThread.interrupt();
                    }
                    // short Tone
                    //toneGen1.startTone(ToneGenerator.TONE_PROP_ACK, 100);
                } else { // there is no transponder
                    //cbCtx.success(mID);
                    //Log.d(TAG, "Found no transponder: ");
                    //mSendingThread.interrupt();
                }
            }
        }
    }

    /*
    @Override
    protected void onDestroy() {
        if (mSendingThread != null)
            mSendingThread.interrupt();
        super.onDestroy();
    }*/
}

