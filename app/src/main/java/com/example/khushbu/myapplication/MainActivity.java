package com.example.khushbu.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.icu.util.Output;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button listen, listDevices, send;
    private EditText status;
    private TextView messageBox, device_name;
    ListView listView,listMsg;


    //TODO changes2
    private List<ChatMessage> chatMessages;
    private boolean isMine = true;
    private ArrayAdapter<ChatMessage> adapter;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btarray;

    SendRecieve sendRecieve;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSaGE_RECIEVED = 5;

    int REQUEST_ENABLE_BLUETOOTH = 1;

    private static final String APP_NAME = "MyApplication";
    private static final UUID MY_UUID = UUID.fromString("c365018e-79ec-46c6-b380-e9e78981fdbb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listen = findViewById(R.id.listen);
        listDevices = findViewById(R.id.listDevices);
        send = findViewById(R.id.send);
        status = findViewById(R.id.status);
        messageBox = findViewById(R.id.message);
        device_name = findViewById(R.id.device_name);
        listView = findViewById(R.id.listview_devices);
        listMsg = findViewById(R.id.listview_devices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        }

        implementListeners();
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ServerClass serverClass = new ServerClass();
                    serverClass.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    ClientClass clientClass = new ClientClass(btarray[i]);
                    clientClass.start();
                    device_name.setText("CONNECTING");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sendText = String.valueOf(status.getText());
                try {
                    sendRecieve.write(sendText.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });



        //TODO CHANGES

        chatMessages = new ArrayList<>();

        //set ListView adapter first
        adapter = new chatAdapter(this, R.layout.chat_left, chatMessages);
        listMsg.setAdapter(adapter);

    }

    private void implementListeners() {
        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> bt = bluetoothAdapter.getBondedDevices();
                String[] strings = new String[bt.size()];

                btarray = new BluetoothDevice[bt.size()];
                int index = 0;
                if (bt.size() > 0) {
                    for (BluetoothDevice device : bt) {
                        Log.e("nullpointer ", device.getName()+"");
                        btarray[index] = device;
                        strings[index] = device.getName();
                        index++;
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case STATE_LISTENING:
                    device_name.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    device_name.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    device_name.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    device_name.setText("Connecton failed");
                    break;
                case STATE_MESSaGE_RECIEVED:

                    byte[] readBuffer = (byte[]) message.obj;

                    String tempString = new String(readBuffer,0,message.arg1);
                    messageBox.setText(tempString);


                    //TODO changes

                    ChatMessage chatMessage = new ChatMessage(tempString, isMine);
                    chatMessages.add(chatMessage);
                    adapter.notifyDataSetChanged();
                    if (isMine) {
                        isMine = false;
                    } else {
                        isMine = true;
                    }

//                    status.setText("msg received");
                    break;
            }
            return true;
        }
    });


    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass() throws IOException {
            serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
        }

        public void run() {
            BluetoothSocket socket = null;
            while (socket == null) {
                try {

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);

                }

                if (socket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    try {
                        sendRecieve = new SendRecieve(socket);
                        sendRecieve.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread {
        BluetoothDevice device;
        BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1) throws IOException {
            device=device1;
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        }

        public void run(){
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendRecieve = new SendRecieve(socket);
                sendRecieve.start();

            } catch (IOException e) {

                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);

                e.printStackTrace();
            }
        }
    }

    private class SendRecieve extends  Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendRecieve(BluetoothSocket bluetoothSocket) throws IOException {
            this.bluetoothSocket = bluetoothSocket;
            InputStream tempin = null;
            OutputStream tempout= null;

            tempin = bluetoothSocket.getInputStream();
            tempout = bluetoothSocket.getOutputStream();

            inputStream = tempin;
            outputStream = tempout;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes= inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSaGE_RECIEVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) throws IOException {
            outputStream.write(bytes);
        }
    }
}
