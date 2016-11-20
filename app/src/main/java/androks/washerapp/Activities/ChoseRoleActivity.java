package androks.washerapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import androks.washerapp.R;

public class ChoseRoleActivity extends AppCompatActivity implements View.OnClickListener {
    public Button driver, owner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chose_role);
        //Hello, Second commit
        driver = (Button) findViewById(R.id.driver);
        owner = (Button) findViewById(R.id.washer_owner);
        driver.setOnClickListener(this);
        owner.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.washer_owner:
                startActivity(new Intent(getApplicationContext(), WasherOwnerLoginActivity.class));
                break;
            case R.id.driver:
                startActivity(new Intent(getApplicationContext(), WashersMapActivity.class));
                break;
        }
    }
}
