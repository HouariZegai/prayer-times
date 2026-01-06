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

  public void setAdhan(String adhanName) {
    Media media = new Media(new File(FileUtils.RESOURCES_PATH.resolve("adhan").resolve(adhanName).toString()).toURI().toString());
    adhanPlayer = new MediaPlayer(media);
  }

  public void play() {
    adhanPlayer.play();
  }

  public void pause() {
    adhanPlayer.pause();
    adhanPlayer.seek(adhanPlayer.getStartTime());
  }

  public boolean isPlaying() {
    return adhanPlayer.getStatus().equals(MediaPlayer.Status.PLAYING);
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
