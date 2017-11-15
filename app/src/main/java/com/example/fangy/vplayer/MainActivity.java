package com.example.fangy.vplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Button btnWatchVideo;
    private Uri mVideoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnWatchVideo = (Button) findViewById(R.id.bt_watchVideo);
        btnWatchVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoUri = Uri.parse("http://monterosa.d2.comp.nus.edu.sg/~team03/playlists/VID20171113113420.mpd");
                startActivity(new Intent(MainActivity.this, VideoViewActivity.class).setData(mVideoUri));
            }
        });
    }
}
