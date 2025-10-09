/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * */
package application;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import models.Ip;
import models.Language;
import models.LoggerInitializer;
import models.Settings;
import org.apache.commons.lang.exception.ExceptionUtils;
import ui.GeneralController;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class App extends Application {

	private Settings settings;
	private Language language;
	public static Stage stage = null;

	public static String MAIN_STAGE_FXML_FILE = "/fxml/Main.fxml";
	public static String INFO_STAGE_FXML_FILE = "/fxml/InfoWindow.fxml";
	public static String ICON_URI = "/images/icon.png";
	protected static Logger LOGGER = Logger.getLogger(App.class.getName());

	@Override
	public void start(Stage primaryStage) {
		try {
			LoggerInitializer.init();
			// load language
			GeneralController.getLanguage().changeLanguageBundle(true);
			this.language = new Language();
			this.language.changeLanguageBundle(true);
			this.settings = new Settings();
			Ip ip = new Ip();
			//Config.loadConfiguration();
			// detect screens and choose the right one, first option can be load from settings, second as fallback use primary screen
			// to settings is saved hash of the screen and at the start of app all screens hash codes are compared if saved
			// hash is equal to any screen
			Screen screen = null;
			if (settings.getScreensHash().isEmpty()) {
				screen = Screen.getPrimary();
			} else {
				String hash = settings.getScreensHash().get(0);
				ObservableList<Screen> screens = Screen.getScreens();
				List<Screen> screens1 = screens.stream().filter(screen1 -> Integer.toString(screen1.hashCode()).equals(hash)).collect(Collectors.toList());
				screen = screens1.size() == 0 ? Screen.getPrimary():screens1.get(0);
			}
			settings.saveScreenHash(Integer.toString(screen.hashCode()));
			FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_STAGE_FXML_FILE),GeneralController.getLanguage().getLanguageBundle());
			Stage newStage = new Stage();
			newStage.getIcons().add(new Image(ICON_URI));
			newStage.setScene(new Scene((Parent) loader.load()));
			newStage.setTitle(GeneralController.APP_TITTLE + " " + GeneralController.APP_TITTLE);
			// calculate coordinates to start app in the middle of screen
			double minX = screen.getVisualBounds().getMinX();
			double minY = screen.getVisualBounds().getMinY();
			double maxX = screen.getVisualBounds().getMaxX();
			double maxY = screen.getVisualBounds().getMaxY();
			double offsetX = Math.abs(maxX-minX)/2-300;
			double offsetY = Math.abs(maxY-minY)/2-200;
			double x = minX+offsetX;
			double y = minY+offsetY;
			newStage.setX(x);
			newStage.setY(y);
			// pass objects
			GeneralController controller = (GeneralController) loader.getController();
			controller.setSettings(settings);
			controller.setLabels();
			controller.setIpDns(ip);
			// show scene
			App.stage = newStage;
			newStage.show();


		} catch (Exception e) {
			//e.printStackTrace();
			LOGGER.severe(ExceptionUtils.getStackTrace(e));
		}
	}

	@Override
	public void stop() {
		// on stop of app, save screen hash so app can be opened on the same screen again
		for (Screen screen : Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1., 1.)) {
			settings.saveScreenHash(Integer.toString(screen.hashCode()));
		}
		settings.appIsClossing();
		System.exit(0);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
