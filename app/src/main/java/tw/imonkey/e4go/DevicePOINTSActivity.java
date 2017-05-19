package tw.imonkey.e4go;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DevicePOINTSActivity extends AppCompatActivity {

    public static final String service="POINTS"; //集點機 deviceType
    DatabaseReference mPOINTSACT ;
    Map<String, Object> mPOINTSNo=new HashMap<>();

    public static final String devicePrefs = "devicePrefs";
    DatabaseReference mFriends, mDevice;
    String deviceId, memberEmail;
    int ACT;
    boolean master;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicepoints);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bundle extras = getIntent().getExtras();
        deviceId = extras.getString("deviceId");
        memberEmail = extras.getString("memberEmail");
        master = extras.getBoolean("master");
        mDevice=FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId+"/ACT/");
        mDevice.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot==null){
                    ACT=1;
                }else {
                    ACT = Integer.parseInt(snapshot.getValue().toString());
                }
                mPOINTSACT= FirebaseDatabase.getInstance().getReference("/LOG/"+service+"/"+deviceId+"/"+ACT+"/");
                mPOINTSNo.clear();
                mPOINTSNo.put("message",1);
                mPOINTSNo.put("memberEmail",memberEmail);
                mPOINTSNo.put("timeStamp", ServerValue.TIMESTAMP);
                mPOINTSACT.push().setValue(mPOINTSNo);
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

    }

    public void reset(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("開始新的集點活動?")
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mDevice.setValue(ACT+1);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }
}
