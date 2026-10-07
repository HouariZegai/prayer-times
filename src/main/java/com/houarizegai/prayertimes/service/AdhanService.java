package com.houarizegai.prayertimes.service;

import com.houarizegai.prayertimes.util.FileUtils;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;

import java.io.File;

public class AdhanService {

  private MediaPlayer adhanPlayer;

  @Setter
  @Getter
  private boolean canPlay = true;

  // Invoked on the FX thread when playback reaches the end of the media
  // (used by the settings preview to reset its play/stop icon).
  @Setter
  private Runnable onPlaybackFinished;

  public void setAdhan(String adhanName) {
    if (adhanPlayer != null) {
      adhanPlayer.stop(); // silence any previous preview/alarm before swapping
    }
    Media media = new Media(new File(FileUtils.RESOURCES_PATH.resolve("adhan").resolve(adhanName).toString()).toURI().toString());
    adhanPlayer = new MediaPlayer(media);
    adhanPlayer.setOnEndOfMedia(() -> {
      if (onPlaybackFinished != null) {
        onPlaybackFinished.run();
      }
    });
  }

  public void play() {
    if (adhanPlayer != null) {
      if (adhanPlayer.getStatus().equals(MediaPlayer.Status.STOPPED)) {
        adhanPlayer.seek(adhanPlayer.getStartTime()); // replay from the beginning
      }
      adhanPlayer.play();
    }
  }

  public void pause() {
    if (adhanPlayer != null) {
      adhanPlayer.pause();
      adhanPlayer.seek(adhanPlayer.getStartTime());
    }
  }

  public boolean isPlaying() {
    return adhanPlayer != null && adhanPlayer.getStatus().equals(MediaPlayer.Status.PLAYING);
  }

  public void launchPeriodStop() { // If I stop Adhan don't play it again until the next prayer
    canPlay = false;
    new Thread(() -> {
      try {
        Thread.sleep(60000L); // Sleep 1 min
      } catch (InterruptedException ie) {
        ie.printStackTrace();
      }
      canPlay = true;
    }).start();
  }
}
