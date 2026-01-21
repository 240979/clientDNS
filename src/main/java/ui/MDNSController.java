package ui;

import enums.*;
import exceptions.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.*;
import tasks.DNSOverMulticastTask;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MDNSController extends GeneralController {

    public static final String FXML_FILE_NAME = "/fxml/MDNS_small.fxml";
    // menu items


    @FXML
    protected ProgressBar progressBar = new ProgressBar();
    // text fields
    @FXML
    protected TextField domainNameTextField;
    @FXML
    protected RadioButton multicastRadioButton;
    @FXML
    protected RadioButton unicasttRadioButton;
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
    protected ComboBox<String> savedDomainNamesChoiseBox;
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
    @FXML
    private VBox vboxRoot;
    @FXML
    private MenuItem deleteMDNSDomainNameHistory;
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
    // toogleGroup
    private ToggleGroup ipToggleGroup;
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
        //ipv4RadioButton = new RadioButton();
        //ipv6RadioButton = new RadioButton();
        super.initialize();
        ipToggleGroup = new ToggleGroup();
        IPv4RadioButton.setToggleGroup(ipToggleGroup);
        IPv6RadioButton.setToggleGroup(ipToggleGroup);
        ipv4RadioButton.setSelected(true);

        multicastResponseToggleGroup = new ToggleGroup();
        //multicastResponseRadioButton = new RadioButton();
        //unicastResponseRadioButton = new RadioButton();
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
        ArrayList<Q_COUNT> list = new ArrayList<Q_COUNT>();
        CheckBox[] checkBoxArray = {aCheckBox, aaaaCheckBox, ptrCheckBox, txtCheckBox, nsecCheckBox, srvCheckBox,
                anyCheckBox};
        for (int i = 0; i < checkBoxArray.length; i++) {
            if (checkBoxArray[i].isSelected()) {
                list.add((Q_COUNT) checkBoxArray[i].getUserData());
            }
        }
        if (list.contains(Q_COUNT.PTR) && list.size() > 1) {
            throw new MoreRecordsTypesWithPTRException();
        }
        if (list.size() == 0) {
            throw new NonRecordSelectedException();
        }
        Q_COUNT returnList[] = new Q_COUNT[list.size()];
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
    private void onDomainNameMDNSChoiseBoxFired(Event event) {
        savedDomainNamesChoiseBox.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
        savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesMDNS());
    }

    public void loadDataFromSettings() {
        savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesMDNS());
    }

    @FXML
    private void onDomainNameMDNSChoiseBoxAction(Event event) {
        try {
            if (!savedDomainNamesChoiseBox.getValue().equals(null)
                    && !savedDomainNamesChoiseBox.getValue().equals("")) {
                domainNameTextField.setText(savedDomainNamesChoiseBox.getValue());
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    @FXML
    protected void czechSelected(ActionEvent event) {
        language.changeLanguageBundle(true);
        setLabels();
    }

    @FXML
    protected void englishSelected(ActionEvent event) {
        language.changeLanguageBundle(false);
        setLabels();
    }

    private void logAction(Q_COUNT[] records, String domain, boolean dnssec, IP_PROTOCOL networkProtocol,
                           RESPONSE_MDNS_TYPE mdnsType) {
        String res = "";
        res += "Domain: " + domain + "\n";
        res += "DNSSEC: " + dnssec + "\n";
        res += "IP: " + networkProtocol.toString() + "\n";
        res += "MDNS response: " + mdnsType.toString() + "\n";
        res += "Records: \n";

        for (Q_COUNT q_COUNT : records) {
            res += "\t" + q_COUNT.toString() + "\n";
        }
        LOGGER.info(res);

    }

    protected void showAllertStringContent(String content) {
        Alert alert = new Alert(AlertType.ERROR, content);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner((Stage) sendButton.getScene().getWindow());
        alert.show();
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
            Q_COUNT records[] = getRecordTypes();
            String domain = getDomain();
            boolean multicast = unicastResponseRadioButton.isSelected();
            boolean caFlag = checkingDisabledCheckBox.isSelected();
            boolean adFlag = authenticateDataCheckBox.isSelected();
            boolean doFlag = DNSSECOkCheckBox.isSelected();
            RESPONSE_MDNS_TYPE mdnsType = (RESPONSE_MDNS_TYPE) multicastResponseToggleGroup.getSelectedToggle()
                    .getUserData();
            logAction(records, domain, doFlag, IPv4RadioButton.isSelected() ? IP_PROTOCOL.IPv4 : IP_PROTOCOL.IPv6, mdnsType);

            task = new DNSOverMulticastTask(multicast, adFlag, caFlag, doFlag, getDomain(),
                    records, TRANSPORT_PROTOCOL.UDP, APPLICATION_PROTOCOL.MDNS, getDnsServerIp(), getInterface(),
                    IPv4RadioButton.isSelected(), (RESPONSE_MDNS_TYPE) multicastResponseToggleGroup.getSelectedToggle().getUserData());
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
            //e.printStackTrace();
            //String fullClassName = e.getClass().getSimpleName();
            //showAlert(fullClassName);
            showAlert(e);
        } catch (Exception e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            //LOGGER.warning(e.toString());
            //showAlert("Exception");
            showAlert(e);
        }
    }
    /*
    protected void setRequestAfterNotRieciveResponse() {
        requestTreeView.setRoot(sender.getAsTreeItem());
        queryTitledPane.setText(language.getLanguageBundle().getString(queryTitledPane.getId().toString()) + " ("
                + sender.getByteSizeQuery() + " B)");
        expandAll(requestTreeView);
        responseTreeView.setRoot(null);
        responseTitledPane.setText(language.getLanguageBundle().getString(queryTitledPane.getId().toString()));
        copyRequestJsonButton.setDisable(false);
        copyResponseJsonButton.setDisable(true);
        responseTimeValueLabel.setText("0");
    }
    */
    protected void copyDataToClipBoard(String data) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(data);
        clipboard.setContent(content);
    }

    protected void informWindow(String textToShow) {
        Alert alert = new Alert(AlertType.INFORMATION, textToShow);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner((Stage) sendButton.getScene().getWindow());
        alert.show();
    }

    protected List<String> autobindingsStringsArray(String textToFind, List<String> arrayToCompare) {
        List<String> result = new ArrayList<String>();
        for (String string : arrayToCompare) {
            if (string.contains(textToFind))
                result.add(string);
        }

        return result;
    }

    protected void autobinging(String textFromField, List<String> fullArray, ComboBox<String> box) {
        List<String> result = autobindingsStringsArray(textFromField, fullArray);
        if (result.size() == 0) {
            box.hide();
            box.getItems().removeAll(box.getItems());
            box.getItems().addAll(settings.getDomainNamesDNS());
        } else {
            box.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
            box.getItems().setAll(result);
            box.show();
        }
    }

    @FXML
    private void deleteMDNSDomainNameHistoryFired(Event event) {
        settings.eraseMDNSDomainNames();
        savedDomainNamesChoiseBox.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
    }

    @FXML
    protected void expandAllRequestOnClick(Event event) {
        expandAll(requestTreeView);
    }

    @FXML
    protected void expandAllResponseOnClick(Event event) {
        expandAll(responseTreeView);
    }

    @FXML
    private void domainNameKeyPressed(KeyEvent event) {
        controlKeys(event, domainNameTextField);
        autobinging(domainNameTextField.getText(), settings.getDomainNamesMDNS(), savedDomainNamesChoiseBox);
    }
    /*
     * @FXML private void treeViewclicked(Event event) {
     *
     * }
     */

    protected void controlKeys(KeyEvent e, TextField text) {
        byte b = e.getCharacter().getBytes()[0];
        if (b == (byte) 0x08 && text.getText().length() >= 1 && isRightToLeft(text.getText())) {
            System.out.println(text.getText());
            text.setText(text.getText().substring(1, text.getText().length()));
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
            while (true) {
                if (t.getTreeItem(i).getValue() == null) {
                    break;
                } else {
                    t.getTreeItem(i).setExpanded(true);
                }
                i++;
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
    }

    @FXML
    private void deleteDomainNameHistoryFired(Event event) {
        settings.eraseMDNSDomainNames();
        savedDomainNamesChoiseBox.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
    }
}
