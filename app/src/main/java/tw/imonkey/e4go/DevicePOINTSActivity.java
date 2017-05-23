package tw.imonkey.e4go;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DevicePOINTSActivity extends AppCompatActivity {

    public static final String service="POINTS"; //集點機 deviceType
    DatabaseReference mPOINTSACT ;
    Map<String, Object> mPOINTSNo=new HashMap<>();

    public static final String devicePrefs = "devicePrefs";
    DatabaseReference mFriends, mDevice , mPointChanged;
    String deviceId, memberEmail;
    Integer buyTimes=0;
    int ACT;
    boolean master;
    Spinner spBuyTimes;
    EditText ETDescription;
    TextView TVChanged;
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
        ETDescription=(EditText) findViewById(R.id.editTextPOINTSDescription);
        TVChanged=(TextView) findViewById(R.id.textViewChanged);

        mDevice=FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId);
        mDevice.child("ACT").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot==null){
                    ACT=0;
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

        // Spinner element
         spBuyTimes= (Spinner) findViewById(R.id.spinnerBuyTimes);
        // Spinner Drop down elements
        final List<Integer> items = new ArrayList<>();
        items.add(0);
        items.add(1);
        items.add(2);
        items.add(3);
        items.add(4);
        items.add(5);
        items.add(6);
        items.add(7);
        items.add(8);
        items.add(9);
        items.add(10);
        items.add(15);
        items.add(20);
        items.add(25);
        items.add(30);
        items.add(40);
        items.add(50);
        items.add(100);
        // Creating adapter for spinner
        ArrayAdapter<Integer> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spBuyTimes.setAdapter(dataAdapter);
        spBuyTimes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(DevicePOINTSActivity.this, "你選的是" + items.get(position), Toast.LENGTH_SHORT).show();
                buyTimes=items.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mPointChanged=FirebaseDatabase.getInstance().getReference("/DEVICE/"+ deviceId+"/CHANGED");
        mPointChanged.limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Calendar timeStamp = Calendar.getInstance();
                timeStamp.setTimeInMillis(Long.parseLong((snapshot.child("timeStamp").getValue().toString())));
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss MM/dd", Locale.TAIWAN);
                TVChanged.setText(snapshot.child("memberEmail").getValue().toString()+"@"+df.format(timeStamp.getTime()+"  兌換"));
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
                        mDevice.child("ACT").setValue(ACT+1);
                        mDevice.child("BUYTIMES").setValue(buyTimes);
                        mDevice.child("DESCRIPTION").setValue(ETDescription.getText());
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }
}
