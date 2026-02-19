/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * Changed buttons for LLMNR and DoT, added button for load testing, added support for dark mode button and added file checking (rootservers, servers and load config)
 * */
package ui;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

import application.Config;
import application.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Language;
import models.Settings;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.parser.ParseException;

public class MainController extends GeneralController
{
    // FXML Components
    // language radio buttons

    // button to chose protocol
    @FXML
    private Button llmnrButton;
    @FXML
    private Button mdnsButton;
    @FXML
    private Button dohButton;
    @FXML
    private Button dotButton;
    @FXML
    private Button doqButton;
    @FXML
    @Translation
    protected Button loadButton;

    @FXML
    @Translation
    protected Button reportBugButton;
    // labels for protocol group
    @FXML
    @Translation
    protected Label basicDNSLabel;

    // help image
    @FXML
    protected Label dnsButtonHelp;

    @FXML
    @Translation
    protected Label multicastDNSLabel;

    @FXML
    @Translation
    protected Label encryptedDNSLabel;

    @FXML
    private ImageView llmrButtonHelp;
    @FXML
    private ImageView mdnsButtonHelp;
    @FXML
    private ImageView dohButtonHelp;
    @FXML
    private ImageView dotButtonHelp;
    @FXML
    private ImageView doqButtonHelp;

    private static final String BUG_URL = "https://github.com/xramos00/DNS_client/issues";
    private ToggleGroup languagegroup;

    private static Map<String, String> defaultPropertyValues = null;

    private static Map<String, String> defaultFilePaths = null;

    public static final String FXML_FILE_NAME = "/fxml/Main.fxml";

    public void initialize()
    {
        //
        LOGGER = Logger.getLogger(DNSController.class.getName());

        // setup toogle group
        languagegroup = new ToggleGroup();
        czechLangRadioButton.setToggleGroup(languagegroup);
        englishLangRadioButton.setToggleGroup(languagegroup);
        PROTOCOL = "DNS";
        czechLangRadioButton.setSelected(true);
    }



