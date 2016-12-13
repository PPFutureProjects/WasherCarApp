package androks.washerapp.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import androks.washerapp.Fragments.CarsFragment;
import androks.washerapp.Fragments.WashersFragment;
import androks.washerapp.R;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private BroadcastReceiver mInternetReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        setMainFragment();
    }

    private void setMainFragment(){
        Fragment fragment = null;
        Class fragmentClass = WashersFragment.class;
        try {
            fragment  = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction().replace(R.id.content_main, fragment).commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Fragment fragment = null;
        Class fragmentClass = null;
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_map) {
            fragmentClass = WashersFragment.class;
        } else if(id == R.id.nav_cars)
            fragmentClass = CarsFragment.class;

        try {
            if (fragmentClass != null) fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_main, fragment).commit();

        item.setChecked(true);
        setTitle(item.getTitle());


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Receiver for stable internet connection
        mInternetReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                checkInternetConnection();
            }
        };registerReceiver(mInternetReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    private void checkInternetConnection() {
        boolean isProcess;
        try {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            isProcess = networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            isProcess = false;
            e.printStackTrace();
        }

        if (!isProcess) {
            try {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(MainActivity.this, R.style.AppCompatAlertDialogStyle);
                builder.setTitle("Internet not avaliable");
                builder.setMessage("You are offline. Please, check your internet connection");
                builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkInternetConnection();
                    }
                });
                builder.setCancelable(false);
                builder.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Crouton.makeText(MainActivity.this, "Internet connection avaliable", Style.CONFIRM).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mInternetReceiver);
    }
}
