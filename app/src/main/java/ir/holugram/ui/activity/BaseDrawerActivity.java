package ir.holugram.ui.activity;

import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import butterknife.BindDimen;
import butterknife.BindString;
import butterknife.BindView;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramUploadVideoRequest;
import dev.niekirk.com.instagram4android.requests.payload.StatusResult;
import ir.holugram.HolugramApplication;
import ir.holugram.R;


/**
 * Created by Miroslaw Stanek on 15.07.15.
 */
public class BaseDrawerActivity extends BaseActivity {

    public Instagram4Android instagram;
    @BindView(R.id.drawerLayout)
    DrawerLayout drawerLayout;
    @BindView(R.id.vNavigation)
    NavigationView vNavigation;
    @BindDimen(R.dimen.global_menu_avatar_size)
    int avatarSize;
    @BindString(R.string.user_profile_photo)
    String profilePhoto;
    //Cannot be bound via Butterknife, hosting view is initialized later (see setupHeader() method)
    private ImageView ivMenuUserProfilePhoto;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentViewWithoutInject(R.layout.activity_drawer);
        this.instagram = ((HolugramApplication) this.getApplication()).getInstagram();


        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.flContentRoot);
        LayoutInflater.from(this).inflate(layoutResID, viewGroup, true);
        bindViews();
        setupHeader();
        setupNavItemListener();
//        uploadTest();
/*
        vNavigation.findViewById(R.id.menu_feed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(BaseDrawerActivity.this, "TEST", Toast.LENGTH_SHORT).show();
            }
        });*/

    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getToolbar() != null) {
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawerLayout.openDrawer(Gravity.LEFT);
                }
            });
        }
    }

    private void setupHeader() {
        View headerView = vNavigation.getHeaderView(0);
        ivMenuUserProfilePhoto = (ImageView) headerView.findViewById(R.id.ivMenuUserProfilePhoto);
        headerView.findViewById(R.id.vGlobalMenuHeader).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGlobalMenuHeaderClick(v);
            }
        });


        Picasso.with(this)
                .load(profilePhoto)
                .placeholder(R.drawable.img_circle_placeholder)
                .resize(avatarSize, avatarSize)
                .centerCrop()
                .transform(new ir.holugram.ui.utils.CircleTransformation())
                .into(ivMenuUserProfilePhoto);
    }

    public void onGlobalMenuHeaderClick(final View v) {
        drawerLayout.closeDrawer(Gravity.LEFT);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int[] startingLocation = new int[2];
                v.getLocationOnScreen(startingLocation);
                startingLocation[0] += v.getWidth() / 2;
                ir.holugram.ui.activity.UserProfileActivity.startUserProfileFromLocation(startingLocation, BaseDrawerActivity.this, 0);
                overridePendingTransition(0, 0);
            }
        }, 200);
    }


    public void setupNavItemListener() {
        vNavigation.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.menu_feed:
                        Toast.makeText(BaseDrawerActivity.this, "TEST", Toast.LENGTH_SHORT).show();


                        break;
                    case R.id.show_explore:
                        drawerLayout.closeDrawers();
                        Intent intent = new Intent(getApplicationContext(), ExploreActivity.class);
                        startActivity(intent);
                        break;
                }
                return false;
            }
        });
    }

    public void uploadTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                String path = "/storage/emulated/0/Movies/lion-sample.mp4";
                File sdcard = Environment.getExternalStorageDirectory();
                //Get the text file
                File file = new File(sdcard, "/Movies/big_buck_bunny.mp4");

                StringBuilder text = new StringBuilder();

                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;

                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                } catch (IOException e) {
                    //You'll need to add proper error handling here

                }

                File file2 = new File(sdcard, "/Movies/222");
                if (file.exists()) {
                    try {
                        Log.e("TRYUPLAOD", "start upload");

                        StatusResult r = instagram.sendRequest(new InstagramUploadVideoRequest(file, "Video posted with Instagram4j, how cool is that?", file2));
                        Log.i("UPLOAD", r.getMessage());
                    } catch (IOException e) {
                        Log.e("UPLOAD", Log.getStackTraceString(e));
                    }
                }

            }
        }).start();
    }
}