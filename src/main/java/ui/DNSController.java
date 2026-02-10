/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Based on work of Martin Biolek (https://github.com/mbio16/clientDNS)
 * Changed hierarchy of class, changed FXML file and changed some mechanics
 * */
package ui;

import application.App;
import application.Config;
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import models.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import tasks.DNSOverTCPTask;
import tasks.DNSOverUDPTask;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Getter
@Setter
public class DNSController extends GeneralController {

    public static final String FXML_FILE_NAME_SMALL = "/fxml/DNS_small.fxml";
    public static final String FXML_FILE_NAME_LARGE = "/fxml/DNS_small.fxml";

    public static boolean layoutLarge = true;
    @FXML
    protected Label wiresharkLabel;
    @FXML
    protected RadioButton dnssecYesRadioButton;
    @FXML
    protected RadioButton dnssecNoRadioButton;
    // menu items
    @FXML
    protected RadioMenuItem justIp;
    @FXML
    protected RadioMenuItem ipAsFilter;
    @FXML
    protected RadioMenuItem ipwithTCPAsFilter;
    @FXML
    protected CheckBox nsec3CheckBox;
    @FXML
    protected GridPane dnsServerGridPane;
    @FXML
    protected VBox rootServersVBox;
    @FXML
    protected VBox leftRootServersVBox;
    @FXML
    protected VBox rightRootServersVBox;

    @FXML
    protected TitledPane dnsServerTitledPane;

    protected ToggleGroup otherDNSToggleGroup;
    protected ToggleGroup dnssecToggleGroup;

    @FXML
    private VBox vboxRoot;
    // text fields
    @FXML
    private TextField dnsServerTextField;
    @FXML
    private TitledPane DNSSECTitledPane;
    // radio buttons
    @FXML
    private RadioButton tcpRadioButton;
    @FXML
    private RadioButton udpRadioButton;
    @FXML
    private RadioMenuItem ipWithUDPAsFilter;
    @FXML
    private RadioMenuItem ipWithUDPandTcpAsFilter;
    // titledpane
    @FXML
    @Translation
    protected TitledPane transportTitledPane;
    // toogleGroup
    // toogleGroup
    private ToggleGroup transportToggleGroup;
    private ToggleGroup iterativeToggleGroup;
    // choice box
    @FXML
    private ComboBox<String> savedDNSChoiceBox;


    private Map<KeyCode, RadioButton> keyMappings;

    private TCPConnection tcp;

    public DNSController() {

        super();
        LOGGER = Logger.getLogger(DNSController.class.getName());
        PROTOCOL = "DNS";

    }

    @Override
    protected void updateCustomParameters() {
        parameters.put(WiresharkFilter.Parameters.TCPPORT, "53");
        parameters.put(WiresharkFilter.Parameters.UDPPORT, "53");
    }

    public void initialize() {
        super.initialize();

        transportToggleGroup = new ToggleGroup();
        tcpRadioButton.setToggleGroup(transportToggleGroup);
        udpRadioButton.setToggleGroup(transportToggleGroup);

        iterativeToggleGroup = new ToggleGroup();
        iterativeQueryRadioButton.setToggleGroup(iterativeToggleGroup);
        recursiveQueryRadioButton.setToggleGroup(iterativeToggleGroup);

        dnssecToggleGroup = new ToggleGroup();

        IPprotToggleGroup = new ToggleGroup();

        IPv4RadioButton.setToggleGroup(IPprotToggleGroup);
        IPv6RadioButton.setToggleGroup(IPprotToggleGroup);

        keyMappings = new HashMap<>();

        dnsserverToggleGroup = new ToggleGroup();
        int i = 0;
        for (NameServer ns : Config.getRootNameServers()) {
            RadioButton rb = new RadioButton(ns.getKeyCode().toString().toUpperCase());
            if (i % 2 == 0) {
                leftRootServersVBox.getChildren().add(rb);
            } else {
                rightRootServersVBox.getChildren().add(rb);
            }
            rb.setUserData(ns);
            rb.setTooltip(new Tooltip(ns.getIpv4() + "\n" + ns.getIpv6()));
            rb.setToggleGroup(dnsserverToggleGroup);
            keyMappings.put(ns.getKeyCode(), rb);
            i++;
        }

        Ip ip = new Ip();

        Config.getNameServers().add(new NameServer("System DNS", "System DNS", ip.getIpv4DnsServers(),
                ip.getIpv6DnsServers()));
        Config.getNameServers().stream().filter(nameServer1 -> !nameServer1.isDohOnly() && !nameServer1.isDotOnly())
                .forEach(ns -> otherDNSVbox.getChildren().add(new NameServerVBox(ns, dnsserverToggleGroup, this)));
        NameServerVBox nsVBox = ((NameServerVBox)otherDNSVbox.getChildren().getLast());
        nsVBox.selectFirst();
        HBox customDNS = new HBox();
        RadioButton customToggle = new RadioButton();
        customToggle.setToggleGroup(dnsserverToggleGroup);
        TextField input = new TextField();
        //input.setPromptText(language.getLanguageBundle().getString("dnsServerDropDownLabel"));
        customToggle.setUserData(input);
        input.setOnMouseClicked(actionEvent -> customToggle.setSelected(true));
        customDNS.getChildren().addAll(customToggle, input);
        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 5, 0));
        otherDNSVbox.getChildren().add(separator);
        otherDNSVbox.getChildren().add(customDNS);

        // hide root tree item in tree view, so it is expanded after receiving response
        requestTreeView.setShowRoot(false);
        responseTreeView.setShowRoot(false);

        // disable checkbox for holding TCP connection, because default selected transport protocol is UDP
        holdConnectionCheckbox.setDisable(true);
        tcpRadioButton.selectedProperty().addListener((observable, oldValue, newValue) ->
                holdConnectionCheckbox.setDisable(!newValue));
        setLanguageRadioButton();
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    protected void setCustomUserDataRecords() {
        nsCheckBox.setUserData(Q_COUNT.NS);
        checkBoxArray.add(nsCheckBox);
        mxCheckBox.setUserData(Q_COUNT.MX);
        checkBoxArray.add(mxCheckBox);
        soaCheckBox.setUserData(Q_COUNT.SOA);
        checkBoxArray.add(soaCheckBox);
        cnameCheckBox.setUserData(Q_COUNT.CNAME);
        checkBoxArray.add(cnameCheckBox);
        dnskeyCheckBox.setUserData(Q_COUNT.DNSKEY);
        checkBoxArray.add(dnskeyCheckBox);
        dsCheckBox.setUserData(Q_COUNT.DS);
        checkBoxArray.add(dsCheckBox);
        caaCheckBox.setUserData(Q_COUNT.CAA);
        checkBoxArray.add(caaCheckBox);
        rrsigCheckBox.setUserData(Q_COUNT.RRSIG);
        checkBoxArray.add(rrsigCheckBox);
        nsec3CheckBox.setUserData(Q_COUNT.NSEC3);
        checkBoxArray.add(nsec3CheckBox);
        nsec3paramCheckBox.setUserData(Q_COUNT.NSEC3PARAM);
        checkBoxArray.add(nsec3paramCheckBox);
        cdsCheckBox.setUserData(Q_COUNT.CDS);
        checkBoxArray.add(cdsCheckBox);
        cdnskeyCheckBox.setUserData(Q_COUNT.CDNSKEY);
        checkBoxArray.add(cdnskeyCheckBox);
        // 240979
        svcbCheckBox.setUserData(Q_COUNT.SVCB);
        checkBoxArray.add(svcbCheckBox);
        httpsCheckBox.setUserData(Q_COUNT.HTTPS);
        checkBoxArray.add(httpsCheckBox);
    }

    @Override
    public String getProtocol() {
        return "DNS";
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    public void setLabels() {
        setKeyboardShortcuts();
        setUserDataTransportProtocol();

    }

    private void setKeyboardShortcuts() {
        vboxRoot.getScene().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            // System.out.println(ke.getCode());
            LOGGER.fine(ke.getCode().getChar());
            if (!ke.isShiftDown()) {
                return;
            }
            if (keyMappings.get(ke.getCode()) != null) {
                keyMappings.get(ke.getCode()).setSelected(true);
            }
            ke.consume();
        });
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    private void setUserDataTransportProtocol() {
        tcpRadioButton.setUserData(TRANSPORT_PROTOCOL.TCP);
        udpRadioButton.setUserData(TRANSPORT_PROTOCOL.UDP);
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    public void loadDataFromSettings() {
		savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesDNS());
		//savedDNSChoiceBox.getItems().addAll(settings.getDnsServers())
    }

    @FXML
    protected void backButtonFired(ActionEvent event) {
        otherDNSVbox.getChildren().clear();
        super.backButtonFired(event);
    }

    @Override
    protected void saveDomain(String domain) {
        settings.addDNSDomain(domain);
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private TRANSPORT_PROTOCOL getTransportProtocol() {
        if (udpRadioButton.isSelected()) {
            return TRANSPORT_PROTOCOL.UDP;
        } else {
            return TRANSPORT_PROTOCOL.TCP;
        }
    }

    private boolean isRecursiveSet() {
        return recursiveQueryRadioButton.isSelected();
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    private void logMessage(String dnsServer, String domain, Q_COUNT[] records, boolean recursive, boolean dnssec,
                            TRANSPORT_PROTOCOL transport, boolean dnssecRRsig, boolean holdConnection) {
        LOGGER.info("DNS server: " + dnsServer + "\n" + "Domain: " + domain + "\n" + "Records: " + Arrays.toString(records)
                + "\n" + "Recursive:" + recursive + "\n" + "DNSSEC: " + dnssec + "\n" + "DNSSEC sig records"
                + dnssecRRsig + "\n" + "Transport protocol: " + transport + "\n" + "Hold connection: " + holdConnection
                + "\n" + "Application protocol: " + APPLICATION_PROTOCOL.DNS);
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    private void logMessage(String dnsServer, String domain, Q_COUNT[] records, boolean recursive, boolean dnssec,
                            TRANSPORT_PROTOCOL transport, boolean dnssecRRsig, boolean holdConnection,
                            boolean checkingdisabled) {
        LOGGER.info("DNS server: " + dnsServer + "\n" + "Domain: " + domain + "\n" + "Records: " + Arrays.toString(records)
                + "\n" + "Recursive:" + recursive + "\n" + "DNSSEC: " + dnssec + "\n" + "DNSSEC sig records"
                + dnssecRRsig + "\n" + "Transport protocol: " + transport + "\n" + "Hold connection: " + holdConnection
                + "\n" + "Application protocol: " + APPLICATION_PROTOCOL.DNS + "\n" + "Checking disabled " + checkingdisabled);
    }


    @FXML
    protected void sendButtonFired(ActionEvent event) {
        super.sendButtonFired(event);
        if (isTerminatingThread()){
            return;
        }
        try {
            String dnsServer = getDnsServerIp();
            if (dnsServer == null) {
                Platform.runLater(()->{
                    sendButton.setText(getButtonText());
                    progressBar.setProgress(0);
                });
                return;
            }
            //addServerToList();
            LOGGER.info(dnsServer);
            Q_COUNT[] records = getRecordTypes();
            TRANSPORT_PROTOCOL transport = getTransportProtocol();
            String domain = getDomain();
            boolean recursive = isRecursiveSet();
            boolean holdConnection = holdConnectionCheckbox.isSelected();
            boolean cdFlag = checkingDisabledCheckBox.isSelected();
            boolean adFlag = authenticateDataCheckBox.isSelected();
            boolean doFlag = DNSSECOkCheckBox.isSelected();
            logMessage(dnsServer, domain, records, recursive, adFlag, transport, doFlag, holdConnection, cdFlag);
            RequestSettings requestSettings = new RequestSettings.RequestSettingsBuilder()
                    .recursion(isRecursiveSet())
                    .cdFlag(checkingDisabledCheckBox.isSelected())
                    .adFlag(authenticateDataCheckBox.isSelected())
                    .doFlag(DNSSECOkCheckBox.isSelected())
                    .domain(getDomain())
                    .types(getRecordTypes())
                    .build();
            ConnectionSettings connectionSettings = new ConnectionSettings.ConnectionSettingsBuilder()
                    .holdConnection(holdConnectionCheckbox.isSelected())
                    .transport_protocol(getTransportProtocol())
                    .application_protocol(APPLICATION_PROTOCOL.DNS)
                    .resolverIP(dnsServer)
                    .netInterface(getInterface())
                    .build();
            if (transport == TRANSPORT_PROTOCOL.TCP) {
                task = new DNSOverTCPTask(requestSettings, connectionSettings);
            } else {
                task = new DNSOverUDPTask(requestSettings, connectionSettings);
            }
            task.setController(this);
            numberOfMessagesValueLabel.textProperty().bind(task.messagesSentPropertyProperty().asString());
            responseTimeValueLabel.textProperty().bind(task.durationPropertyProperty().asString());
            requestTreeView.rootProperty().bind(task.requestPropertyProperty());
            responseTreeView.rootProperty().bind(task.responsePropertyProperty());
            querySizeLabel.textProperty().bind(task.querySizeProperty().asString());
            responseSizeLabel.textProperty().bind(task.responseSizeProperty().asString());
            thread = new Thread(task);
            // pass new progress bar to Task
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            task.setProgressBar(progressBar);
            thread.start();
        } catch (NotValidDomainNameException | NotValidIPException | DnsServerIpIsNotValidException
                | MoreRecordsTypesWithPTRException | NonRecordSelectedException | NoIpAddrForDomainName | IOException e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            showAlert(e);
        }
    }


    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    @FXML
    private void deleteDomainNameHistoryFired(Event event) {
		settings.eraseDomainNames();
		savedDomainNamesChoiseBox.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    @FXML
    private void transportProtocolAction(ActionEvent event) {
        if (tcpRadioButton.isSelected()) {
            holdConnectionCheckbox.setDisable(false);
        } else {
            holdConnectionCheckbox.setDisable(true);
            holdConnectionCheckbox.setSelected(false);
        }
    }

    @FXML
    private void changeLayout(ActionEvent event) {
        // change to small layout
        try {
            FXMLLoader loader = null;
            if (layoutLarge) {
                loader = new FXMLLoader(getClass().getResource(FXML_FILE_NAME_SMALL));
            } else {
                loader = new FXMLLoader(getClass().getResource(FXML_FILE_NAME_LARGE));
            }
            Stage newStage = new Stage();

            newStage.setScene(new Scene((Parent) loader.load()));
            GeneralController controller = (GeneralController) loader.getController();

            Stage oldStage = (Stage) sendButton.getScene().getWindow();
            newStage.setX(oldStage.getX());
            newStage.setY(oldStage.getY());
            newStage.getIcons().add(new Image(App.ICON_URI));
            controller.setSettings(settings);
            controller.setIpDns(ipDns);
            controller.setLabels();
            controller.loadDataFromSettings();
            controller.networkInterfaces();
            App.stage = newStage;
            newStage.show();
            oldStage.close();
            layoutLarge = !layoutLarge;
        } catch (Exception e) {
            LOGGER.severe("Could not open new window. Caused by: " + ExceptionUtils.getStackTrace(e));
            Alert alert = new Alert(AlertType.ERROR, GeneralController.language.getLanguageBundle().getString("windowError"));
            alert.showAndWait();
        }
    }

}