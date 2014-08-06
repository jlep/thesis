package fi.hiit.complesense.connection.local;

import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.List;

import fi.hiit.complesense.connection.AbstractUdpConnectionRunnable;
import fi.hiit.complesense.core.GroupOwnerManager;
import fi.hiit.complesense.core.ScheduledUdpQueryTask;
import fi.hiit.complesense.core.SystemMessage;

/**
 * Created by hxguo on 7/22/14.
 */
public class GroupOwnerUdpConnectionRunnable extends AbstractUdpConnectionRunnable
{
    private String TAG = "GroupOwnerUdpConnectionRunnable";
    private GroupOwnerManager groupOwnerManager;
    private SocketAddress remoteSocketAddr;

    public GroupOwnerUdpConnectionRunnable(DatagramSocket s,
                                           GroupOwnerManager groupOwnerManager,
                                           Messenger remoteMessenger)
            throws IOException
    {
        super(s, remoteMessenger);
        this.groupOwnerManager = groupOwnerManager;
    }

    @Override
    public void run()
    {
        Log.i(TAG, "run()");
        //requestBarometerValues();

        Log.i(TAG,"Query available sensors on the connected client");
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                socket.receive(recPacket);
                remoteSocketAddr = recPacket.getSocketAddress();
                parseSystemMessage(SystemMessage.getFromBytes(
                        recPacket.getData()));

                if(Thread.currentThread().isInterrupted())
                    throw new InterruptedException();

            } catch (IOException e)
            {
                Log.e(TAG, "disconnected" + e.toString());
                break;
            } catch (InterruptedException e)
            {
                Log.e(TAG, e.toString());
                break;
            }
        }

        Log.w(TAG,"Group Owner UDP connection terminates..");
    }

    @Override
    protected void parseSystemMessage(SystemMessage sm)
    {
        float[] values;
        int type;
        Log.i(TAG,sm.toString());
        switch (sm.getCmd())
        {
            case SystemMessage.INIT:
                write(SystemMessage.makeAudioStreamingRequest(),remoteSocketAddr);
                break;
            case SystemMessage.Y:

                break;
            case SystemMessage.R:
                break;

            case SystemMessage.V:

                type = SystemMessage.parseSensorType(sm);
                values = SystemMessage.parseSensorValues(sm);

                groupOwnerManager.setSensorValues(values, type, remoteSocketAddr.toString());
                try {
                    updateStatusTxt(remoteSocketAddr + "->: " + sm.toString());
                } catch (RemoteException e) {
                    Log.i(TAG,e.toString());
                }
                break;

            case SystemMessage.N:
                List<Integer> typeList = SystemMessage.parseSensorTypeList(sm);

                groupOwnerManager.registerSensors(remoteSocketAddr.toString(), typeList);
                Log.i(TAG,remoteSocketAddr + ":" +
                        groupOwnerManager.getSensorsList(remoteSocketAddr.toString()).toString());
                //Log.i(TAG,SystemMessage.parseSensorTypeList(sm).toString() );

                int sType = groupOwnerManager.randomlySelectSensor(typeList, remoteSocketAddr.toString());

                write(SystemMessage.makeAudioStreamingRequest(), remoteSocketAddr);

                ScheduledUdpQueryTask sTask = new ScheduledUdpQueryTask(this, groupOwnerManager);
                timer.schedule(sTask, 0, 2000);

                break;
            default:
                break;
        }

    }

    public SocketAddress getRemoteSocketAddr() {
        return remoteSocketAddr;
    }
}
