package ui;

import application.Config;
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import models.*;
import tasks.DNSOverTLS;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

@Getter
@Setter
public class DoTController extends GeneralController {

    public static final String FXML_FILE_NAME_SMALL = "/fxml/DoT_small.fxml";
    public static final String FXML_FILE_NAME_LARGE = "/fxml/DoT_small.fxml";

    public static boolean layoutLarge = true;
    @FXML
    protected Label wiresharkLabel;
    @FXML
    protected RadioButton dnssecYesRadioButton;
    @FXML
    protected RadioButton dnssecNoRadioButton;
    // menu items
    @FXML
    protected MenuItem deleteDomainNameHistory;
    @FXML
    protected RadioMenuItem justIp;
    @FXML
    protected RadioMenuItem ipAsFilter;
    @FXML
    protected RadioMenuItem ipwithTCPAsFilter;
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
    @FXML
    protected TitledPane iterativeTitledPane;
    protected ToggleGroup otherDNSToggleGroup;
    protected ToggleGroup dnssecToggleGroup;
    @FXML
    protected ImageView cloudflareIpv4ImageView;
    @FXML
    protected ImageView googleIpv4IamgeView;

    @FXML
    private VBox vboxRoot;
    // text fields
    @FXML
    private TextField dnsServerTextField;
    @FXML
    private ComboBox<String> dnsServerComboBox;
    @FXML
    private TitledPane DNSSECTitledPane;
    @FXML
    private MenuItem deleteDNSServersHistory;
    @FXML
    private RadioMenuItem ipWithUDPAsFilter;
    @FXML
    private RadioMenuItem ipWithUDPandTcpAsFilter;
    // titledpane
    private ToggleGroup iterativeToggleGroup;
    // choice box
    @FXML
    private ComboBox<String> savedDNSChoiceBox;

    private Map<KeyCode, RadioButton> keyMappings;

    private TCPConnection tcp;

    public DoTController() {

        super();
        LOGGER = Logger.getLogger(DoTController.class.getName());
        PROTOCOL = "DoT";

    }

    @Override
    protected void updateCustomParameters() {
        parameters.put(WiresharkFilter.Parameters.TCPPORT, "853");
        parameters.put(WiresharkFilter.Parameters.UDPPORT, "853");
    }


