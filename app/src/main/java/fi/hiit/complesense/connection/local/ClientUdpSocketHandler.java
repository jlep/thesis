package fi.hiit.complesense.connection.local;

import android.os.Messenger;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import fi.hiit.complesense.Constants;
import fi.hiit.complesense.connection.AbstractUdpSocketHandler;
import fi.hiit.complesense.core.ClientManager;

/**
 * Created by hxguo on 7/22/14.
 */
public class ClientUdpSocketHandler extends AbstractUdpSocketHandler
{

    private static final String TAG = "ClientUdpSocketHandler";
    private final InetSocketAddress ownerAddr;
    private final int delay;
    private final ClientManager clientManager;
    private Thread mThread;


    public ClientUdpSocketHandler(Messenger remoteMessenger,
                               ClientManager clientManager, InetAddress ownerAddr, int delay)
    {
        super(remoteMessenger);
        this.clientManager = clientManager;
        this.ownerAddr = new InetSocketAddress(ownerAddr, Constants.SERVER_PORT);
        this.delay = delay;
    }

    @Override
    public void run()
    {
        if(delay>0)
            Log.i(TAG, "Starting client in " + delay + "ms");
        DatagramSocket socket = null;
        try
        {
            socket = new DatagramSocket();
            socket.connect(ownerAddr);
            Log.i(TAG, ownerAddr.toString());
            Log.i(TAG, "Launching the I/O handler at: " + socket.getLocalSocketAddress().toString());

            connectionRunnable = new ClientUdpConnectionRunnable(socket,
                    clientManager, ownerAddr, remoteMessenger);
            mThread = new Thread(connectionRunnable);
            mThread.start();
        }
        catch (IOException e)
        {
            Log.i(TAG, e.toString() );
            AbstractUdpSocketHandler.closeSocket(socket);
            return;
        }

    }

    @Override
    public void stopHandler()
    {
        Log.i(TAG, "stopHandler()");
        if(connectionRunnable!=null)
        {
            connectionRunnable.signalStop();
            mThread.interrupt();
        }

    }

    public ClientUdpConnectionRunnable getConnectionRunnable()
    {
        return (ClientUdpConnectionRunnable)connectionRunnable;
    }
}