    @FXML
    private void definedButtonFired(ActionEvent event)
    {

        checkFiles();

        Button button = (Button) event.getSource();
        String fxml_file = (String) button.getUserData();
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml_file), GeneralController.language.getLanguageBundle());
            Stage newStage = new Stage();

            newStage.setScene(new Scene((Parent) loader.load()));
            GeneralController controller = (GeneralController) loader.getController();

            Stage oldStage = (Stage) dnsButton.getScene().getWindow();
            newStage.setX(oldStage.getX());
            newStage.setY(oldStage.getY());
            newStage.getIcons().add(new Image(App.ICON_URI));
            newStage.setTitle(controller.getTitle());
            controller.setSettings(settings);
            controller.setIpDns(ipDns);
            controller.setLabels();
            controller.loadDataFromSettings();
            controller.networkInterfaces();
            // added supporst for dark mode
            if(GeneralController.darkMode){
                controller.setDarkMode();
            } else {
                controller.clearDarkMode();
            }
            App.stage = newStage;
            newStage.show();
            oldStage.close();

        } catch (Exception e) {
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
            Alert alert = new Alert(AlertType.ERROR, GeneralController.language.getLanguageBundle().getString("windowError"));
            alert.showAndWait();
        }
    }

    private void checkFiles()
    {
        if (defaultPropertyValues == null){
            defaultPropertyValues = new HashMap<>();
            defaultPropertyValues.put(Config.ROOTSERVERSDOMAINNAMES,"rootservers.json");
            defaultPropertyValues.put(Config.SERVERSDOMAINNAMES,"servers.json");
            defaultPropertyValues.put(Config.GENERALCONFIG,"GeneralConfig.json");
            defaultPropertyValues.put(Config.LOADTESTCONFIG,"LoadTestConfig.json");
        }
        if (defaultFilePaths == null){
            defaultFilePaths = new HashMap<>();
            defaultFilePaths.put(Config.ROOTSERVERSDOMAINNAMES,"/backup/rootservers.json");
            defaultFilePaths.put(Config.SERVERSDOMAINNAMES,"/backup/servers.json");
            defaultFilePaths.put(Config.GENERALCONFIG,"/backup/GeneralConfig.json");
            defaultFilePaths.put(Config.LOADTESTCONFIG,"/backup/LoadTestConfig.json");
        }
        String[] files = {Config.ROOTSERVERSDOMAINNAMES, Config.SERVERSDOMAINNAMES, Config.GENERALCONFIG, Config.LOADTESTCONFIG};


        for (String file : files)
        {
            checkFile(file);
        }

        // try load config.properties
        try
        {
            Config.loadConfiguration();
        } catch (ConfigurationException e)
        {
            showAlert(e);
        } catch (IOException e)
        {
            showAlert("ConfigurationException");
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
        } catch (ParseException e)
        {
            showAlert("parseExceptionConfig");
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
        }
    }

    private void checkFile(String prop)
    {
        // load root DNS servers file path
        String rootDNSFilePath = null;
        try
        {
            rootDNSFilePath = Config.getConfProperties().getString(prop);
        } catch (ConfigurationException e)
        {
            Alert info = new Alert(AlertType.ERROR);
            info.setTitle(GeneralController.language.getLanguageBundle().getString("errorConfigProp") + " config.properties");
            info.setContentText(GeneralController.language.getLanguageBundle().getString("errorConfigProp") + " config.properties"+"\n"+language.getLanguageBundle().getString("addConfig"));
            info.initModality(Modality.APPLICATION_MODAL);
            info.initOwner((Stage) dnsButton.getScene().getWindow());
            info.showAndWait();
            System.exit(1);
        }

        // check if file exists
        File rootDNSFile = new File(rootDNSFilePath);
        if (!rootDNSFile.exists())
        {
            ButtonType findInFiles = new ButtonType(GeneralController.language.getLanguageBundle().getString("findInFiles"), ButtonBar.ButtonData.OK_DONE);
            ButtonType loadBackup = new ButtonType(GeneralController.language.getLanguageBundle().getString("loadBackup"), ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert info = new Alert(AlertType.ERROR);
            info.setTitle(GeneralController.language.getLanguageBundle().getString("errorConfigProp") + " " + prop);
            info.setContentText(GeneralController.language.getLanguageBundle().getString("errorConfigProp") + " " + defaultPropertyValues.get(prop)+"\n"+
                    GeneralController.language.getLanguageBundle().getString(prop));
            info.initModality(Modality.APPLICATION_MODAL);
            info.initOwner((Stage) dnsButton.getScene().getWindow());
            info.getButtonTypes().clear();
            info.getButtonTypes().addAll(findInFiles,loadBackup);
            Optional<ButtonType> result =  info.showAndWait();
            if (result.orElse(loadBackup) == findInFiles){
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Resource File");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("All Files", "*.*"));
                File selectedFile = fileChooser.showOpenDialog((Stage) dnsButton.getScene().getWindow());
                if (selectedFile != null)
                {
                    try
                    {
                        Config.getConfProperties().setProperty(prop, selectedFile.getAbsolutePath());
                        Config.getConfProperties().save();
                    } catch (ConfigurationException e)
                    {
                        //showAlert(e.getClass().getSimpleName());
                        //e.printStackTrace();
                        showAlert(e);
                    }
                } else {
                    loadBackup(prop);
                }
            } else {
                loadBackup(prop);
            }
        }
    }

    private void loadBackup(String prop){
        URL inputUrl = getClass().getResource(defaultFilePaths.get(prop));
        File dest = new File(defaultPropertyValues.get(prop));
        try {
            FileUtils.copyURLToFile(inputUrl, dest);
        } catch (IOException e) {
            showAlert("fileCopyError");
            LOGGER.severe(ExceptionUtils.getStackTrace(e));
        }
        try {
            Config.getConfProperties().setProperty(prop,"./"+defaultPropertyValues.get(prop));
            Config.getConfProperties().save();
        } catch (ConfigurationException e) {
            showAlert(e);
        }
    }

    @FXML
    private void reportBugButtonFired(ActionEvent event)
    {
        final Desktop desktop = Desktop.getDesktop();
        try
        {
            desktop.browse(new URI(BUG_URL));
        } catch (Exception e)
        {
            Alert alert = new Alert(AlertType.ERROR, GeneralController.language.getLanguageBundle().getString("bugButtonError"));
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initOwner((Stage) dnsButton.getScene().getWindow());
            alert.showAndWait();
        }
    }

    @Override
    protected void setUserDataRecords() {

    }

    @Override
    protected void setCustomUserDataRecords() {

    }

    @Override
    protected void updateCustomParameters()
    {

    }

    public void setSettings(Settings settings)
    {
        this.settings = settings;
    }

    @Override
    public String getProtocol()
    {
        return "DNS";
    }

    public void setLabels()
    {
        dnsButton.setUserData(DNSController.FXML_FILE_NAME_SMALL);
        mdnsButton.setUserData(MDNSController.FXML_FILE_NAME);
        loadButton.setUserData(TesterController.FXML_FILE_NAME);
        dotButton.setUserData(DoTController.FXML_FILE_NAME_SMALL);
        llmnrButton.setUserData(LLMNRController.FXML_FILE_NAME);
        dohButton.setUserData(DoHController.FXML_FILE_NAME);
        doqButton.setUserData(DoQController.FXML_FILE_NAME);
        Stage stage = (Stage) dnsButton.getScene().getWindow();
        stage.setTitle(GeneralController.language.getLanguageBundle().getString(APP_TITTLE) + " " + PROTOCOL);

        switch (GeneralController.language.getCurrentLanguage())
        {
            case Language.CZECH:
                czechLangRadioButton.setSelected(true);
                englishLangRadioButton.setSelected(false);
                break;
            case Language.ENGLISH:
                czechLangRadioButton.setSelected(false);
                englishLangRadioButton.setSelected(true);
                break;
            default:
                break;
        }
    }

    @Override
    protected void saveDomain(String domain) {

    }
}
