package tw.imonkey.e4go;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class DeviceBossActivity extends AppCompatActivity {
    public static final String service="Boss"; //主機 deviceType

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deviceboss);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


    }

        public void onClickLogout(View v) {
                  AuthUI.getInstance().signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                // user is now signed out
                                startActivity(new Intent(DeviceBossActivity.this, LoginActivity.class));
                                finish();
                            }
                        });

        }

    public void onClickAddThingsDevice(View v) {

        Intent intent = new Intent(DeviceBossActivity.this, AddThingsDeviceActivity.class);
        startActivity(intent);
        finish();

    }

    }


