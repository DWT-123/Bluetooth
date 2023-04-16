package example.dwt.bluetooth1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String  DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;
    public int a = 0;
    public int b = 0;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private Button mSendButton1;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter;
    private ChatService mChatService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);// savedInstanceState是保存当前Activity的状态信息
        setContentView(R.layout. activity_main);
        // 得到本地蓝牙适配器
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        // 若当前设备不支持蓝牙功能
        if(mBluetoothAdapter == null){
            Toast.makeText(this,"蓝牙不可用",Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
    @Override
    public void onStart(){
        super.onStart();
        if(!mBluetoothAdapter.isEnabled()){
            // 若当前设备蓝牙功能未开启，则开启蓝牙
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        } else{
            if(mChatService==null)
                setupChat();
        }
    }
    @Override
    public synchronized void onResume(){
        super.onResume();

        if(mChatService != null)
            if(mChatService.getState() == ChatService.STATE_NONE)
                mChatService.start();
    }
    @Override
    public synchronized void onPause(){
        super.onPause();
    }
    @Override
    public synchronized void onStop(){
        super.onStop();
    }
    @Override
    public synchronized void onDestroy(){
        super.onDestroy();
        if(mChatService != null)
            mChatService.stop();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 菜单栏
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.scan:
                Intent serverIntent=new Intent(this,DeviceList.class);
                startActivityForResult(serverIntent,REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                ensureDiscoverable();
                return true;
            case R.id.BtOpen:
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, REQUEST_ENABLE_BT);
                }
                return true;
            case R.id.BtOff:
                mBluetoothAdapter.disable();

                return true;
        }
        return false;
    }
//蓝牙设备设置为可见状态
    private void ensureDiscoverable(){
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);//可见时间
            startActivity(discoverableIntent);
        }
    }
    private void sendMessage(String message){
        if(mChatService.getState() != ChatService.STATE_CONNECTED){
            Toast.makeText(this,R.string.not_connected,Toast.LENGTH_SHORT).show();
            return;
        }

        if(message.length() > 0){
            byte[] send=message.getBytes();
            mChatService.write(send);
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }
    private void setupChat(){
        mConversationArrayAdapter=new ArrayAdapter<String>(this, R.layout.list_item);

        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText=(EditText)findViewById(R.id.edit_text_out);
        mSendButton = (Button)findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mOutEditText.getText().toString();
                sendMessage(message);
            }
        });
        mSendButton1 = (Button)findViewById(R.id.button1);
        mSendButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (a==0) {
                    a= 1;
                    mSendButton1.setText("关闭灯");
                    sendMessage("1");
                }else
                {  sendMessage("2");
                    a= 0;
                    mSendButton1.setText("打开灯");
                }
    }
        });
        final Button mSendButton2 = (Button)findViewById(R.id.button2);
        mSendButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(b==0)
                {
                    b=1;
                    mSendButton2.setText("关闭风扇");
                    sendMessage("3");
                }else{
                    sendMessage("4");
                    b=0;
                    mSendButton2.setText("打开风扇");
                }
            }
        });
        mChatService = new ChatService(this,mHandler);
        mOutStringBuffer=new StringBuffer("");
    }
//传递消息Message
    private final Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case ChatService.STATE_CONNECTED:
                            mConversationArrayAdapter.clear();
                            break;
                        case ChatService.STATE_CONNECTING:
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            break;
                    }break;
                case MESSAGE_WRITE:
                    byte[]writeBuf =(byte[])msg.obj;
                    String writeMessage=new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[]readBuf =(byte[])msg.obj;
                    String readMessage=new String(readBuf,0,msg.arg1);
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName=msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(),
                            "连接到"+mConnectedDeviceName,Toast.LENGTH_SHORT)
                            .show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST),Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };
    public void onActivityResult(int requesstCode, int resultCode, Intent data){
        switch (requesstCode){
            case REQUEST_CONNECT_DEVICE:
                if(resultCode== Activity.RESULT_OK){
                    String address=data.getExtras().getString(DeviceList.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device=mBluetoothAdapter.getRemoteDevice(address);
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK){
                    setupChat();
                }else {
                    Toast.makeText(this, "bt_not_enable_leaving",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
}