package tw.imonkey.e4go;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import net.glxn.qrgen.android.QRCode;

import java.util.HashMap;
import java.util.Map;

public class ClubActivity extends AppCompatActivity {
    String memberEmail, deviceId, number;
    boolean master;
    Map<String, Object> countClient = new HashMap<>();
    DatabaseReference mCountClient;
    TextView TVQRGClub, TVQRClub;
    ImageView IVQRGClub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Bundle extras = getIntent().getExtras();
        memberEmail = extras.getString("memberEmail");
        deviceId = extras.getString("deviceId");
        number= extras.getString("number");
        master = extras.getBoolean("master");
        TVQRClub = (TextView) findViewById(R.id.textViewQRMember);
        TVQRGClub = (TextView) findViewById(R.id.textViewQRGMember);
        IVQRGClub = (ImageView) findViewById(R.id.imageViewQRMember);

        TVQRGClub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVQRGClub.setVisibility(View.VISIBLE);
                mCountClient = FirebaseDatabase.getInstance().getReference("/NUMBER/" + deviceId + "/CLIENT");
                Bitmap bitmap = QRCode.from(mCountClient.push().getKey()).withSize(250, 250).bitmap();
                IVQRGClub.setImageBitmap(bitmap);
            }
        });

        IVQRGClub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVQRGClub.setVisibility(View.INVISIBLE);
            }
        });

        TVQRClub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ClubActivity.this,QRActivity.class);
                intent.putExtra("deviceId",deviceId);
                intent.putExtra("memberEmail",memberEmail);
                intent.putExtra("number",number);
                startActivity(intent);
                finish();

            }
        });

    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,DeviceQMSActivity.class);
        intent.putExtra("deviceId",deviceId);
        intent.putExtra("memberEmail",memberEmail);
        intent.putExtra("master",master);
        startActivity(intent);
        finish();
    }
}
