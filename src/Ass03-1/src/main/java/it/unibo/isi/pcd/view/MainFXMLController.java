package it.unibo.isi.pcd.view;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import it.unibo.isi.pcd.events.StartEvent;
import it.unibo.isi.pcd.model.Particle;
import it.unibo.isi.pcd.msgs.AddParticleMsg;
import it.unibo.isi.pcd.msgs.DoStepMsg;
import it.unibo.isi.pcd.msgs.PauseResumeMsg;
import it.unibo.isi.pcd.msgs.PauseResumeMsg.Type;
import it.unibo.isi.pcd.msgs.RemoveParticleMsg;
import it.unibo.isi.pcd.msgs.ShutdownMsg;
import it.unibo.isi.pcd.msgs.StopMsg;
import it.unibo.isi.pcd.utils.SystemUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class MainFXMLController implements Initializable {

  private static final String TIME_LABEL_TEXT = "Current Time: ";
  private static final String STEPS_LABEL_TEXT = "Steps left: ";
  private static final String BIANCO = "FFFFFF";
  private static final String ROSSO = "#F44242";
  private static final int MAX_LENGTH_STRING = 5;
  private ActorRef viewActor;
  private final List<Control> control;

  private Stage mainStage;
  /** Nodes **/

  @FXML
  private AnchorPane anPane;

  @FXML
  private Pane pane2D;

  @FXML
  private Slider thdSlider;

  @FXML
  private TextField stepNumber;

  @FXML
  private TextField particlesNumber;

  @FXML
  private Button addBtn;

  @FXML
  private Button removeBtn;

  @FXML
  private Button startStopBtn;

  @FXML
  private Button stepButton;

  @FXML
  private Button pauseButton;

  @FXML
  private Label stepLabel;

  @FXML
  private Label timeLabel;

  /** Actions **/

  @FXML
  private void startStopAction() {
    if (this.startStopBtn.getText().equals("Start")) {
      this.viewActor.tell(
          new StartEvent(this.getThdNumber(), StartEvent.EsType.ES1, this.getSteps(),
              this.getNumParticles(), (int) this.pane2D.getWidth(), (int) this.pane2D.getHeight()),
          this.viewActor);
      this.disableAll();
      this.removeBtn.setDisable(false);
      this.addBtn.setDisable(false);
      this.pauseButton.setDisable(false);
    } else {
      this.viewActor.tell(new StopMsg(), this.viewActor);
      this.disableAll();
      this.startStopBtn.setDisable(true);
      this.stepButton.setDisable(true);
      this.pauseButton.setDisable(true);
      this.addBtn.setDisable(true);
      this.removeBtn.setDisable(true);

    }

  }

  @FXML
  private void removeAction() {
    this.viewActor.tell(new RemoveParticleMsg(), this.viewActor);
  }

  @FXML
  private void addAction() {
    this.viewActor.tell(
        new AddParticleMsg((int) this.pane2D.getWidth(), (int) this.pane2D.getHeight()),
        this.viewActor);
  }

  @FXML
  private void pauseAction() {

    if (this.pauseButton.getText().equals("Pause")) {
      this.stepButton.setDisable(false);
      this.startStopBtn.setDisable(true);
      this.pauseButton.setText("Resume");
      this.viewActor.tell(new PauseResumeMsg(Type.PAUSE), this.viewActor);
    } else {
      this.stepButton.setDisable(true);
      this.startStopBtn.setDisable(false);
      this.pauseButton.setText("Pause");
      this.viewActor.tell(new PauseResumeMsg(Type.RESUME), this.viewActor);
    }
  }

  @FXML
  private void stepBystepAction() {
    this.viewActor.tell(new DoStepMsg(), this.viewActor);
  }

  /** Methods **/

  public int getThdNumber() {
    return (int) this.thdSlider.getValue();
  }

  public int getSteps() {
    return Integer.parseInt(this.stepNumber.getText());
  }

  public int getNumParticles() {
    return Integer.parseInt(this.particlesNumber.getText());
  }

  public void disableAll() {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.setControls(false);
      });
    } else {
      this.setControls(false);
    }
  }

  public void enableAll() {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.setControls(true);
      });
    } else {
      this.setControls(true);
    }
  }

  public void resetStartStopBtn() {

    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.startStopBtn.setDisable(false);
        this.startStopBtn.setText("Start");
        this.pauseButton.setDisable(true);
        this.addBtn.setDisable(true);
        this.removeBtn.setDisable(true);
      });
    } else {
      this.startStopBtn.setDisable(false);
      this.startStopBtn.setText("Start");
      this.pauseButton.setDisable(true);
      this.addBtn.setDisable(true);
      this.removeBtn.setDisable(true);
    }
  }

  public void toggleStartStopButton() {

    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.startStopBtn.setText(this.startStopBtn.getText().equals("Start") ? "Stop" : "Start");
      });
    } else {
      this.startStopBtn.setText(this.startStopBtn.getText().equals("Start") ? "Stop" : "Start");
    }
  }

  private void setControls(final boolean state) {
    this.control.forEach(control -> {
      control.setDisable(!state);
    });
  }

  public MainFXMLController() {
    this.control = new ArrayList<>();
    this.viewActor = null;
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    this.initSliders();
    this.initTextField(this.stepNumber);
    this.initTextField(this.particlesNumber);
    this.commonInit();

  }

  private void commonInit() {
    this.control.addAll(Arrays.asList(this.stepNumber, this.thdSlider, this.particlesNumber));
    this.pauseButton.setDisable(true);
    this.stepButton.setDisable(true);
    this.addBtn.setDisable(true);
    this.removeBtn.setDisable(true);
    this.stepLabel.setText(MainFXMLController.STEPS_LABEL_TEXT + "0");
    this.timeLabel.setText(MainFXMLController.TIME_LABEL_TEXT + "0");
    this.anPane.setOnScroll(event -> {
      double zoomFactor = 1.1;
      final double deltaY = event.getDeltaY();
      if (deltaY < 0) {
        zoomFactor = 2.0 - zoomFactor;
      }
      this.pane2D.setScaleX(this.pane2D.getScaleX() * zoomFactor);
      this.pane2D.setScaleY(this.pane2D.getScaleY() * zoomFactor);

      event.consume();
    });

  }

  private void insertError(final TextField field) {
    field.setBackground(new Background(
        new BackgroundFill(Color.web(MainFXMLController.ROSSO), CornerRadii.EMPTY, Insets.EMPTY)));
    field.setText("Inserici un numero valido");
  }

  private void resetError(final TextField field) {
    field.setBackground(new Background(
        new BackgroundFill(Color.web(MainFXMLController.BIANCO), CornerRadii.EMPTY, Insets.EMPTY)));
  }

  private void initTextField(final TextField txtF) {
    txtF.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
      if (!newValue.matches("\\d*")
          || (txtF.getText().length() > MainFXMLController.MAX_LENGTH_STRING)) {
        txtF.setText(oldValue);
      }
    });
    txtF.setText("100");
  }

  private void initSliders() {
    this.thdSlider.setMin(1);
    this.thdSlider.setMax(SystemUtils.getCores());
    this.thdSlider.setBlockIncrement(1);
    this.thdSlider.setMajorTickUnit(1);
    this.thdSlider.setMinorTickCount(0);
    this.thdSlider.setShowTickLabels(true);
    this.thdSlider.setSnapToTicks(true);
  }

  public void setStage(final Stage mainStage) {
    this.mainStage = mainStage;
    this.mainStage.setOnCloseRequest(content -> {
      this.viewActor.tell(new ShutdownMsg(), this.viewActor);
      try {
        Thread.sleep(1000);
      } catch (final InterruptedException e) {
      }
      Platform.runLater(() -> {
        Platform.exit();

      });

    });
  }

  public void setActorRef(final ActorRef self) {
    this.viewActor = self;
  }

  public void setNumStepsLeft(final int stepsLeft) {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.stepLabel.setText(MainFXMLController.STEPS_LABEL_TEXT + Integer.toString(stepsLeft));
      });
    } else {
      this.stepLabel.setText(MainFXMLController.STEPS_LABEL_TEXT + Integer.toString(stepsLeft));
    }
  }

  public void setCurrentLogicTime(final double d) {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.timeLabel.setText(MainFXMLController.TIME_LABEL_TEXT + Double.toString(d));
      });
    } else {
      this.timeLabel.setText(MainFXMLController.TIME_LABEL_TEXT + Double.toString(d));
    }
  }

  public void setupCircles(final List<Particle> circles) {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.internalSetupCircles(circles);
      });
    } else {
      this.internalSetupCircles(circles);
    }
  }

  private void internalSetupCircles(final List<Particle> particles) {

    this.pane2D.getChildren().clear();
    this.pane2D.getChildren().addAll(particles.stream().map(element -> {

      return new Circle(element.getPosition().getX(), element.getPosition().getY(), 10,
          element.getColor());
    }).collect(Collectors.toList()));
  }

  public void clear() {
    if (!Platform.isFxApplicationThread()) {
      Platform.runLater(() -> {
        this.pane2D.getChildren().clear();
      });
    } else {
      this.pane2D.getChildren().clear();
    }
  }
}
