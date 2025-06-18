package com.example.mymaps;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.brightcove.player.edge.VideoListener;
import com.brightcove.player.view.BrightcoveExoPlayerVideoView;
import com.brightcove.player.model.Video;
import com.brightcove.player.model.DeliveryType;
import com.brightcove.player.edge.Catalog;
import com.brightcove.player.event.EventEmitter;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    private BrightcoveExoPlayerVideoView brightcoveVideoView;
    private static final String VIDEO_ID = "6369961038112";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        brightcoveVideoView = findViewById(R.id.brightcove_video_view);

        // Account and policy keys
        String BRIGHTCOVE_ACCOUNT_ID = "4744899836001";
        String BRIGHTCOVE_POLICY_KEY = "BCpkADawqM19VpQr3q33W4MMj0xAZ3rykWSCMgHaICS-V5P4wcC7N1f4oAjkrjS4zhaj-zZ6Fb_gJFUx7s_AXZZutX_bF3yKiohmjFoO0L7-pZBcch9YQjfN-01DqXRc-4yeIEeBf9AMUaDP";

        EventEmitter eventEmitter = brightcoveVideoView.getEventEmitter();
        Catalog catalog = new Catalog.Builder(eventEmitter, BRIGHTCOVE_ACCOUNT_ID)
                .setPolicy(BRIGHTCOVE_POLICY_KEY)
                .build();

        // Try loading video from Brightcove catalog
        catalog.findVideoByID(VIDEO_ID, new VideoListener() {
            @Override
            public void onVideo(Video video) {
                brightcoveVideoView.add(video);
                brightcoveVideoView.start();
            }

            @Override
            public void onError(String error) {
                Log.e("BrightcoveError", "Error loading video from catalog: " + error);
                loadFallbackVideo();
            }
        });
    }

    private void loadFallbackVideo() {
        // Create a fallback video using a direct HLS URL
        Video fallbackVideo = Video.createVideo(
                "https://sdks.support.brightcove.com/assets/videos/hls/greatblueheron/greatblueheron.m3u8",
                DeliveryType.HLS
        );

        try {
            URI posterImage = new URI("https://sdks.support.brightcove.com/assets/images/general/Great-Blue-Heron.png");
            fallbackVideo.getProperties().put(Video.Fields.STILL_IMAGE_URI, posterImage);
        } catch (URISyntaxException e) {
            Log.e("PosterImageError", "Invalid URI for poster image", e);
        }

        brightcoveVideoView.add(fallbackVideo);
        brightcoveVideoView.start();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE);
    }

}
