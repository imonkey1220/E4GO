package tw.imonkey.e4go;//打卡機 deviceType

import android.app.TimePickerDialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.TimeZone;


public class DeviceTCActivity extends AppCompatActivity {
    TextView TVAtWork1,TVOffWork1,TVAtWork2,TVOffWork2,TVOverTime,TVOverTimeEnd;
    //基本設定:Start
    public static final String devicePrefs = "devicePrefs";
    public static final String service="TC";//打卡機 deviceType
    String deviceId, memberEmail;
    boolean master;
    DatabaseReference mDevice, mFriends;
    ArrayList<String> friends = new ArrayList<>();
    ListView deviceView;
    //基本設定:end
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicetc);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        init();
        TCSettings();
        mFriends=FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId+"/friend/");
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
                AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceTCActivity.this);
                LayoutInflater inflater = LayoutInflater.from(DeviceTCActivity.this);
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
                                        Toast.makeText(DeviceTCActivity.this, "已寄出邀請函(有效時間10分鐘)", Toast.LENGTH_LONG).show();
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
                AlertDialog.Builder dialog_list = new AlertDialog.Builder(DeviceTCActivity.this);
                dialog_list.setTitle("選擇要刪除的朋友");
                dialog_list.setItems(friends.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(DeviceTCActivity.this, "你要刪除是" + friends.get(which), Toast.LENGTH_SHORT).show();
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
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
        Bundle extras = getIntent().getExtras();
        deviceId = extras.getString("deviceId");
        memberEmail = extras.getString("memberEmail");
        master = extras.getBoolean("master");

        if (master) {
            mDevice = FirebaseDatabase.getInstance().getReference("/FUI/" + memberEmail.replace(".", "_") + "/" + deviceId);
            mDevice.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot != null) {
                        if (snapshot.child("connection").getValue() != null) {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "上線");
                        } else {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "離線");
                            Toast.makeText(DeviceTCActivity.this, "打卡機離線", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else {
            mDevice = FirebaseDatabase.getInstance().getReference("/FUI/" + memberEmail.replace(".", "_") + "/" + deviceId);
            mDevice.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        if (snapshot.child("connection").getValue() != null) {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "上線");
                        } else {
                            setTitle(snapshot.child("companyId").getValue().toString() + "." + snapshot.child("device").getValue().toString() + "." + "離線");
                            Toast.makeText(DeviceTCActivity.this, "打卡機離線", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }
    private void TCSettings(){
        TVAtWork1=(TextView) findViewById(R.id.textViewAtWork1);
        TVOffWork1=(TextView) findViewById(R.id.textViewOffWork1);
        TVAtWork2=(TextView) findViewById(R.id.textViewAtWork2);
        TVOffWork2=(TextView) findViewById(R.id.textViewOffWork2);
        TVOverTime=(TextView) findViewById(R.id.textViewOverTime);
        TVOverTimeEnd=(TextView) findViewById(R.id.textViewOverTimeEnd);

        TVAtWork1.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int hour =8;
            int minute = 0;
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(DeviceTCActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    TVAtWork1.setText("上班1\n"+ selectedHour + ":" + selectedMinute);
                    DatabaseReference mTCServer=FirebaseDatabase.getInstance().getReference("/TC/"+deviceId+"/SERVER/AtWork1");
                    mTCServer.setValue(selectedHour + ":" + selectedMinute);
                }
            }, hour, minute, true);//24 hour time
            mTimePicker.setTitle("選擇時間");
            mTimePicker.show();
        }
    });

        TVAtWork2.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int hour =13;
            int minute = 0;
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(DeviceTCActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    TVAtWork2.setText("上班2\n"+ selectedHour + ":" + selectedMinute);
                    DatabaseReference mTCServer=FirebaseDatabase.getInstance().getReference("/TC/"+deviceId+"/SERVER/AtWork2");
                    mTCServer.setValue(selectedHour + ":" + selectedMinute);
                }
            }, hour, minute, true);//24 hour time
            mTimePicker.setTitle("選擇時間");
            mTimePicker.show();
        }
    });

        TVOffWork1.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int hour =12;
            int minute = 0;
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(DeviceTCActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    TVOffWork1.setText("下班1\n"+ selectedHour + ":" + selectedMinute);
                    DatabaseReference mTCServer=FirebaseDatabase.getInstance().getReference("/TC/"+deviceId+"/SERVER/OffWork1");
                    mTCServer.setValue(selectedHour + ":" + selectedMinute);
                }
            }, hour, minute, true);//24 hour time
            mTimePicker.setTitle("選擇時間");
            mTimePicker.show();
        }
    });
        TVOffWork2.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int hour =17;
            int minute = 0;
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(DeviceTCActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    TVOffWork2.setText("下班2\n"+ selectedHour + ":" + selectedMinute);
                    DatabaseReference mTCServer=FirebaseDatabase.getInstance().getReference("/TC/"+deviceId+"/SERVER/OffWork2");
                    mTCServer.setValue(selectedHour + ":" + selectedMinute);
                }
            }, hour, minute, true);//24 hour time
            mTimePicker.setTitle("選擇時間");
            mTimePicker.show();
        }
     });
    }
}
