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
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.*;
import tasks.DNSOverHTTPSTask;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ResourceBundle;

public class DoHController extends GeneralController {
    @FXML
    @Translation
    protected RadioButton jsonFormat;
    @FXML
    @Translation
    protected RadioButton wireFormat;
    @FXML
    @Translation
    protected TitledPane requestFormatTiltedPane;
    @FXML
    protected ToggleGroup requestFormatToggleGroup;
    @FXML
    protected TitledPane getPostTiltedPane;
    @FXML
    protected RadioButton get;
    @FXML
    protected RadioButton post;
    @FXML
    protected ToggleGroup getPostToggleGroup;

    private Stage helpStage = null;

    public static final String FXML_FILE_NAME = "/fxml/DoH_small.fxml";

    private RadioButton isGetRadioButton;


    public DoHController() {
        super();
        PROTOCOL = "DNS over Https";
    }

    @Override
    protected void updateCustomParameters() {
        parameters.put(WiresharkFilter.Parameters.TCPPORT, "443");
        parameters.put(WiresharkFilter.Parameters.UDPPORT, "443");
    }

    @Override
    public String getProtocol() {
        return "DoH";
    }

    public void initialize() {
        super.initialize();
        dnsserverToggleGroup = new ToggleGroup();
        IPprotToggleGroup = new ToggleGroup();
        IPv4RadioButton.setToggleGroup(IPprotToggleGroup);
        IPv6RadioButton.setToggleGroup(IPprotToggleGroup);
        useDomainName.setToggleGroup(IPprotToggleGroup);
        iterativeToggleGroup = new ToggleGroup();
        recursiveQueryRadioButton.setToggleGroup(iterativeToggleGroup);
        iterativeQueryRadioButton.setToggleGroup(iterativeToggleGroup);
        requestFormatToggleGroup = new ToggleGroup();
        jsonFormat.setToggleGroup(requestFormatToggleGroup);
        wireFormat.setToggleGroup(requestFormatToggleGroup);
        getPostToggleGroup = new ToggleGroup();
        get.setToggleGroup(getPostToggleGroup);
        post.setToggleGroup(getPostToggleGroup);
        post.setOnMouseClicked(event -> disableGetOnlyServers(true));
        get.setOnMouseClicked(event -> disableGetOnlyServers(false));

        Config.getNameServers().stream().filter(NameServer::isDoh).forEach(nameServer -> otherDNSVbox.getChildren()
                .add(new NameServerVBox(nameServer, dnsserverToggleGroup, this)));
        HBox customDNS = new HBox();
        RadioButton customToggle = new RadioButton();
        isGetRadioButton = new RadioButton();
        isGetRadioButton.setText("GET");
        isGetRadioButton.setTooltip(new Tooltip("GET"));
        customToggle.setToggleGroup(dnsserverToggleGroup);
        TextField input = new TextField();

        customToggle.setUserData(input);
        input.setOnMouseClicked(actionEvent -> {
            customToggle.setSelected(true);
        });
        customDNS.getChildren().addAll(customToggle, input,isGetRadioButton);
        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 5, 0));
        otherDNSVbox.getChildren().add(separator);
        otherDNSVbox.getChildren().add(customDNS);

        setLanguageRadioButton();
    }

    private void disableGetOnlyServers(boolean disable) {
        otherDNSVbox.getChildren().stream()
                .filter(node -> node instanceof NameServerVBox)
                .map(node -> (NameServerVBox) node)
                .forEach(nameServerVBox -> {
                        nameServerVBox.setDisable(nameServerVBox.getNameServer().isGetOnly() && disable);
                    }
                );
    }


    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    protected void setCustomUserDataRecords()
    {
        cnameCheckBox.setUserData(Q_COUNT.CNAME);
        mxCheckBox.setUserData(Q_COUNT.MX);
        nsCheckBox.setUserData(Q_COUNT.NS);
        caaCheckBox.setUserData(Q_COUNT.CAA);
        dnskeyCheckBox.setUserData(Q_COUNT.DNSKEY);
        soaCheckBox.setUserData(Q_COUNT.SOA);
        dsCheckBox.setUserData(Q_COUNT.DS);
        rrsigCheckBox.setUserData(Q_COUNT.RRSIG);
        nsec3paramCheckBox.setUserData(Q_COUNT.NSEC3PARAM);
        nsec3CheckBox.setUserData(Q_COUNT.NSEC3);
        soaCheckBox.setUserData(Q_COUNT.SOA);
        checkBoxArray.add(soaCheckBox);
        checkBoxArray.add(nsec3CheckBox);
        checkBoxArray.add(cnameCheckBox);
        checkBoxArray.add(mxCheckBox);
        checkBoxArray.add(nsCheckBox);
        checkBoxArray.add(caaCheckBox);
        checkBoxArray.add(dnskeyCheckBox);
        checkBoxArray.add(dsCheckBox);
        checkBoxArray.add(rrsigCheckBox);
        checkBoxArray.add(nsec3paramCheckBox);
        // 240979
        svcbCheckBox.setUserData(Q_COUNT.SVCB);
        checkBoxArray.add(svcbCheckBox);
        httpsCheckBox.setUserData(Q_COUNT.HTTPS);
        checkBoxArray.add(httpsCheckBox);
    }

    @FXML
    protected void sendButtonFired(ActionEvent event){
        super.sendButtonFired(event);
        if (isTerminatingThread()){
            return;
        }
        try {
            String domain =  getDnsServerIp();
            if (domain == null){
                Platform.runLater(()->sendButton.setText(getButtonText()));
                return;
            }
            Q_COUNT[] qCount = getRecordTypes();
            String path = getPath();
            String resolverURL = "dummy resolver";
            boolean isGet = get.isSelected();
            logRequest(authenticateDataCheckBox.isSelected(), checkingDisabledCheckBox.isSelected(), domain, qCount, resolverURL);
            RequestSettings requestSettings = new RequestSettings.RequestSettingsBuilder()
                    .recursion(recursiveQueryRadioButton.isSelected())
                    .adFlag(authenticateDataCheckBox.isSelected())
                    .cdFlag(checkingDisabledCheckBox.isSelected())
                    .doFlag(DNSSECOkCheckBox.isSelected())
                    .types(getRecordTypes())
                    .domain(getDomain())
                    .isGet(isGet)
                    .build();

            ConnectionSettings connectionSettings = new ConnectionSettings.ConnectionSettingsBuilder()
                    .application_protocol(APPLICATION_PROTOCOL.DOH)
                    .transport_protocol(TRANSPORT_PROTOCOL.TCP)
                    .resolverIP(domain)
                    .netInterface(getInterface())
                    .resolverUri(getDnsServerDomainName(getDnsServerIp()))
                    .isReqJsonFormat(isRequestJson())
                    .isDomainNameUsed(isDomainNameUsed())
                    .path(path)
                    .build();
            task = new DNSOverHTTPSTask(requestSettings, connectionSettings);

            numberOfMessagesValueLabel.textProperty().bind(task.messagesSentPropertyProperty().asString());
            responseTimeValueLabel.textProperty().bind(task.durationPropertyProperty().asString());
            requestTreeView.rootProperty().bind(task.requestPropertyProperty());
            responseTreeView.rootProperty().bind(task.responsePropertyProperty());
            querySizeLabel.textProperty().bind(task.querySizeProperty().asString());
            responseSizeLabel.textProperty().bind(task.responseSizeProperty().asString());
            task.setController(this);
            thread = new Thread(task);
            // pass new progress bar to Task
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            task.setProgressBar(progressBar);
            thread.start();
        } catch (NotValidDomainNameException
                 | NotValidIPException
                 | MoreRecordsTypesWithPTRException
                 | NonRecordSelectedException
                 | IOException
                 | DnsServerIpIsNotValidException
                 | NoIpAddrForDomainName e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            showAlert(e);
        }
    }

    private String getPath() {
        if (dnsserverToggleGroup.getSelectedToggle().getUserData() instanceof TextField textField){
            return textField.getText().split("/")[1];
        }
        return nameServer.getPath();
    }

    private boolean isServerGet() {
        if (dnsserverToggleGroup.getSelectedToggle().getUserData() instanceof TextField){
            return isGetRadioButton.isSelected();
        }
        return nameServer.isGetOnly();
    }

    @Override
    protected String getDnsServerIp() throws DnsServerIpIsNotValidException, UnknownHostException, NoIpAddrForDomainName, NotValidDomainNameException, UnsupportedEncodingException {
        Toggle selected = dnsserverToggleGroup.getSelectedToggle();

        if (selected == null) {
            Platform.runLater(()->sendButton.setText(getButtonText()));
            showAlert("ChooseDNSServer");
            return null;
        }

        Object userDataObject = selected.getUserData();

        String serverIp = null;

        switch (userDataObject) {
            case null -> {
                return null;
            }
            case String s -> serverIp = s;
            case NameServer server -> serverIp = IPv4RadioButton.isSelected() ?
                    server.getIpv4().getFirst() :
                    server.getIpv6().getFirst();
            case ToggleGroup group -> {
                Toggle selectedAddress = group.getSelectedToggle();
                if (selectedAddress == null) {
                    sendButton.setText(getButtonText());
                    showAlert("ChooseDNSServer");
                    return null;
                }
                serverIp = (String) selectedAddress.getUserData();
            }
            case TextField input -> {
                String inputString = input.getText();
                if (inputString.split("/").length < 2) {
                    throw new NotValidDomainNameException();
                }
                if (DomainConvert.isValidDomainName(inputString.split("/")[0])) {
                    if (IPv4RadioButton.isSelected()) {
                        serverIp = Ip.getIpV4OfDomainName(inputString.split("/")[0]);
                    } else {
                        serverIp = Ip.getIpV6OfDomainName(inputString.split("/")[0]);
                    }
                    if (serverIp == null) {
                        throw new NoIpAddrForDomainName();
                    }
                } else if (!Ip.isIpValid(inputString.split("/")[0])) {
                    throw new DnsServerIpIsNotValidException();
                } else {
                    serverIp = inputString.split("/")[0];
                }
            }
            default -> {
            }
        }

        return serverIp;
    }

    @Override
    protected void saveDomain(String domain) {
        settings.addDNSDomain(domain);
    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    private void logRequest(boolean dnssec, boolean signatures, String domain, Q_COUNT[] qcount, String resolverURL) {
        StringBuilder records = new StringBuilder();
        for (Q_COUNT q_COUNT : qcount) {
            records.append(q_COUNT).append(",");
        }
        LOGGER.info("DoH:\n " + "dnssec: " + dnssec + "\n" + "signatures: " + signatures + "\n" + "domain: " + domain
                + "\n" + "records: " + records + "\n" + "resolverURL: " + resolverURL);

    }

    /*
     * Body of method taken from Martin Biolek thesis and modified
     * */
    public void loadDataFromSettings() {
        savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesDNS());
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

    protected boolean isRequestJson(){
        return this.jsonFormat.isSelected();
    }

    @FXML
    protected void useDomainNameAction()
    {

    }

    public void helpFired(ActionEvent actionEvent) {
        if (helpStage != null && helpStage.isShowing()) {   // This part of the code should prevent user from opening infinite amount of windows
            helpStage.toFront();
            helpStage.requestFocus();
            return;
        }
        ResourceBundle bundle = GeneralController.language.getLanguageBundle();
        helpStage = new Stage();
        helpStage.getIcons().add(new Image(App.ICON_URI));
        helpStage.setTitle(bundle.getString("helpItem"));
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setText(bundle.getString("helpDoH"));
        VBox vbox = new VBox(textArea);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        Scene scene = new Scene(vbox, 600, 400);
        helpStage.setScene(scene);
        helpStage.show();
        helpStage.setOnCloseRequest(e -> helpStage = null); // Here I set it to null on close, to be able to open it again
    }
}
