package ui;

import enums.*;
import exceptions.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;

import lombok.extern.slf4j.Slf4j;
import models.*;
import tasks.DNSOverMulticastTask;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Slf4j
public class MDNSController extends GeneralController {

    public static final String FXML_FILE_NAME = "/fxml/MDNS_small.fxml";
    // menu items


    @FXML
    protected ProgressBar progressBar = new ProgressBar();
    // text fields
    @FXML
    protected TextField domainNameTextField;
    // checkboxes
    @FXML
    protected CheckBox aCheckBox;
    @FXML
    protected CheckBox aaaaCheckBox;
    @FXML
    protected CheckBox nsecCheckBox;
    @FXML
    protected CheckBox ptrCheckBox;
    @FXML
    protected CheckBox txtCheckBox;
    @FXML
    protected CheckBox dnssecRecordsRequestCheckBox;
    @FXML
    protected CheckBox anyCheckBox;

    @FXML
    protected TitledPane dnssecTitledPane;
    @FXML
    protected TitledPane recordTypeTitledPane;
    @FXML
    protected TitledPane queryTitledPane;
    @FXML
    protected TitledPane responseTitledPane;
    @FXML
    protected ComboBox<String> savedDomainNamesChoiceBox;
    @FXML
    protected TreeView<String> requestTreeView;
    @FXML
    protected TreeView<String> responseTreeView;
    protected MessageParser parser;
    //protected MessageSender sender;
    @FXML
    CheckBox DNSSECOkCheckBox;
    @FXML
    CheckBox authenticateDataCheckBox;
    // titledpane
    @FXML
    CheckBox checkingDisabledCheckBox;
    // radio buttons
    @FXML
    private RadioButton ipv4RadioButton = new RadioButton();

    // labels
    @FXML
    private RadioButton ipv6RadioButton = new RadioButton();
    @FXML
    private RadioButton multicastResponseRadioButton;
    @FXML
    private RadioButton unicastResponseRadioButton;
    @FXML
    private CheckBox srvCheckBox;
    @FXML
    @Translation
    protected TitledPane multicastResponseTitledPane;
    private ToggleGroup multicastResponseToggleGroup;

    public MDNSController() {
        super();
        LOGGER = Logger.getLogger(MDNSController.class.getName());
        PROTOCOL = "mDNS";
    }

    @Override
    protected void updateCustomParameters() {
        parameters.put(WiresharkFilter.Parameters.TCPPORT,"5353");
        parameters.put(WiresharkFilter.Parameters.UDPPORT,"5353");
    }


    @Override
    protected void IPv4RadioButtonAction(ActionEvent event) {

    }

    @Override
    protected void IPv6RadioButtonAction(ActionEvent event) {

    }

    public void initialize() {
        super.initialize();
        // toogleGroup
        ToggleGroup ipToggleGroup = new ToggleGroup();
        IPv4RadioButton.setToggleGroup(ipToggleGroup);
        IPv6RadioButton.setToggleGroup(ipToggleGroup);
        ipv4RadioButton.setSelected(true);

        multicastResponseToggleGroup = new ToggleGroup();
        multicastResponseRadioButton.setUserData(RESPONSE_MDNS_TYPE.RESPONSE_MULTICAST);
        unicastResponseRadioButton.setUserData(RESPONSE_MDNS_TYPE.RESPONSE_UNICAST);
        multicastResponseRadioButton.setToggleGroup(multicastResponseToggleGroup);
        unicastResponseRadioButton.setToggleGroup(multicastResponseToggleGroup);
        multicastResponseRadioButton.setSelected(true);
        /*interfaceToggleGroup = new ToggleGroup();*/
        setLanguageRadioButton();
    }

    @Override
    public String getProtocol() {
        return "MDNS";
    }

    public void setLabels() {

        setUserDataRecords();
    }

    protected void setUserDataRecords() {
        aCheckBox.setUserData(Q_COUNT.A);
        aaaaCheckBox.setUserData(Q_COUNT.AAAA);
        ptrCheckBox.setUserData(Q_COUNT.PTR);
        nsecCheckBox.setUserData(Q_COUNT.NSEC);
        txtCheckBox.setUserData(Q_COUNT.TXT);
        srvCheckBox.setUserData(Q_COUNT.SRV);
        anyCheckBox.setUserData(Q_COUNT.ANY);
    }

    @Override
    protected void setCustomUserDataRecords() {

    }