    public void initialize() {
        super.initialize();
        iterativeToggleGroup = new ToggleGroup();
        iterativeQueryRadioButton.setToggleGroup(iterativeToggleGroup);
        recursiveQueryRadioButton.setToggleGroup(iterativeToggleGroup);

        dnssecToggleGroup = new ToggleGroup();

        IPprotToggleGroup = new ToggleGroup();

        IPv4RadioButton.setToggleGroup(IPprotToggleGroup);
        IPv6RadioButton.setToggleGroup(IPprotToggleGroup);
        useDomainName.setToggleGroup(IPprotToggleGroup);
        keyMappings = new HashMap<>();

        dnsserverToggleGroup = new ToggleGroup();

        Ip ip = new Ip();

        Config.getNameServers().add(new NameServer("System DNS", "System DNS", ip.getIpv4DnsServer(),
                ip.getIpv6DnsServer()));
        Config.getNameServers().stream().filter(nameServer1 -> nameServer1.isDot() && !nameServer1.isDohOnly()).forEach(ns -> {
            otherDNSVbox.getChildren().add(new NameServerVBox(ns, dnsserverToggleGroup, this));
        });

        HBox customDNS = new HBox();
        RadioButton customToggle = new RadioButton();
        customToggle.setToggleGroup(dnsserverToggleGroup);
        TextField input = new TextField();
        //input.setPromptText(language.getLanguageBundle().getString("dnsServerDropDownLabel"));
        customToggle.setUserData(input);
        input.setOnMouseClicked(actionEvent -> {
            customToggle.setSelected(true);
        });
        customDNS.getChildren().addAll(customToggle, input);
        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 5, 0));
        otherDNSVbox.getChildren().add(separator);
        otherDNSVbox.getChildren().add(customDNS);

        // hide root tree item in tree view, so it is expanded after receiving response
        requestTreeView.setShowRoot(false);
        responseTreeView.setShowRoot(false);
        setLanguageRadioButton();
    }

    @Override
    public String getProtocol() {
        return "DoT";
    }

    public void setLabels() {
        setKeyboardShortcuts();
    }

    private void setKeyboardShortcuts() {
        vboxRoot.getScene().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            System.out.println(ke.getCode());
            if (!ke.isShiftDown()) {
                return;
            }
            if (keyMappings.get(ke.getCode()) != null) {
                keyMappings.get(ke.getCode()).setSelected(true);
            }
            ke.consume();
        });
    }


    @Override
    protected void setCustomUserDataRecords() {
        cnameCheckBox.setUserData(Q_COUNT.CNAME);
        checkBoxArray.add(cnameCheckBox);
        mxCheckBox.setUserData(Q_COUNT.MX);
        checkBoxArray.add(mxCheckBox);
        nsCheckBox.setUserData(Q_COUNT.NS);
        checkBoxArray.add(nsCheckBox);
        caaCheckBox.setUserData(Q_COUNT.CAA);
        checkBoxArray.add(caaCheckBox);
        dnskeyCheckBox.setUserData(Q_COUNT.DNSKEY);
        checkBoxArray.add(dnskeyCheckBox);
        soaCheckBox.setUserData(Q_COUNT.SOA);
        checkBoxArray.add(soaCheckBox);
        dsCheckBox.setUserData(Q_COUNT.DS);
        checkBoxArray.add(dsCheckBox);
        rrsigCheckBox.setUserData(Q_COUNT.RRSIG);
        checkBoxArray.add(rrsigCheckBox);
        nsec3CheckBox.setUserData(Q_COUNT.NSEC3);
        checkBoxArray.add(nsec3CheckBox);
        nsec3paramCheckBox.setUserData(Q_COUNT.NSEC3PARAM);
        checkBoxArray.add(nsec3paramCheckBox);
        // 240979
        svcbCheckBox.setUserData(Q_COUNT.SVCB);
        checkBoxArray.add(svcbCheckBox);
        httpsCheckBox.setUserData(Q_COUNT.HTTPS);
        checkBoxArray.add(httpsCheckBox);
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    public void loadDataFromSettings() {
        savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesDNS());
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
     * Body of method taken from Martin Biolek thesis and modified
     * */
    private TRANSPORT_PROTOCOL getTransportProtocol() {
        return TRANSPORT_PROTOCOL.TCP;
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
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
     * Body of method taken from Martin Biolek thesis
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
                Platform.runLater(()->sendButton.setText(getButtonText()));
                return;
            }
            //addServerToList();
            LOGGER.info(dnsServer);
            Q_COUNT[] records = getRecordTypes();
            TRANSPORT_PROTOCOL transport = getTransportProtocol();
            String domain = getDomain();
            boolean recursive = isRecursiveSet();
            boolean holdConnection = false;
            boolean cdFlag = checkingDisabledCheckBox.isSelected();
            boolean adFlag = authenticateDataCheckBox.isSelected();
            boolean doFlag = DNSSECOkCheckBox.isSelected();
            logMessage(dnsServer, domain, records, recursive, adFlag, transport, doFlag, holdConnection, cdFlag);
            RequestSettings rs = new RequestSettings.RequestSettingsBuilder()
                    .recursion(recursive)
                    .adFlag(adFlag)
                    .cdFlag(cdFlag)
                    .doFlag(doFlag)
                    .domain(domain)
                    .types(records)
                    .build();
            ConnectionSettings cs = new ConnectionSettings.ConnectionSettingsBuilder()
                    .transport_protocol(TRANSPORT_PROTOCOL.TCP)
                    .application_protocol(APPLICATION_PROTOCOL.DOT)
                    .resolverIP(dnsServer)
                    .netInterface(getInterface())
                    .isDomainNameUsed(isDomainNameUsed())
                    .resolverUri(getDnsServerDomainName(getDnsServerIp()))
                    .build();
            task = new DNSOverTLS(rs,cs);
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
            task.setMassTesting(false);
            thread.start();
        } catch (NotValidDomainNameException | NoIpAddrForDomainName | NotValidIPException | DnsServerIpIsNotValidException
                | MoreRecordsTypesWithPTRException | NonRecordSelectedException | IOException e) {
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
    @Override
    protected void setWiresharkMenuItems() {
        parameters = new HashMap<String, String>();
        parameters.put("prefix", "ipv4");
        parameters.put("ip", null);
        parameters.put("tcpPort", null);
        wiresharkMenu.getItems().removeAll();
        filters = new LinkedList<>();
        filters.add(new WiresharkFilter("IP", "${ip}"));
        filters.add(new WiresharkFilter("IP filter", "${prefix}.addr == ${ip}"));
        filters.add(new WiresharkFilter("IP & TCP", "${prefix}.addr == ${ip} && tcp.port == ${tcpPort}"));
        for (WiresharkFilter filter : filters) {
            RadioMenuItem menuItem = new RadioMenuItem(filter.getName());
            menuItem.setUserData(filter);
            menuItem.setToggleGroup(wiresharkFilterToogleGroup);
            wiresharkMenu.getItems().add(menuItem);
        }
    }

}