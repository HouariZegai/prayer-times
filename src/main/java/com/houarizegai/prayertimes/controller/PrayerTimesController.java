package com.houarizegai.prayertimes.controller;

import com.houarizegai.prayertimes.App;
import com.houarizegai.prayertimes.data.DataRepository;
import com.houarizegai.prayertimes.data.model.CalculationMethod;
import com.houarizegai.prayertimes.data.model.City;
import com.houarizegai.prayertimes.data.model.Country;
import com.houarizegai.prayertimes.data.model.PrayerTimes;
import com.houarizegai.prayertimes.i18n.I18n;
import com.houarizegai.prayertimes.service.AdhanService;
import com.houarizegai.prayertimes.util.FileUtils;
import com.houarizegai.prayertimes.util.SettingsStore;
import com.houarizegai.prayertimes.data.network.WebService;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXToggleButton;
import com.jfoenix.transitions.hamburger.HamburgerBasicCloseTransition;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrayerTimesController implements Initializable {

  private static final int DEFAULT_METHOD_ID = 4; // Umm Al-Qura, Makkah
  private static final String DEFAULT_COUNTRY_CODE = "SA";
  private static final String DEFAULT_CITY = "Mecca";

  @FXML
  private StackPane menuBar;
  @FXML
  private JFXHamburger hamburgerMenu;
  @FXML
  private JFXComboBox<String> comboCountry;
  @FXML
  private JFXComboBox<String> comboCities;
  @FXML
  private Label lblDateTitle, lblClockTitle, lblAppTitle;
  @FXML
  private Label lblDate;
  @FXML
  private Label lblLoadError;
  @FXML
  private Label lblTimeH, lblTimeSeparator, lblTimeM, lblTimeSeparator2, lblTimeS;
  @FXML
  private Label lblPrayerFajr, lblPrayerSunrise, lblPrayerDhuhr, lblPrayerAsr, lblPrayerMaghrib, lblPrayerIsha;
  @FXML
  private Label lblNameFajr, lblNameSunrise, lblNameDhuhr, lblNameAsr, lblNameMaghrib, lblNameIsha;

  /* Adhan part */
  @FXML
  private StackPane alarmView;
  @FXML
  private Label lblAlarmTitle;
  @FXML
  private Text txtAlarmPrefix, txtAlarmPrayer, txtAlarmMiddle, txtAlarmCity, txtAlarmSuffix;

  /* Settings part */
  @FXML
  private VBox settingsView;
  @FXML
  private Label lblSettingsTitle, lblAdhanSection, lblEnableAdhan, lblAdhanSound, lblMethodLabel, lblLanguageLabel;
  @FXML
  private JFXToggleButton tglRunAdhan;
  @FXML
  private JFXComboBox<String> comboAdhan;
  @FXML
  private JFXComboBox<String> comboMethod;
  @FXML
  private JFXComboBox<String> comboLanguage;
  @FXML
  private FontIcon iconPlayAdhan;

  private HamburgerBasicCloseTransition hamburgerTransition;

  // Used to make stage draggable
  private double xOffset = 0;
  private double yOffset = 0;

  private final WebService webService;
  private final AdhanService adhanService;
  private final SettingsStore settings = new SettingsStore();

  // Background fetch of prayer times (the UI thread must never block on the network)
  private final ExecutorService prayerTimesExecutor = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "prayer-times-fetch");
    thread.setDaemon(true);
    return thread;
  });
  // Incremented per fetch; a response is applied only if it is still the latest
  private int prayerTimesRequest;

  private List<Country> countries;
  private List<CalculationMethod> methods;

  // Suppresses event handlers while settings are being applied programmatically
  private boolean loading = true;
  private String dateFormatPattern = "dd.MM.yyyy";

  public PrayerTimesController() {
    this.webService = new WebService();
    this.adhanService = new AdhanService();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    settings.load();
    countries = DataRepository.loadCountries();
    methods = DataRepository.loadMethods();
    I18n.init(settings.get("lang", I18n.LANG_EN));

    initMenu();
    initDateAndClock();
    loadSettingsLog(); // load saved app state
    initAdhan();

    /* Make stage draggable */
    menuBar.setOnMousePressed(event -> {
      xOffset = event.getSceneX();
      yOffset = event.getSceneY();
    });
    menuBar.setOnMouseDragged(event -> {
      App.stage.setX(event.getScreenX() - xOffset);
      App.stage.setY(event.getScreenY() - yOffset);
      App.stage.setOpacity(0.7f);
    });
    menuBar.setOnDragDone(e -> App.stage.setOpacity(1.0f));
    menuBar.setOnMouseReleased(e -> App.stage.setOpacity(1.0f));
  }

  private void initDateAndClock() {
    /* Init clock (date & time) of prayer times */
    KeyFrame clockKeyFrame = new KeyFrame(Duration.ZERO, e -> {
      Date date = new Date();
      DateFormat dateFormat = new SimpleDateFormat(dateFormatPattern, Locale.ENGLISH);
      lblDate.setText(dateFormat.format(date));

      String time = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(date);
      String[] timeParts = time.split(":");
      lblTimeS.setText(timeParts[2]);
      lblTimeM.setText(timeParts[1]);
      lblTimeH.setText(timeParts[0]);

      // Is it new day? => change the prayer times
      if (time.equals("00:00:00")) {
        setPrayerTimes();
      }

      checkAdhanTime();
    });

    Timeline clock = new Timeline(clockKeyFrame, new KeyFrame(Duration.seconds(1)));
    clock.setCycleCount(Animation.INDEFINITE);
    clock.play();

    // Show/Hide animation for time separator
    KeyFrame clockSeparatorKeyFrame = new KeyFrame(Duration.ZERO, e -> {
      if (lblTimeSeparator.isVisible()) {
        lblTimeSeparator.setVisible(false);
        lblTimeSeparator2.setVisible(false);
      } else {
        lblTimeSeparator.setVisible(true);
        lblTimeSeparator2.setVisible(true);
      }
    });

    Timeline clockSeparator = new Timeline(clockSeparatorKeyFrame, new KeyFrame(Duration.millis(500)));
    clockSeparator.setCycleCount(Animation.INDEFINITE);
    clockSeparator.play();

  }

  private void initAdhan() {
    String adhanName = comboAdhan.getSelectionModel().getSelectedItem();
    if (adhanName != null) {
      adhanService.setAdhan(adhanName);
    }
  }

  private void setPrayerTimes() {
    City city = selectedCity();
    if (city == null) {
      return;
    }
    double lat = city.getLat();
    double lng = city.getLng();
    int method = selectedMethodId();
    final int request = ++prayerTimesRequest;

    prayerTimesExecutor.execute(() -> {
      PrayerTimes prayerTimes = webService.getPrayerTimes(lat, lng, method); // null on failure
      Platform.runLater(() -> {
        if (request != prayerTimesRequest) {
          return; // superseded by a newer selection: drop this response
        }
        applyPrayerTimes(prayerTimes);
      });
    });
  }

  /** Shows the fetched times, or "--:--" plus the retry hint when the fetch failed. */
  private void applyPrayerTimes(PrayerTimes times) {
    String unavailable = "--:--";
    lblPrayerFajr.setText(times != null ? times.getFajr() : unavailable);
    lblPrayerSunrise.setText(times != null ? times.getSunrise() : unavailable);
    lblPrayerDhuhr.setText(times != null ? times.getDhuhr() : unavailable);
    lblPrayerAsr.setText(times != null ? times.getAsr() : unavailable);
    lblPrayerMaghrib.setText(times != null ? times.getMaghrib() : unavailable);
    lblPrayerIsha.setText(times != null ? times.getIsha() : unavailable);
    lblLoadError.setVisible(times == null);
  }

  @FXML
  private void onRetryLoad() {
    lblLoadError.setVisible(false); // feedback while retrying; re-shown if it fails again
    setPrayerTimes();
  }

  @FXML
  private void onClose() {
    saveSettings();
    App.stage.hide();
  }

  @FXML
  private void onHide() {
    App.stage.setIconified(true);
  }

  /* Alarm (Adhan) part */

  private void checkAdhanTime() {
    String timeNow = lblTimeH.getText() + ":" + lblTimeM.getText();

    checkTimeWithPrayer(timeNow, lblPrayerFajr, I18n.t("prayer.fajr"));
    checkTimeWithPrayer(timeNow, lblPrayerDhuhr, I18n.t("prayer.dhuhr"));
    checkTimeWithPrayer(timeNow, lblPrayerAsr, I18n.t("prayer.asr"));
    checkTimeWithPrayer(timeNow, lblPrayerMaghrib, I18n.t("prayer.maghrib"));
    checkTimeWithPrayer(timeNow, lblPrayerIsha, I18n.t("prayer.isha"));
  }

  private void checkTimeWithPrayer(String time, Label lblPrayerTime, String prayerName) { // Check if it's the time of prayer
    if (time.equals(lblPrayerTime.getText()) && adhanService.isCanPlay() && !adhanService.isPlaying() && tglRunAdhan.isSelected()) {
      adhanService.setCanPlay(false);

      String cityName = comboCities.getSelectionModel().getSelectedItem();
      String[] segments = alarmSegments(I18n.t("alarm.pattern"), prayerName, cityName != null ? cityName : "");
      txtAlarmPrefix.setText(segments[0]);
      txtAlarmPrayer.setText(segments[1]);
      txtAlarmMiddle.setText(segments[2]);
      txtAlarmCity.setText(segments[3]);
      txtAlarmSuffix.setText(segments[4]);

      setShowView(true, alarmView);
      adhanService.play();
    }
  }

  /**
   * Splits the alarm pattern "prefix {0} middle {1} suffix" into
   * [prefix, prayer, middle, city, suffix] so each part can be styled.
   */
  private static String[] alarmSegments(String pattern, String prayer, String city) {
    String[] aroundPrayer = pattern.split("\\{0\\}", 2);
    String[] aroundCity = aroundPrayer.length > 1
      ? aroundPrayer[1].split("\\{1\\}", 2)
      : new String[]{""};
    return new String[]{
      aroundPrayer[0],
      prayer,
      aroundCity[0],
      city,
      aroundCity.length > 1 ? aroundCity[1] : ""
    };
  }

  @FXML
  public void onCloseAlarm() {
    setShowView(false, alarmView);
    adhanService.pause();
    adhanService.launchPeriodStop();
  }

  /* Settings part */

  private void initMenu() { // Init settings
    /* Init show/hide menu */
    hamburgerTransition = new HamburgerBasicCloseTransition(hamburgerMenu);
    hamburgerTransition.setRate(-1);
    hamburgerMenu.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
      if (hamburgerTransition.getRate() == -1) {
        hamburgerTransition.setRate(1);
        setShowView(true, settingsView);
      } else {
        hamburgerTransition.setRate(-1);
        setShowView(false, settingsView);
        stopPreview();
      }
      hamburgerTransition.play();
    });

    /* Init language combobox */
    comboLanguage.getItems().addAll("English", "العربية");
    comboLanguage.setOnAction(e -> {
      if (loading) {
        return;
      }
      int index = comboLanguage.getSelectionModel().getSelectedIndex();
      applyLanguage(index == 0 ? I18n.LANG_EN : I18n.LANG_AR);
      saveSettings();
    });

    /* Init calculation method combobox */
    comboMethod.setOnAction(e -> {
      if (loading) {
        return;
      }
      saveSettings();
      setPrayerTimes();
    });

    /* Init country combobox */
    comboCountry.setOnAction(e -> {
      if (loading) {
        return;
      }
      onCountryChanged();
    });

    /* Init city combobox */
    comboCities.setOnAction(e -> {
      if (loading) {
        return;
      }
      if (selectedCity() != null) {
        saveSettings();
        setPrayerTimes();
      }
    });

    /* Init Adan combobox */
    List<String> adhanFilesName = FileUtils.getFilesNameFromFolder(FileUtils.RESOURCES_PATH.resolve("adhan").toString());
    if (adhanFilesName != null && !adhanFilesName.isEmpty())
      comboAdhan.getItems().addAll(Optional.ofNullable(adhanFilesName).get());

    comboAdhan.setOnAction(e -> {
      stopPreview(); // silence the previous sound before swapping players
      initAdhan();
      if (!loading) {
        saveSettings();
      }
    });

    tglRunAdhan.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
      if (!loading) {
        saveSettings();
      }
    });

    // Init play/stop preview of the selected adhan sound
    iconPlayAdhan.setIconLiteral(FontAwesome.PLAY.getDescription());
    adhanService.setOnPlaybackFinished(() -> iconPlayAdhan.setIconLiteral(FontAwesome.PLAY.getDescription()));
    iconPlayAdhan.setOnMouseClicked(e -> {
      if (adhanService.isPlaying()) {
        stopPreview();
      } else {
        String adhanName = comboAdhan.getSelectionModel().getSelectedItem();
        if (adhanName == null) {
          return;
        }
        adhanService.setAdhan(adhanName); // fresh player => rewinds to the start
        adhanService.play();
        iconPlayAdhan.setIconLiteral(FontAwesome.PAUSE.getDescription());
      }
    });
  }

  /** Stops any running adhan (preview or alarm) and resets the preview icon. */
  private void stopPreview() {
    adhanService.pause();
    iconPlayAdhan.setIconLiteral(FontAwesome.PLAY.getDescription());
  }

  @FXML
  private void onCloseMenu() {
    setShowView(false, settingsView);

    hamburgerTransition.setRate(-1);
    hamburgerTransition.play();

    stopPreview();
  }

  private void setShowView(boolean show, Parent view) {
    ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(500), view);
    if (show) {
      view.setVisible(true);
      scaleTransition.setFromX(0);
      scaleTransition.setFromY(0);
      scaleTransition.setToX(1);
      scaleTransition.setToY(1);
    } else {
      scaleTransition.setFromX(1);
      scaleTransition.setFromY(1);
      scaleTransition.setToX(0);
      scaleTransition.setToY(0);
      scaleTransition.setOnFinished(e -> view.setVisible(false));
    }
    scaleTransition.play();
  }

  /* Localization */

  private void applyLanguage(String language) {
    boolean wasLoading = loading;
    loading = true;
    try {
      I18n.init(language);

      lblDateTitle.setText(I18n.t("date.label"));
      lblClockTitle.setText(I18n.t("clock.label"));
      lblAppTitle.setText(I18n.t("app.title"));
      lblNameFajr.setText(I18n.t("prayer.fajr"));
      lblNameSunrise.setText(I18n.t("prayer.sunrise"));
      lblNameDhuhr.setText(I18n.t("prayer.dhuhr"));
      lblNameAsr.setText(I18n.t("prayer.asr"));
      lblNameMaghrib.setText(I18n.t("prayer.maghrib"));
      lblNameIsha.setText(I18n.t("prayer.isha"));
      lblSettingsTitle.setText(I18n.t("settings.title"));
      lblAlarmTitle.setText(I18n.t("settings.section.adhan"));
      lblAdhanSection.setText(I18n.t("settings.section.adhan"));
      lblEnableAdhan.setText(I18n.t("settings.enableAdhan"));
      lblAdhanSound.setText(I18n.t("settings.adhanSound"));
      lblMethodLabel.setText(I18n.t("settings.method"));
      lblLanguageLabel.setText(I18n.t("settings.language"));
      lblLoadError.setText(I18n.t("times.error"));
      comboAdhan.setPromptText(I18n.t("settings.adhanChoose"));
      comboCities.setPromptText(I18n.t("city.prompt"));
      comboCountry.setPromptText(I18n.t("country.prompt"));
      dateFormatPattern = I18n.t("format.date");

      comboLanguage.getSelectionModel().select(I18n.LANG_EN.equals(I18n.lang()) ? 0 : 1);

      // Repopulate country, city & method labels in the new language, keeping selections
      Country currentCountry = selectedCountry();
      City currentCity = selectedCity();
      populateCountryCombo(currentCountry != null ? currentCountry.getCode() : null);
      if (currentCountry != null) {
        populateCities(currentCountry);
        selectCity(currentCity);
      }
      populateMethodCombo(currentMethodId());

      applyDirection(I18n.isRtl());
      App.refreshTrayLabels();
    } finally {
      loading = wasLoading;
    }
  }

  private void applyDirection(boolean rtl) {
    NodeOrientation orientation = rtl ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT;
    comboCountry.setNodeOrientation(orientation);
    comboCities.setNodeOrientation(orientation);
    comboAdhan.setNodeOrientation(orientation);
    comboMethod.setNodeOrientation(orientation);
    comboLanguage.setNodeOrientation(orientation);
    tglRunAdhan.setNodeOrientation(orientation);

    // Settings grid: labels lead on the reading-start side, controls stay in the
    // middle column, the preview icon trails the combo (col 0 / col 2 swap)
    int labelColumn = rtl ? 2 : 0;
    int iconColumn = rtl ? 0 : 2;
    for (Label settingsLabel : new Label[]{lblAdhanSection, lblEnableAdhan, lblAdhanSound, lblMethodLabel, lblLanguageLabel}) {
      GridPane.setColumnIndex(settingsLabel, labelColumn);
    }
    GridPane.setColumnIndex(iconPlayAdhan, iconColumn);

    // Prayer rows: name on the reading-start side, time on the other side
    Pos namePos = rtl ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT;
    Pos timePos = rtl ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT;
    StackPane.setAlignment(lblNameFajr, namePos);
    StackPane.setAlignment(lblNameSunrise, namePos);
    StackPane.setAlignment(lblNameDhuhr, namePos);
    StackPane.setAlignment(lblNameAsr, namePos);
    StackPane.setAlignment(lblNameMaghrib, namePos);
    StackPane.setAlignment(lblNameIsha, namePos);
    StackPane.setAlignment(lblPrayerFajr, timePos);
    StackPane.setAlignment(lblPrayerSunrise, timePos);
    StackPane.setAlignment(lblPrayerDhuhr, timePos);
    StackPane.setAlignment(lblPrayerAsr, timePos);
    StackPane.setAlignment(lblPrayerMaghrib, timePos);
    StackPane.setAlignment(lblPrayerIsha, timePos);

    // Font: Alameen (Arabic) by default; system font for Latin scripts
    Parent root = menuBar.getParent();
    if (root != null) {
      if (rtl) {
        root.getStyleClass().remove("lang-en");
      } else if (!root.getStyleClass().contains("lang-en")) {
        root.getStyleClass().add("lang-en");
      }
    }
  }

  private void populateCountryCombo(String selectCode) {
    comboCountry.getItems().clear();
    int selectedIndex = -1;
    for (int i = 0; i < countries.size(); i++) {
      Country country = countries.get(i);
      comboCountry.getItems().add(I18n.isRtl() ? country.getNameAr() : country.getNameEn());
      if (country.getCode().equals(selectCode)) {
        selectedIndex = i;
      }
    }
    if (selectedIndex >= 0) {
      comboCountry.getSelectionModel().select(selectedIndex);
    }
  }

  private void populateMethodCombo(Integer selectId) {
    comboMethod.getItems().clear();
    int selectedIndex = -1;
    for (int i = 0; i < methods.size(); i++) {
      CalculationMethod method = methods.get(i);
      comboMethod.getItems().add(I18n.isRtl() ? method.getNameAr() : method.getName());
      if (selectId != null && method.getId() == selectId) {
        selectedIndex = i;
      }
    }
    if (selectedIndex >= 0) {
      comboMethod.getSelectionModel().select(selectedIndex);
    }
  }

  private void populateCities(Country country) {
    comboCities.getItems().clear();
    for (City city : country.getCities()) {
      comboCities.getItems().add(cityLabel(city));
    }
  }

  /** Display name of a city in the current language (canonical name is always English). */
  private static String cityLabel(City city) {
    return I18n.isRtl() && city.getNameAr() != null && !city.getNameAr().isBlank()
      ? city.getNameAr()
      : city.getName();
  }

  /* Country / city / method selection */

  private void onCountryChanged() {
    Country country = selectedCountry();
    if (country == null) {
      return;
    }
    loading = true;
    try {
      populateCities(country);
      selectCity(resolveCity(country, null));
    } finally {
      loading = false;
    }
    saveSettings();
    setPrayerTimes();
  }

  private Country findCountry(String code) {
    for (Country country : countries) {
      if (country.getCode().equals(code)) {
        return country;
      }
    }
    return countries.isEmpty() ? null : countries.get(0);
  }

  private Country selectedCountry() {
    int index = comboCountry.getSelectionModel().getSelectedIndex();
    return (index >= 0 && index < countries.size()) ? countries.get(index) : null;
  }

  private City selectedCity() {
    Country country = selectedCountry();
    if (country == null) {
      return null;
    }
    // The combo is populated in list order, so the selected index maps directly
    int index = comboCities.getSelectionModel().getSelectedIndex();
    return (index >= 0 && index < country.getCities().size()) ? country.getCities().get(index) : null;
  }

  private Integer currentMethodId() {
    int index = comboMethod.getSelectionModel().getSelectedIndex();
    return (index >= 0 && index < methods.size()) ? methods.get(index).getId() : null;
  }

  private int selectedMethodId() {
    Integer id = currentMethodId();
    return id != null ? id : DEFAULT_METHOD_ID;
  }

  private void selectMethod(int methodId) {
    for (int i = 0; i < methods.size(); i++) {
      if (methods.get(i).getId() == methodId) {
        comboMethod.getSelectionModel().select(i);
        return;
      }
    }
    // Unknown id: fall back to the default (Makkah)
    for (int i = 0; i < methods.size(); i++) {
      if (methods.get(i).getId() == DEFAULT_METHOD_ID) {
        comboMethod.getSelectionModel().select(i);
        return;
      }
    }
    if (!methods.isEmpty()) {
      comboMethod.getSelectionModel().select(0);
    }
  }

  private void selectCity(City city) {
    if (city == null) {
      return;
    }
    Country country = selectedCountry();
    if (country == null) {
      return;
    }
    int index = country.getCities().indexOf(city);
    if (index >= 0) {
      comboCities.getSelectionModel().select(index);
    }
  }

  /**
   * Resolves the stored city value. Older versions stored an index into the
   * Algeria city list; that index is migrated to the city name here (the DZ list
   * order in locations.json must stay stable for this to work).
   */
  private City resolveCity(Country country, String stored) {
    if (stored != null && !stored.isBlank()) {
      if (stored.matches("\\d+")) { // legacy: saved city index
        int index = Integer.parseInt(stored);
        if (index >= 0 && index < country.getCities().size()) {
          return country.getCities().get(index);
        }
      } else {
        for (City city : country.getCities()) {
          if (city.getName().equals(stored)) {
            return city;
          }
        }
      }
    }
    // Default city for the default country (Makkah, Saudi Arabia)
    if (DEFAULT_COUNTRY_CODE.equals(country.getCode())) {
      for (City city : country.getCities()) {
        if (DEFAULT_CITY.equals(city.getName())) {
          return city;
        }
      }
    }
    return country.getCities().get(0);
  }

  /* Settings persistence */

  private void loadSettingsLog() {
    loading = true;
    try {
      applyLanguage(settings.get("lang", I18n.LANG_EN));

      String storedCity = settings.get("city", "");
      // Legacy settings stored an Algeria city index and had no country key
      String defaultCountry = storedCity.matches("\\d+") ? "DZ" : DEFAULT_COUNTRY_CODE;
      Country country = findCountry(settings.get("country", defaultCountry));
      int countryIndex = countries.indexOf(country);
      if (countryIndex >= 0) {
        comboCountry.getSelectionModel().select(countryIndex);
      }
      populateCities(country);
      selectCity(resolveCity(country, storedCity));

      selectMethod(settings.getInt("method", DEFAULT_METHOD_ID));

      tglRunAdhan.setSelected(Boolean.parseBoolean(settings.get("enableAdhan", "true")));
      int adhanIndex = settings.getInt("adhan", 0);
      if (adhanIndex >= comboAdhan.getItems().size()) {
        adhanIndex = 0;
      }
      if (adhanIndex >= 0 && !comboAdhan.getItems().isEmpty()) {
        comboAdhan.getSelectionModel().select(adhanIndex);
      }
    } finally {
      loading = false;
    }
    saveSettings(); // persists migrated/normalized values
    setPrayerTimes();
  }

  private void saveSettings() {
    settings.set("lang", I18n.lang());
    Country country = selectedCountry();
    if (country != null) {
      settings.set("country", country.getCode());
    }
    City city = selectedCity();
    if (city != null) {
      settings.set("city", city.getName());
    }
    settings.set("method", String.valueOf(selectedMethodId()));
    settings.set("enableAdhan", String.valueOf(tglRunAdhan.isSelected()));
    settings.set("adhan", String.valueOf(comboAdhan.getSelectionModel().getSelectedIndex()));
    settings.save();
  }

}