    protected Q_COUNT[] getRecordTypes() throws MoreRecordsTypesWithPTRException, NonRecordSelectedException {
        ArrayList<Q_COUNT> list = new ArrayList<>();
        CheckBox[] checkBoxArray = {aCheckBox, aaaaCheckBox, ptrCheckBox, txtCheckBox, nsecCheckBox, srvCheckBox,
                anyCheckBox};
        for (CheckBox checkBox : checkBoxArray) {
            if (checkBox.isSelected()) {
                list.add((Q_COUNT) checkBox.getUserData());
            }
        }
        if (list.contains(Q_COUNT.PTR) && list.size() > 1) {
            throw new MoreRecordsTypesWithPTRException();
        }
        if (list.isEmpty()) {
            throw new NonRecordSelectedException();
        }
        Q_COUNT[] returnList = new Q_COUNT[list.size()];
        for (int i = 0; i < returnList.length; i++) {
            returnList[i] = list.get(i);
        }
        return returnList;
    }

    protected String getDomain() throws NotValidDomainNameException, UnsupportedEncodingException {
        String domain = domainNameTextField.getText();

        if (ptrCheckBox.isSelected()) {
            if (Ip.isIpValid(domain) || domain.contains(".arpa")) {
                return domain;
            } else {
                throw new NotValidDomainNameException();
            }
        }
        if (!DomainConvert.isValidDomainName(domain)) {
            throw new NotValidDomainNameException();
        }

        saveDomain(domain);
        DomainConvert.encodeMDNS(domain);
        return domain;
    }

    @Override
    protected void saveDomain(String domain) {
        settings.addMDNSDomain(domain);
    }

    @FXML
    private void getWiresharkFilter(ActionEvent event) {
        MenuItem item = (MenuItem) event.getSource();
        copyDataToClipBoard((String) item.getUserData());
    }

    @FXML
    private void onDomainNameMDNSChoiceBoxFired() {
        savedDomainNamesChoiceBox.getItems().removeAll(savedDomainNamesChoiceBox.getItems());
        savedDomainNamesChoiceBox.getItems().addAll(settings.getDomainNamesMDNS());
    }

    public void loadDataFromSettings() {
        savedDomainNamesChoiceBox.getItems().addAll(settings.getDomainNamesMDNS());
    }

