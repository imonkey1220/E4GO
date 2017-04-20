package tw.imonkey.e4go;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QRActivity extends AppCompatActivity {
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;
    SurfaceView cameraView;
    TextView barcodeValue;
    DatabaseReference mCountClient,mShop,mAddClub;
    Map<String, Object> countClient = new HashMap<>();
    String memberEmail,deviceId,number;
    Boolean master ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        Bundle extras = getIntent().getExtras();
        memberEmail = extras.getString("memberEmail");
        deviceId = extras.getString("deviceId");
        number= extras.getString("number");
        master = extras.getBoolean("master");
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeValue = (TextView) findViewById(R.id.code_info);
        takeCard();
    }
    public void takeCard(){

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        if (!barcodeDetector.isOperational()) {
            barcodeValue.setText("Could not set up the detector!");
            return;
        }

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    //noinspection MissingPermission
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    barcodeValue.post(new Runnable() {
                        @Override
                        public void run() {
                            barcodeValue.setText(barcodes.valueAt(0).displayValue);
                            barcodeDetected(barcodes.valueAt(0).displayValue);
                            cameraSource.stop();
                        }
                    });
                }
            }
        });
    }

    private void barcodeDetected(String s){
        barcodeValue.setText(number);
        mCountClient= FirebaseDatabase.getInstance().getReference("/NUMBER/"+deviceId+"/CLIENT");
        countClient.clear();
        countClient.put("message",Integer.parseInt(number)+1);
        countClient.put("memberEmail",s);
        countClient.put("timeStamp", ServerValue.TIMESTAMP);
        mCountClient.push().setValue(countClient);
        mShop= FirebaseDatabase.getInstance().getReference("/SHOP/"+deviceId);
        mAddClub=FirebaseDatabase.getInstance().getReference("/CLUB/" + s + "/SHOP/");
        mShop.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()){
                    Shop shop=snapshot.getValue(Shop.class);
                    mAddClub.child(shop.getTopics_id()).setValue(shop);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        Intent intent = new Intent(this,DeviceQMSActivity.class);
        intent.putExtra("deviceId",deviceId);
        intent.putExtra("memberEmail",memberEmail);
        intent.putExtra("master",master);
        startActivity(intent);
        finish();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource!=null) {
            cameraSource.release();
        }
        if (barcodeDetector!=null) {
            barcodeDetector.release();
        }
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
