package ui;

import application.Config;
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import exceptions.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import models.DomainConvert;
import models.Ip;
import models.NameServer;
import models.WiresharkFilter;
import tasks.DNSOverHTTPSTask;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

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

    protected ToggleGroup requestFormatToggleGroup;
    @FXML
    @Translation
    protected RadioButton useDomainName;

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


        Config.getNameServers().stream().filter(NameServer::isDoh).forEach(nameServer -> otherDNSVbox.getChildren()
                .add(new NameServerVBox(nameServer, dnsserverToggleGroup, this)));
        HBox customDNS = new HBox();
        RadioButton customToggle = new RadioButton();
        isGetRadioButton = new RadioButton();
        isGetRadioButton.setText("GET");
        isGetRadioButton.setTooltip(new Tooltip("GET"));
        customToggle.setToggleGroup(dnsserverToggleGroup);
        TextField input = new TextField();
        //input.setPromptText(language.getLanguageBundle().getString("dnsServerDropDownLabel"));
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
        // TODO add option for custom DNS server
    }

    private void setWiresharkUserData() {

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
            boolean isGet = isServerGet();
            String path = getPath();
            String resolverURL = "dummy resolver";
            logRequest(authenticateDataCheckBox.isSelected(), checkingDisabledCheckBox.isSelected(), domain, qCount, resolverURL);

            task = new DNSOverHTTPSTask(recursiveQueryRadioButton.isSelected(), authenticateDataCheckBox.isSelected(),
                    checkingDisabledCheckBox.isSelected(), DNSSECOkCheckBox.isSelected(),getDomain(),
                    getRecordTypes(), null, APPLICATION_PROTOCOL.DOH, domain+"/"+path,
                    getInterface(), isGet, getDnsServerDomainName(getDnsServerIp())+"/"+path, isRequestJson(), isDomainNameUsed());

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
        } catch (NotValidDomainNameException | NotValidIPException | MoreRecordsTypesWithPTRException | NonRecordSelectedException | IOException | DnsServerIpIsNotValidException e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            //e.printStackTrace();
            //String fullClassName = e.getClass().getSimpleName();
            //showAlert(fullClassName);
            showAlert(e);
        } catch (Exception e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            // LOGGER.warning(e.toString());
            // showAlert("Exception");
            showAlert(e);
        }
    }

    private String getPath() {
        if (dnsserverToggleGroup.getSelectedToggle().getUserData() instanceof TextField){
            TextField textField = (TextField) dnsserverToggleGroup.getSelectedToggle().getUserData();
            return textField.getText().split("/")[1];
        }
        return nameServer.getPath();
    }

    private boolean isServerGet() {
        if (dnsserverToggleGroup.getSelectedToggle().getUserData() instanceof TextField){
            return isGetRadioButton.isSelected();
        }
        return nameServer.isGet();
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

        if (userDataObject == null) {
            return null;
        }

        if (userDataObject instanceof String) {
            serverIp = (String) userDataObject;
        } else if (userDataObject instanceof NameServer) {
            serverIp = IPv4RadioButton.isSelected() ?
                    ((NameServer) userDataObject).getIpv4().getFirst() :
                    ((NameServer) userDataObject).getIpv6().getFirst();
        } else if (userDataObject instanceof ToggleGroup) {
            ToggleGroup group = (ToggleGroup) userDataObject;
            Toggle selectedAddress = group.getSelectedToggle();
            if (selectedAddress == null) {
                sendButton.setText(getButtonText());
                showAlert("ChooseDNSServer");
                return null;
            }
            serverIp = (String) selectedAddress.getUserData();
        } else if (userDataObject instanceof TextField) {
            TextField input = (TextField) userDataObject;
            String inputString = input.getText();
            if (inputString.split("/").length < 2){
                throw new NotValidDomainNameException();
            }
            if(DomainConvert.isValidDomainName(inputString.split("/")[0])){
                if (IPv4RadioButton.isSelected()){
                    serverIp = Ip.getIpV4OfDomainName(inputString.split("/")[0]);
                } else {
                    serverIp = Ip.getIpV6OfDomainName(inputString.split("/")[0]);
                }
                if (serverIp == null){
                    throw new NoIpAddrForDomainName();
                }
            } else if (!Ip.isIpValid(inputString.split("/")[0])){
                throw new DnsServerIpIsNotValidException();
            } else{
                serverIp = inputString.split("/")[0];
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
        //LOGGER.info("DoH:\n " + "dnssec: " + dnssec + "\n" + "signatures: " + signatures + "\n" + "domain: " + domain
       //         + "\n" + "records: " + records + "\n" + "resovlerURL: " + resolverURL);

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
        super.setWiresharkMenuItems();
    }

    protected boolean isRequestJson(){
        return this.jsonFormat.isSelected();
    }
    protected boolean isDomainNameUsed(){
        return this.useDomainName.isSelected();
    }

    @FXML
    protected void useDomainNameAction()
    {

    }
}
