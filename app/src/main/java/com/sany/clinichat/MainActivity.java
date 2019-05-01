package com.sany.clinichat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sany.clinichat.Fragments.ChatsFragment;
import com.sany.clinichat.Fragments.ProfileFragment;
import com.sany.clinichat.Fragments.UsersFragment;
import com.sany.clinichat.Model.Chat;
import com.sany.clinichat.Model.User;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private static final String APP_KEY = "e237f41d-f67d-439b-902f-9dc43a3c7814";
    private static final String APP_SECRET = "X6oVPhF8bUy2UoFdMKJw4w==";
    private static final String ENVIRONMENT = "sandbox.sinch.com";

    private Call call;
    private SinchClient sinchClient;

    CircleImageView profile_image;
    TextView username;

    FirebaseAuth auth;
    FirebaseUser firebaseUser;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .userId(firebaseUser.getUid())
                .applicationKey(APP_KEY)
                .applicationSecret(APP_SECRET)
                .environmentHost(ENVIRONMENT)
                .build();
        sinchClient.setSupportCalling(true);
        sinchClient.startListeningOnActiveConnection();
        sinchClient.start();
        sinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {

                    //change this
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final TabLayout tabLayout = findViewById(R.id.tab_layout);
        final ViewPager viewPager = findViewById(R.id.view_pager);


        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
                int unread = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(firebaseUser.getUid()) && !chat.isIsseen()){
                        unread++;
                    }
                }

                if (unread == 0){
                    viewPagerAdapter.addFragment(new ChatsFragment(), "Chats");
                } else {
                    viewPagerAdapter.addFragment(new ChatsFragment(), "("+unread+") Chats");
                }

                viewPagerAdapter.addFragment(new UsersFragment(), "Doctor List");
                viewPagerAdapter.addFragment(new ProfileFragment(), "Profile");

                viewPager.setAdapter(viewPagerAdapter);

                tabLayout.setupWithViewPager(viewPager);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){

            case  R.id.logout:
                FirebaseAuth.getInstance().signOut();
                // change this code beacuse your app will crash
                startActivity(new Intent(MainActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;

            case R.id.tips:
                startActivity(new Intent(MainActivity.this, HealthTips.class));
                return true;

            case R.id.about:
                TextView msg = new TextView(this);
                msg.setText(Html.fromHtml("<div id=\"readme\" class=\"readme blob instapaper_body js-code-block-container\">\n" + "<article class=\"markdown-body entry-content\">\n" +
                        "<p><strong>ACKNOWLEDGEMENT</strong></p>\n" + "<p style=\"text-align: justify;\"><span style=\"color: #ff9900;\">We would like to express our deepest appreciation to all those who provided us the possibility to complete this project. " +
                        "We express our humble gratitude to our Vc sir Brig Gen Prof M. Mustafa Kamal, Department Head Dr. Mirza A.F.M. Rashidul Hasan for his useful and constructive recommendations on this project and special thanks should be given to Md. Sumon Mia, our project supervisor for his professional guidance, continuous help and valuable support.</span></p>\n" +
                        "<p style=\"text-align: justify;\"><span style=\"color: #ff9900;\">We are also indebted to other faculty members of Computer Science &amp; Engineering department for their assistance and inspiration. Without their genuine help completing our undergraduate Project would not be feasible.</span></p>\n" +
                        "<p style=\"text-align: justify;\"><span style=\"color: #ff9900;\">Finally, we wish to thank our parents for their support and encouragement throughout our study.</span></p>\n" +
                        "<p style=\"text-align: justify;\">&nbsp;</p>\n" + "<h4 class=\"mt-0 mb-2\">MIT License</h4>\n" + "<p style=\"text-align: justify;\">&copy; 2019 <span title=\"0.35269s from unicorn-85988c657d-j4n94\">GitHub</span>, Inc. and contributors</p>\n" +
                        "<p style=\"text-align: justify;\">Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:</p>\n" +
                        "<p style=\"text-align: justify;\">The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.</p>\n" + "<p style=\"text-align: justify;\">&nbsp;</p>\n" +
                        "<p><strong>DECLARATION</strong></p>\n" + "<p style=\"text-align: justify;\"><span style=\"color: #800080;\">We thereby declare that our project entitled <strong>&ldquo;</strong>Mobile-Based&nbsp; Medical&nbsp; Health&nbsp; Application [E-Clinic]<strong>&rdquo; </strong>is the result of our own work and to the best of our knowledge and belief, it contains no material previously published or written by another person, nor material which to a substantial extent has been accepted for the awarded of any other degree at CSE, BAUET or any other educational intuition, except where due acknowledgement is made in the project.</span></p>\n" +
                        "</article>\n" + "</div>"));
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("About E-clinic");
                builder.setView(msg);
                builder.show();

        }

        return false;
    }


    class ViewPagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        ViewPagerAdapter(FragmentManager fm){
            super(fm);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        public void addFragment(Fragment fragment, String title){
            fragments.add(fragment);
            titles.add(title);
        }

        // Ctrl + O

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    private void status(String status){
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallProgressing(Call call) {
            Toast.makeText(getApplicationContext(),"Ringing...",Toast.LENGTH_LONG).show();

        }

        @Override
        public void onCallEstablished(Call call) {
            Toast.makeText(getApplicationContext(),"Call Established",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCallEnded(Call endedcall) {
            Toast.makeText(getApplicationContext(),"Call Ended",Toast.LENGTH_LONG).show();
            call = null;
            endedcall.hangup();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> list) {

        }
    }

    private class SinchCallClientListener implements CallClientListener
    {

        @Override
        public void onIncomingCall(CallClient callClient, final Call incomingcall) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Incoming Call");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Reject", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    call.hangup();
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Pick", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    call = incomingcall;
                    call.answer();
                    call.addCallListener(new SinchCallListener());
                    Toast.makeText(getApplicationContext(),"Call is started",Toast.LENGTH_LONG).show();
                }
            });
            alertDialog.show();
        }
    }

    public void callUser(User user)
    {
        if (call==null)
        {
            call = sinchClient.getCallClient().callUser(user.getId());
            call.addCallListener(new SinchCallListener());

            openCallerDialog(call);
        }
    }

    private void openCallerDialog(final Call call) {
        AlertDialog alertDialogcall = new AlertDialog.Builder(MainActivity.this).create();
        alertDialogcall.setTitle("Alert");
        alertDialogcall.setMessage("Outgoing Call");
        alertDialogcall.setButton(AlertDialog.BUTTON_NEUTRAL, "Hang Up", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                call.hangup();
            }
        });
        alertDialogcall.show();
    }
}
