package fi.hiit.complesense.service;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import fi.hiit.complesense.Constants;
import fi.hiit.complesense.connection.Acceptor;
import fi.hiit.complesense.connection.Connector;
import fi.hiit.complesense.core.AbstractSystemThread;
import fi.hiit.complesense.core.ClientManager;
import fi.hiit.complesense.core.GroupOwnerManager;
import fi.hiit.complesense.core.GroupOwnerServiceHandler;
import fi.hiit.complesense.core.LocalDeviceManager;
import fi.hiit.complesense.core.ServiceHandler;
import fi.hiit.complesense.core.SystemMessage;
import fi.hiit.complesense.util.SensorUtil;
import fi.hiit.complesense.util.SystemUtil;

/**
 * Created by hxguo on 7/11/14.
 */
public class TestingService extends AbstractGroupService
{
    public static final int NUM_CLIENTS = 1;
    public static final int START_TESTING = 2;
    public static final int STOP_TESTING = 3;
    private ArrayList<LocalDeviceManager> clientsList;
    private LocalDeviceManager serverManager;

    private static final String TAG = "TestingService";
    // Binder given to clients
    private InetAddress localHost;
    private Messenger uiMessenger;

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case START_TESTING:
                    startTesting(msg.replyTo, NUM_CLIENTS);
                    break;
                case STOP_TESTING:
                    stopSelf();
                    break;
                case Constants.SERVICE_MSG_STATUS_TXT_UPDATE:
                    SystemUtil.sendStatusTextUpdate(uiMessenger,
                            (String) msg.obj);
                    break;
               default:
                    super.handleMessage(msg);
            }
        }
    }



    @Override
    public void onCreate()
    {
        Log.i(TAG,"onCreate()");
        clientsList = new ArrayList<LocalDeviceManager>(NUM_CLIENTS);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    localHost = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();

        mMessenger = new Messenger(new IncomingHandler());
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        // Don't restart the GroupOwnerService automatically if its
        // process is killed while it's running.
        return Service.START_NOT_STICKY;
    }


    public void testSensorListParsing()
    {
        Log.i(TAG,"testSensorListParsing()");
        List<Integer> sList= SensorUtil.getLocalSensorTypeList(getApplicationContext());
        Log.i(TAG,sList.toString());

        SystemMessage sm = SystemMessage.makeSensorsListReplyMessage(sList);

        SystemMessage.parseSensorTypeList(sm);

        int a = 13;
        byte[] bArray = SystemMessage.int2Bytes(a);
        Log.i(TAG, Integer.toString(SystemMessage.byteArray2Int(bArray)) );

    }

    /*
    public void startTesting(Messenger uiMessenger, int numClients)
    {
        Log.i(TAG,"startTesting(client="+numClients+")");


        if(localHost==null)
            return;

        this.uiMessenger = uiMessenger;
        Log.i(TAG,"Creating GroupOwner thread");

        //serverManager = new GroupOwnerManager(mMessenger,
        //        getApplication(), false);

        try {
            serverManager.start();
        } catch (IOException e) {
            Log.i(TAG, e.toString());
        }

        for(int i=0;i<NUM_CLIENTS;i++)
        {
            Log.i(TAG,"Creating client thread");
            clientsList.add(new ClientManager(mMessenger,
                    getApplication(), false ));
        }

        for(int i=0;i<NUM_CLIENTS;i++)
        {
            Log.i(TAG, "Starting client thread");
            try {
                clientsList.get(i).start(localHost, (int)(Math.random()*1000) );
            } catch (IOException e) {
                Log.i(TAG,e.toString());
            }
        }

    }
    */
    public void startTesting(Messenger uiMessenger, int numClients)
    {
        Log.i(TAG,"startTesting(client="+numClients+")");


        if(localHost==null)
            return;

        this.uiMessenger = uiMessenger;


        //serverManager = new GroupOwnerManager(mMessenger,
        //        getApplication(), false);
        GroupOwnerServiceHandler ownerServiceHanlder = new GroupOwnerServiceHandler(mMessenger, "Testing Service Handler");
        ServiceHandler clientServiceHanlder = new ServiceHandler(mMessenger, "Testing Service Handler");

        Log.i(TAG,"Creating GroupOwner thread");
        serverManager = new LocalDeviceManager(mMessenger, ownerServiceHanlder, null);
        serverManager.start();

        for(int i=0;i<NUM_CLIENTS;i++)
        {
            Log.i(TAG,"Creating client thread");
            clientsList.add(new LocalDeviceManager(mMessenger, clientServiceHanlder, localHost));
        }

        for(int i=0;i<NUM_CLIENTS;i++)
        {
            Log.i(TAG, "Starting client thread");
            clientsList.get(i).start();
        }
    }



    @Override
    protected void start()
    {

    }

    @Override
    protected void stop()
    {
        Log.i(TAG, "stopTesting()");
        Log.e(TAG,"num of clients: " + clientsList.size());

        for(int i=0;i<NUM_CLIENTS;i++)
        {
            Log.i(TAG, "Stopping client thread");
            clientsList.get(i).stop();
        }
        clientsList.clear();
        serverManager.stop();

    }

}
