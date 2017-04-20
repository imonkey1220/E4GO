package tw.imonkey.e4go;//取號機 deviceType

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import net.glxn.qrgen.android.QRCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static tw.imonkey.e4go.MainActivity.myDeviceId;

public class DeviceQMSActivity extends AppCompatActivity {
    DatabaseReference mFriends, mDevice, mCountClient,mCountServer;
    DatabaseReference presenceRef,lastOnlineRef,connectedRef, mFriend,connectedRefF,mCountServerLive;

    ImageView QRImage ;
    TextView lastClientNo,lastServerNo,TVClientQR,TVServerCall;
    EditText EDServerDeviceNo;
    TextToSpeech tts ;
    Button resetCount ;
    Map<String, Object> mlastClientNo = new HashMap<>();
    Map<String, Object> mlastServerNo= new HashMap<>();

    public static final String devicePrefs = "devicePrefs";
    public static final String service="QMS"; //取號機 deviceType
    String deviceId, memberEmail,deviceNo;
    boolean master;
    ListView deviceView;
    ArrayList<String> friends = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deviceqms);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        init();
        CountClient();
        CountServer();
        mFriends=FirebaseDatabase.getInstance().getReference("/SHOP/"+deviceId+"/friend/");
        mFriends.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                friends.clear();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    friends.add(childSnapshot.getValue().toString());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (master) {
            getMenuInflater().inflate(R.menu.menu, menu);
            return true;
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_friend:
                AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceQMSActivity.this);
                LayoutInflater inflater = LayoutInflater.from(DeviceQMSActivity.this);
                final View v = inflater.inflate(R.layout.add_friend, deviceView, false);
                dialog.setTitle("邀請朋友加入服務");
                dialog.setView(v);
                dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final EditText editTextAddFriendEmail = (EditText) (v.findViewById(R.id.editTextAddFriendEmail));
                        if (!editTextAddFriendEmail.getText().toString().isEmpty()) {
                            DatabaseReference refDevice = FirebaseDatabase.getInstance().getReference("/DEVICE/" + deviceId);
                            refDevice.child("friend").push().setValue(editTextAddFriendEmail.getText().toString());
                            refDevice.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.getValue() != null) {
                                        Device device = snapshot.getValue(Device.class);
                                        DatabaseReference mInvitation = FirebaseDatabase.getInstance().getReference("/friend/" + editTextAddFriendEmail.getText().toString().replace(".", "_") + "/" + deviceId);
                                        mInvitation.setValue(device);
                                        Toast.makeText(DeviceQMSActivity.this, "已寄出邀請函(有效時間10分鐘)", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError firebaseError) {

                                }
                            });
                        }
                        dialog.cancel();
                    }
                });
                dialog.show();

                return true;

            case R.id.action_del_friend:
                AlertDialog.Builder dialog_list = new AlertDialog.Builder(DeviceQMSActivity.this);
                dialog_list.setTitle("選擇要刪除的朋友");
                dialog_list.setItems(friends.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(DeviceQMSActivity.this, "你要刪除是" + friends.get(which), Toast.LENGTH_SHORT).show();
                        mFriends.orderByValue().equalTo(friends.get(which)).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                    childSnapshot.getRef().removeValue();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                            }
                        });
                        friends.remove(which);
                    }
                });
                dialog_list.show();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }


    private void init(){
        resetCount=(Button)findViewById(R.id.buttonReset);

        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.TAIWAN);
                }
            }
        });
        Bundle extras = getIntent().getExtras();
        deviceId = extras.getString("deviceId");
        memberEmail = extras.getString("memberEmail");
        master = extras.getBoolean("master");
        if(myDeviceId.equals(deviceId)){
            myDeviceIdLive();
        }

        if (master) {
            mDevice = FirebaseDatabase.getInstance().getReference("/master/" + memberEmail.replace(".", "_") + "/" + deviceId);
            mDevice.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot != null) {
                        if (snapshot.child("connection").getValue() != null) {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "上線");
                        } else {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "離線");
                            Toast.makeText(DeviceQMSActivity.this, "取號機離線", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

        } else {
            resetCount.setVisibility(View.INVISIBLE);
            mDevice = FirebaseDatabase.getInstance().getReference("/friend/" + memberEmail.replace(".", "_") + "/" + deviceId);
            mDevice.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        if (snapshot.child("connection").getValue() != null) {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "上線");
                        } else {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "離線");
                            Toast.makeText(DeviceQMSActivity.this, "取號機離線", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
        TVClientQR=(TextView) findViewById(R.id.textViewClientQR);
        QRImage=(ImageView) findViewById(R.id.imageViewQR);
        TVServerCall=(TextView)findViewById(R.id.textViewServerTitle);
        TVServerCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tts !=null){
                    tts.stop();
                    tts.shutdown();
                }
            }
        });
        TVClientQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = QRCode.from(encry(deviceId+":"+service+":"+lastClientNo.getText()+":"+mCountClient.push().getKey())).withSize(250, 250).bitmap();
                QRImage.setImageBitmap(bitmap);
                QRImage.setVisibility(View.VISIBLE);
            }
        });

        QRImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QRImage.setVisibility(View.INVISIBLE);
            }
        });

    }
    private void CountClient(){
        lastClientNo=(TextView) findViewById(R.id.textViewLastClientNo);
        mCountClient= FirebaseDatabase.getInstance().getReference("/QMS/"+deviceId+"/CLIENT");
        mCountClient.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                lastClientNo.setText(dataSnapshot.child("message").getValue().toString());
                QRImage.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        lastClientNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlastClientNo.clear();
                mlastClientNo.put("message",Integer.parseInt(lastClientNo.getText().toString())+1);
                mlastClientNo.put("memberEmail",memberEmail);
                mlastClientNo.put("timeStamp", ServerValue.TIMESTAMP);
                mCountClient.push().setValue(mlastClientNo);

            }
        });

        lastClientNo.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                mlastClientNo.clear();
                mlastClientNo.put("message",Integer.parseInt(lastClientNo.getText().toString())-1);
                mlastClientNo.put("memberEmail",memberEmail);
                mlastClientNo.put("timeStamp", ServerValue.TIMESTAMP);
                mCountClient.push().setValue(mlastClientNo);
                return true ;
            }
        });

    }
    private void CountServer(){
        lastServerNo=(TextView) findViewById(R.id.textViewLastServerNo);
        EDServerDeviceNo= (EditText) (findViewById(R.id.editTextServerDeviceNo));
        mCountServer= FirebaseDatabase.getInstance().getReference("/QMS/"+deviceId+"/SERVER");
        mCountServer.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                lastServerNo.setText(dataSnapshot.child("message").getValue().toString());
                if (myDeviceId.equals(deviceId)) {
                    String toSpeak = "號碼牌" + dataSnapshot.child("message").getValue().toString() + "號";
                    tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
                }
              }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        lastServerNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlastServerNo.clear();
                mlastServerNo.put("message",Integer.parseInt(lastServerNo.getText().toString())+1);
                mlastServerNo.put("memberEmail",memberEmail);

                if (!TextUtils.isEmpty(EDServerDeviceNo.getText().toString().trim())) {
                    deviceNo=EDServerDeviceNo.getText().toString();
                    mlastServerNo.put("deviceNo", deviceNo);
                }
                mlastServerNo.put("timeStamp", ServerValue.TIMESTAMP);
                mCountServer.push().setValue(mlastServerNo);
            }
        });

        lastServerNo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mlastServerNo.clear();
                mlastServerNo.put("message",Integer.parseInt(lastServerNo.getText().toString())-1);
                mlastServerNo.put("memberEmail",memberEmail);

                if (!TextUtils.isEmpty(EDServerDeviceNo.getText().toString().trim())) {
                    deviceNo=EDServerDeviceNo.getText().toString();
                    mlastServerNo.put("deviceNo", deviceNo);
                }
                mlastServerNo.put("timeStamp", ServerValue.TIMESTAMP);
                mCountServer.push().setValue(mlastServerNo);
                return true ;
            }
        });
    }
    public void reset(View v){
        mlastServerNo.clear();
        mlastServerNo.put("message",1);
        mlastServerNo.put("memberEmail",memberEmail);
        mlastServerNo.put("timeStamp", ServerValue.TIMESTAMP);
        mCountServer.push().setValue(mlastServerNo);

        mlastClientNo.clear();
        mlastClientNo.put("message",1);
        mlastClientNo.put("memberEmail",memberEmail);
        mlastClientNo.put("timeStamp", ServerValue.TIMESTAMP);
        mCountClient.push().setValue(mlastClientNo);
    }

    private void myDeviceIdLive(){
        mCountServerLive=FirebaseDatabase.getInstance().getReference("/QMS/"+deviceId+"/connection");
        mCountServerLive.setValue(true);
        mCountServerLive.onDisconnect().setValue(null);

        presenceRef = FirebaseDatabase.getInstance().getReference("/master/"+memberEmail.replace(".", "_")+"/"+deviceId+"/connection");
        presenceRef.setValue(true);
        presenceRef.onDisconnect().setValue(null);
        lastOnlineRef =FirebaseDatabase.getInstance().getReference("/master/"+memberEmail.replace(".", "_")+"/"+deviceId+"/lastOnline");
        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    presenceRef.setValue(true);
                    mCountServerLive.setValue(true);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        mFriend= FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId+"/friend/");
        mFriend.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    final DatabaseReference  presenceRefF= FirebaseDatabase.getInstance().getReference("/friend/"+childSnapshot.getValue().toString().replace(".", "_")+"/"+deviceId+"/connection");//childSnapshot.getValue().toString():email
                    presenceRefF.setValue(true);
                    presenceRefF.onDisconnect().setValue(null);
                    connectedRefF = FirebaseDatabase.getInstance().getReference(".info/connected");
                    connectedRefF.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Boolean connected = snapshot.getValue(Boolean.class);
                            if (connected) {
                                presenceRefF.setValue(true);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void addMember(View v){
        Intent intent = new Intent(DeviceQMSActivity.this,ClubActivity.class);
        intent.putExtra("deviceId",deviceId);
        intent.putExtra("memberEmail",memberEmail);
        intent.putExtra("master",master);
        intent.putExtra("number",lastClientNo.getText().toString());
        startActivity(intent);
        finish();
    }

    private String encry(String normalText){
        // encrypt and decrypt using AES Algorithms
        try {
            String seedValue = "imonkey.tw";
            String normalTextEnc;
            normalTextEnc = AESHelper.encrypt(seedValue, normalText);
            //       Toast.makeText(MainActivity.this,AESHelper.decrypt(seedValue,normalTextEnc),Toast.LENGTH_LONG).show();
            return  normalTextEnc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "err";
    }

}