    @FXML
    private void onDomainNameMDNSChoiceBoxAction() {
        try {
            if (savedDomainNamesChoiceBox.getValue() != null
                    && !savedDomainNamesChoiceBox.getValue().isEmpty()) {
                domainNameTextField.setText(savedDomainNamesChoiceBox.getValue());
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    @FXML
    protected void czechSelected() {
        language.changeLanguageBundle(true);
        setLabels();
    }

    @FXML
    protected void englishSelected() {
        language.changeLanguageBundle(false);
        setLabels();
    }

    private void logAction(Q_COUNT[] records, String domain, boolean dnssec, IP_PROTOCOL networkProtocol,
                           RESPONSE_MDNS_TYPE mdnsType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Domain: ")
                .append(domain)
                .append("DNSSEC: ")
                .append(dnssec)
                .append("\n")
                .append("IP: ")
                .append(networkProtocol.toString())
                .append("\n")
                .append("MDNS response: ")
                .append(mdnsType.toString())
                .append("\n")
                .append("Records: ")
                .append("\n");

        for (Q_COUNT q_COUNT : records) {
            sb.append("\t")
                    .append(q_COUNT.toString())
                    .append("\n");
        }
        LOGGER.info(sb.toString());

    }


    @Override
    protected String getDnsServerIp() {
        if (IPv4RadioButton.isSelected()) {
            return "224.0.0.251";
        } else {
            return "ff02::fb";
        }
    }

    @FXML
    protected void sendButtonFired(ActionEvent event){
        super.sendButtonFired(event);
        if (isTerminatingThread()) {
            return;
        }
        try {
            Q_COUNT[] records = getRecordTypes();
            String domain = getDomain();
            boolean multicast = unicastResponseRadioButton.isSelected();
            boolean doFlag = DNSSECOkCheckBox.isSelected();
            RESPONSE_MDNS_TYPE mdnsType = (RESPONSE_MDNS_TYPE) multicastResponseToggleGroup.getSelectedToggle()
                    .getUserData();
            logAction(records, domain, doFlag, IPv4RadioButton.isSelected() ? IP_PROTOCOL.IPv4 : IP_PROTOCOL.IPv6, mdnsType);
            RequestSettings rs = new RequestSettings.RequestSettingsBuilder()
                    .cdFlag(checkingDisabledCheckBox.isSelected())
                    .adFlag(authenticateDataCheckBox.isSelected())
                    .doFlag(DNSSECOkCheckBox.isSelected())
                    .domain(getDomain())
                    .types(getRecordTypes())
                    .build();
            ConnectionSettings cs = new ConnectionSettings.ConnectionSettingsBuilder()
                    .transport_protocol(TRANSPORT_PROTOCOL.UDP)
                    .application_protocol(APPLICATION_PROTOCOL.MDNS)
                    .resolverIP(getDnsServerIp())
                    .netInterface(getInterface())
                    .build();
            task = new DNSOverMulticastTask(rs, cs, multicast, IPv4RadioButton.isSelected(), (RESPONSE_MDNS_TYPE) multicastResponseToggleGroup.getSelectedToggle().getUserData());
            task.setController(this);

            thread = new Thread(task);
            // pass new progress bar to Task
            numberOfMessagesValueLabel.textProperty().bind(task.messagesSentPropertyProperty().asString());
            responseTimeValueLabel.textProperty().bind(task.durationPropertyProperty().asString());
            requestTreeView.rootProperty().bind(task.requestPropertyProperty());
            responseTreeView.rootProperty().bind(task.responsePropertyProperty());
            querySizeLabel.textProperty().bind(task.querySizeProperty().asString());
            responseSizeLabel.textProperty().bind(task.responseSizeProperty().asString());
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            task.setProgressBar(progressBar);
            LOGGER.info("Starting MDNS thread");
            thread.start();

        } catch (NotValidDomainNameException | NotValidIPException | MoreRecordsTypesWithPTRException
                | NonRecordSelectedException | IOException e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            showAlert(e);
        } catch (Exception e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            LOGGER.warning("Catch-all triggered! ");
            showAlert(e);
        }
    }


    protected List<String> autobindingsStringsArray(String textToFind, List<String> arrayToCompare) {
        List<String> result = new ArrayList<>();
        for (String string : arrayToCompare) {
            if (string.contains(textToFind))
                result.add(string);
        }

        return result;
    }

    protected void autobinding(String textFromField, List<String> fullArray, ComboBox<String> box) {
        List<String> result = autobindingsStringsArray(textFromField, fullArray);
        if (result.isEmpty()) {
            box.hide();
            box.getItems().removeAll(box.getItems());
            box.getItems().addAll(settings.getDomainNamesDNS());
        } else {
            box.getItems().removeAll(savedDomainNamesChoiceBox.getItems());
            box.getItems().setAll(result);
            box.show();
        }
    }

    @FXML
    private void deleteMDNSDomainNameHistoryFired() {
        settings.eraseMDNSDomainNames();
        savedDomainNamesChoiceBox.getItems().removeAll(savedDomainNamesChoiceBox.getItems());
    }

    @FXML
    protected void expandAllRequestOnClick() {
        expandAll(requestTreeView);
    }

    @FXML
    protected void expandAllResponseOnClick() {
        expandAll(responseTreeView);
    }

    @FXML
    private void domainNameKeyPressed(KeyEvent event) {
        controlKeys(event, domainNameTextField);
        autobinding(domainNameTextField.getText(), settings.getDomainNamesMDNS(), savedDomainNamesChoiceBox);
    }

    protected void controlKeys(KeyEvent e, TextField text) {
        byte b = e.getCharacter().getBytes()[0];
        if (b == (byte) 0x08 && !text.getText().isEmpty() && isRightToLeft(text.getText())) {
            System.out.println(text.getText());
            text.setText(text.getText().substring(1));
        }
    }

    private boolean isRightToLeft(String text) {
        char[] chars = text.toCharArray();
        for (char c : chars) {
            if (c >= 0x500 && c <= 0x6ff) {
                return true;
            }
        }
        return false;
    }

    protected void expandAll(TreeView<String> t) {
        try {
            int i = 0;
            while (t.getTreeItem(i).getValue() != null) {
                t.getTreeItem(i).setExpanded(true);
                i++;
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    @FXML
    private void deleteDomainNameHistoryFired() {
        settings.eraseMDNSDomainNames();
        savedDomainNamesChoiceBox.getItems().removeAll(savedDomainNamesChoiceBox.getItems());
    }
}
