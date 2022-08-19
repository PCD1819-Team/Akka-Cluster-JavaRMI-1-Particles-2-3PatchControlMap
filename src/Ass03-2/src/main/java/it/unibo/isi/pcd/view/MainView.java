package it.unibo.isi.pcd.view;

import java.awt.Toolkit;
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MainView extends Application {

	private MainFXMLController control;
	private AnchorPane root;
	private static MainView singleton;
	private FXMLLoader loader;

	private MainView() {
	}

	public static MainView getInstance() {
		synchronized (MainView.class) {
			if (MainView.singleton == null) {

				MainView.singleton = new MainView();
			}
		}
		return MainView.singleton;
	}

	public MainFXMLController setupViewController() {
		this.loader = new FXMLLoader(this.getClass().getResource("../file/MainView.fxml"));
		try {
			this.root = (AnchorPane) this.loader.load();
			this.control = (MainFXMLController) this.loader.getController();
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		}
		return this.control;
	}

	@Override
	public void start(final Stage mainStage) throws Exception {
		final Scene scene = new Scene(this.root, Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2,
				Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2);
		scene.getStylesheets().add(this.getClass().getResource("../file/application.css").toExternalForm());
		this.control.setStage(mainStage);

		mainStage.setScene(scene);
		mainStage.show();

	}

}
