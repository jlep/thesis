package fi.hiit.complesense.audio;

import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import fi.hiit.complesense.Constants;
import fi.hiit.complesense.core.AbsSystemThread;
import fi.hiit.complesense.core.ServiceHandler;
import fi.hiit.complesense.util.MIME_FileWriter;

/**
 * Created by hxguo on 24.11.2014.
 */
public class AudioFileWritingThread extends AbsSystemThread {

    private static final String TAG = AudioFileWritingThread.class.getSimpleName();
    private final File outputFile;
    private final DataInputStream dis;

    public AudioFileWritingThread(ServiceHandler serviceHandler, InputStream is, File outputFile) {
        super(TAG, serviceHandler);
        this.dis = new DataInputStream(is);
        this.outputFile = outputFile;
    }

    @Override
    public void run()
    {
        long threadID = Thread.currentThread().getId();
        Log.i(TAG, "Starts AudioFileWritingThread @thread: " + threadID);
        serviceHandler.workerThreads.put(AudioFileWritingThread.TAG, this);

        byte[] buf = new byte[Constants.BUF_SIZE];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        MIME_FileWriter fileWriter = null;

        keepRunning = true;
        int byteCount = 0;
        try{
            fileWriter = new MIME_FileWriter( outputFile, MIME_FileWriter.Format.wav);

            while(keepRunning){
                int bytesRead = dis.read(buf, 0, buf.length);
                if (bytesRead == -1) break;
                byteCount += bytesRead;
                fileWriter.write(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }finally {
            Log.i(TAG, "AudioFileWritingThread stop, byteCount: " + byteCount);
            if(dis!=null){
                try {
                    dis.close();
                } catch (IOException e) { }
            }
            if(fileWriter!=null)
                fileWriter.close();
        }
    }

    @Override
    public void stopThread() {
        keepRunning = false;
    }
}
