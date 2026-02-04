package ui;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Some utility methods from Martin Biolek thesis are marked
 * */
import application.Config;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import enums.APPLICATION_PROTOCOL;
import enums.Q_COUNT;
import enums.TRANSPORT_PROTOCOL;
import exceptions.MoreRecordsTypesWithPTRException;
import exceptions.NonRecordSelectedException;
import exceptions.NotValidDomainNameException;
import exceptions.NotValidIPException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import lombok.Data;
import models.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import testing.Result;
import testing.tasks.TesterTask;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Data
public class TesterController extends GeneralController {

    public static final String FXML_FILE_NAME = "/fxml/Tester.fxml";

    private ToggleGroup dnsProtocolToggleGroup;

    @FXML
    private TableView<Result> resultsTableView;

    @FXML
    @Translation
    protected TableColumn<Result, String> nameColumn;

    @FXML
    @Translation
    protected TableColumn<Result, Double> durationColumn;

    @FXML
    @Translation
    protected TableColumn<Result, String> domainColumn;

    @FXML
    private TableColumn<Result, Double> maxColumn;

    @FXML
    private TableColumn<Result, Double> minColumn;

    @FXML
    @Translation
    protected TableColumn<Result, Long> succColumn;

    @FXML
    @Translation
    protected TableColumn<Result, Long> failColumn;

    @FXML
    @Translation
    protected TableColumn<Result, Double> sizeColumn;

    @FXML
    private RadioButton dnsUdpButton;

    @FXML
    private RadioButton dnsTcpButton;

    @FXML
    private RadioButton dnsDohButton;

    @FXML
    private RadioButton dnsDotButton;

    @FXML
    @Translation
    protected CheckBox holdConnectionCheckbox;

    @FXML
    private TextField numberOfRequests;

    @FXML
    @Translation
    protected Button testAllButton;

    @FXML
    @Translation
    protected TitledPane protocolTitledPane;

    @FXML
    @Translation
    protected TitledPane durationTitledPane;

    @FXML
    @Translation
    protected TitledPane cooldownTitledPane;

    @FXML
    protected TextField cooldownTextField;

    @FXML
    @Translation
    protected Menu resultMenu;

    @FXML
    @Translation
    protected MenuItem copyCsv;

    @FXML
    @Translation
    protected MenuItem copyJson;

    @FXML
    @Translation
    protected MenuItem copyJsonResponses;


    private List<NameServerVBox> nameServerVBoxes;

    private ObservableList<Result> observableList;

    private static Logger LOGGER = Logger.getLogger(TesterController.class.getName());

    public void initialize() {
        super.initialize();
        // create a menu

        resultsTableView.setRowFactory(tv -> {
            TableRow<Result> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (! row.isEmpty() && event.getButton()== MouseButton.PRIMARY
                        && event.getClickCount() == 2) {

                    Result clickedRow = row.getItem();
                    //System.out.println(clickedRow.getName()+" "+clickedRow.getDomain());
                    LOGGER.info(clickedRow.getName()+" "+clickedRow.getDomain());
                    /*String out = clickedRow.getResponses().stream().map(responses ->
                            responses.stream().map(response -> response.getRdata().getDataAsString()).collect(Collectors.joining("\n"))).collect(Collectors.joining("\n========\n"));*/
                    //Platform.runLater(()->showCustomAlert(out, Alert.AlertType.INFORMATION));
                    Platform.runLater(()->showAlertMultipleExceptions(clickedRow.getExceptions()));
                    //Platform.runLater(()->showAlertMultipleExceptions(clickedRow.getExceptions()));
                }
            });
            return row;
        });

        copyCsv.setOnAction(actionEvent -> {
            ObservableList<Result> results = resultsTableView.getItems();
            String csvHead = "DNSserver;Domain;avgTime;minTime;maxTime;Success;Fail;Size";
            StringBuilder builder = new StringBuilder();
            builder.append(csvHead).append("\n");
            results.forEach(r -> builder.append(r.toCsvString()));
            copyDataToClipBoard(builder.toString());
            showAlert("dataCopied", Alert.AlertType.INFORMATION);
        });

        copyJson.setOnAction(actionEvent -> {
            ObservableList<Result> results = resultsTableView.getItems();
            JSONArray result = new JSONArray();
            results.forEach(r -> result.add(r.toJsonObject()));
            copyDataToClipBoard(result.toJSONString());
            showAlert("dataCopied", Alert.AlertType.INFORMATION);
        });

        copyJsonResponses.setOnAction(actionEvent -> {
            ObservableList<Result> results = resultsTableView.getItems();
            JSONObject jsonResult = new JSONObject();
            results.forEach(result -> {
                JSONObject jsonObjectServer = jsonResult.get(result.getNs().getName()) == null?
                        new JSONObject():(JSONObject) jsonResult.get(result.getNs().getName());
                result.getResponses().forEach(responsesList -> {
                    JSONArray jsonResponsesArray = new JSONArray();
                    if (responsesList.getFirst().getDohData() != null){
                        jsonObjectServer.put(result.getDomain(),responsesList.getFirst().getDohData());
                    } else{
                        responsesList.forEach(r->{
                            jsonResponsesArray.add(r.getAsJson(APPLICATION_PROTOCOL.DNS));
                        });
                        jsonObjectServer.put(responsesList.getFirst().getDomain(),jsonResponsesArray);
                    }
                });
                jsonResult.put(result.getNs().getName(),jsonObjectServer);
            });
            copyDataToClipBoard(jsonResult.toJSONString());
            showAlert("dataCopied", Alert.AlertType.INFORMATION);
        });

        //errorContextMenu.getItems().add(errorContextMenuItem);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("average"));
        domainColumn.setCellValueFactory(new PropertyValueFactory<>("domain"));
        maxColumn.setCellValueFactory(new PropertyValueFactory<>("max"));
        minColumn.setCellValueFactory(new PropertyValueFactory<>("min"));
        succColumn.setCellValueFactory(new PropertyValueFactory<>("successful"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("responseSize"));
        failColumn.setCellValueFactory(new PropertyValueFactory<>("failed"));
        dnsserverToggleGroup = new ToggleGroup();

        IPprotToggleGroup = new ToggleGroup();
        IPv4RadioButton.setToggleGroup(IPprotToggleGroup);
        IPv6RadioButton.setToggleGroup(IPprotToggleGroup);

        dnsProtocolToggleGroup = new ToggleGroup();
        dnsDohButton.setToggleGroup(dnsProtocolToggleGroup);
        dnsTcpButton.setToggleGroup(dnsProtocolToggleGroup);
        dnsUdpButton.setToggleGroup(dnsProtocolToggleGroup);
        dnsDotButton.setToggleGroup(dnsProtocolToggleGroup);
        holdConnectionCheckbox.setDisable(true);

        iterativeToggleGroup = new ToggleGroup();
        recursiveQueryRadioButton.setToggleGroup(iterativeToggleGroup);
        iterativeQueryRadioButton.setToggleGroup(iterativeToggleGroup);
        recursiveQueryRadioButton.setSelected(true);

        dnsTcpButton.selectedProperty().addListener((observable, oldValue, newValue) ->
        {
            if (newValue) {
                holdConnectionCheckbox.setDisable(false);
            } else {
                holdConnectionCheckbox.setDisable(true);
            }
        });

        Ip ip = new Ip();
        Config.getNameServers().add(new NameServer("System DNS", "System DNS", ip.getIpv4DnsServers(),
                ip.getIpv6DnsServers()));

        nameServerVBoxes = new LinkedList<>();
        Config.getNameServers().stream().forEach(nameServer -> otherDNSVbox.getChildren()
                .add(new NameServerVBox(nameServer, null, this)));
        enableDnsServers();


        numberOfRequests.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    numberOfRequests.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });

        // if user clicks on DoH protocol button, non DoH servers are disabled
        dnsDohButton.setOnMouseClicked(mouseEvent -> enableDoHServers());
        // if user clicks on DNS over TCP or UDP, non DoH only servers are enabled
        dnsUdpButton.setOnMouseClicked(mouseEvent -> enableDnsServers());
        dnsTcpButton.setOnMouseClicked(mouseEvent -> enableDnsServers());
        dnsDotButton.setOnMouseClicked(mouseEvent -> enableDoTServers());

        LoadTestConfig loadTestConfig = Config.getLoadTestConfig();
        numberOfRequests.setText(loadTestConfig.getDuration());
        IPv4RadioButton.setSelected(loadTestConfig.isIpv4());
        IPv6RadioButton.setSelected(!loadTestConfig.isIpv4());
        if (IPv6RadioButton.isSelected()) {
            reloadDNSServers(false);
        }
        switch (loadTestConfig.getProtocol()) {
            case "UDP": {
                dnsUdpButton.setSelected(true);
                enableDnsServers();
                break;
            }
            case "TCP": {
                dnsTcpButton.setSelected(true);
                this.holdConnectionCheckbox.setSelected(loadTestConfig.isHoldConnection());
                enableDnsServers();
                break;
            }
            case "DOH": {
                dnsDohButton.setSelected(true);
                enableDoHServers();
                break;
            }
            case "DOT": {
                dnsDotButton.setSelected(true);
                enableDoTServers();
                break;
            }
        }
        domainNameTextField.setText(loadTestConfig.getDomain());
        List<String> rrCodes = Arrays.stream(loadTestConfig.getRecord().split(",")).toList();
        checkBoxArray.forEach(box -> {
            Q_COUNT q_count = (Q_COUNT) box.getUserData();
            box.setSelected(rrCodes.contains(q_count.name()));
        });
        recursiveQueryRadioButton.setSelected(loadTestConfig.isRecursive());
        iterativeQueryRadioButton.setSelected(!loadTestConfig.isRecursive());
        DNSSECOkCheckBox.setSelected(loadTestConfig.isDo_());
        checkingDisabledCheckBox.setSelected(loadTestConfig.isCd());
        authenticateDataCheckBox.setSelected(loadTestConfig.isAd());
        cooldownTextField.setText(Integer.toString(Config.getLoadTestConfig().getCooldown()));

        numberOfRequests.textProperty().addListener((observable, oldValue, newValue) -> {
            try{
                if (!numberOfRequests.equals("") && Long.parseLong(newValue) <= 1000000 && Long.parseLong(newValue) > 0) {
                    numberOfRequests.setText(newValue);
                } else {
                    numberOfRequests.setText(oldValue);
                }
            } catch (NumberFormatException e){
                numberOfRequests.setText("1");
            }
        });
        cooldownTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            try{
                if (!cooldownTextField.equals("") && Long.parseLong(newValue) <= 1000000 && Long.parseLong(newValue) >= 10) {
                    cooldownTextField.setText(newValue);
                } else {
                    cooldownTextField.setText(oldValue);
                }
            } catch (NumberFormatException e){
                cooldownTextField.setText("1");
            }
        });

        setLanguageRadioButton();
        // TODO add option for custom DNS server
    }

    public void loadDataFromSettings() {
        savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesLOAD());
    }

    protected void enableDoHServers() {
        otherDNSVbox.getChildren().forEach(node -> {
            NameServerVBox nameServerVBox = (NameServerVBox) node;
            nameServerVBox.setDisable(!nameServerVBox.getNameServer().isDoh());
        });
    }

    protected void enableDoTServers() {
        otherDNSVbox.getChildren().forEach(node -> {
            NameServerVBox nameServerVBox = (NameServerVBox) node;
            nameServerVBox.setDisable(!nameServerVBox.getNameServer().isDot());
        });
    }

    protected void enableDnsServers() {
        otherDNSVbox.getChildren().forEach(node -> {
            NameServerVBox nameServerVBox = (NameServerVBox) node;
            nameServerVBox.setDisable(nameServerVBox.getNameServer().isDotOnly() || nameServerVBox.getNameServer().isDohOnly());
        });
    }

    @Override
    @FXML
    protected void IPv4RadioButtonAction(ActionEvent event) {
        if (IPv4RadioButton.isSelected()) {
            reloadDNSServers(true);
        }
    }

    @Override
    @FXML
    protected void IPv6RadioButtonAction(ActionEvent event) {
        if (IPv6RadioButton.isSelected()) {
            reloadDNSServers(false);
        }
    }

    @Override
    protected void reloadDNSServers(boolean ipv4) {
        // reload DNS servers as usual
        super.reloadDNSServers(ipv4);
        // if DoH is selected then disable servers that do not support DoH
        if (dnsDohButton.isSelected()) {
            for (Node node : otherDNSVbox.getChildren()) {
                if (!(node instanceof NameServerVBox)) {
                    return;
                }
                NameServerVBox nsVBox = (NameServerVBox) node;
                if (!nsVBox.getNameServer().isDoh()) {
                    nsVBox.setDisable(true);
                }
            }
        }
    }

    @Override
    protected void saveDomain(String domain) {
        settings.addLoadDomain(domain);
    }

    protected void setCustomUserDataRecords() {
        cnameCheckBox.setUserData(Q_COUNT.CNAME);
        mxCheckBox.setUserData(Q_COUNT.MX);
        nsCheckBox.setUserData(Q_COUNT.NS);
        caaCheckBox.setUserData(Q_COUNT.CAA);
        dnskeyCheckBox.setUserData(Q_COUNT.DNSKEY);
        soaCheckBox.setUserData(Q_COUNT.SOA);
        dsCheckBox.setUserData(Q_COUNT.DS);
        rrsigCheckBox.setUserData(Q_COUNT.RRSIG);
        nsec3paramCheckBox.setUserData(Q_COUNT.NSEC3PARAM);
        checkBoxArray.add(cnameCheckBox);
        checkBoxArray.add(mxCheckBox);
        checkBoxArray.add(nsCheckBox);
        checkBoxArray.add(caaCheckBox);
        checkBoxArray.add(dnskeyCheckBox);
        checkBoxArray.add(dsCheckBox);
        checkBoxArray.add(rrsigCheckBox);
        checkBoxArray.add(nsec3paramCheckBox);
    }


    @Override
    protected void updateCustomParameters() {
        if (dnsDohButton.isSelected()){
            parameters.put(WiresharkFilter.Parameters.TCPPORT, "443");
            parameters.put(WiresharkFilter.Parameters.UDPPORT, "443");
        } else if (dnsDotButton.isSelected()){
            parameters.put(WiresharkFilter.Parameters.TCPPORT, "853");
            parameters.put(WiresharkFilter.Parameters.UDPPORT, "853");
        } else {
            parameters.put(WiresharkFilter.Parameters.TCPPORT, "53");
            parameters.put(WiresharkFilter.Parameters.UDPPORT, "53");
        }
    }


    @Override
    public String getProtocol() {
        return "Testing";
    }

    @FXML
    protected void sendButtonFired(ActionEvent event){
        super.sendButtonFired(event);
        try {
            // handle stop button action
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                Platform.runLater(() -> {
                    sendButton.setText(getButtonText());
                    resultsTableView.getItems().clear();
                });
                return;
            }
            // prepare data from UI
            String domain = domainNameTextField.getText();

            boolean doFlag = DNSSECOkCheckBox.isSelected();
            boolean adFlag = authenticateDataCheckBox.isSelected();
            boolean cdFlag = checkingDisabledCheckBox.isSelected();
            boolean recursive = recursiveQueryRadioButton.isSelected();
            long cooldown = Integer.parseInt(cooldownTextField.getText());
            long duration = Long.parseLong(numberOfRequests.getText());
            // check user input on number of requests
            duration = Math.min(duration, 1000000);
            duration = Math.max(duration, 1);
            numberOfRequests.setText(Long.toString(duration));
            boolean isIPv4 = IPv4RadioButton.isSelected();
            Q_COUNT[] records = getRecordTypes();
            boolean holdConnection = holdConnectionCheckbox.isSelected();
            observableList = FXCollections.observableList(new LinkedList<>());
            List<List<Result>> results = new LinkedList<>();
            // split domains if necessary
            List<String> domains;
            if (domain.contains(";")) {
                String[] domainsArray = domain.split(";");
                domains = Arrays.asList(domainsArray);
            } else {
                domains = new LinkedList<>();
                domains.add(getDomain(domain));
            }
            for (String dm : domains){
                getDomain(dm);
            }
            saveDomain(domain);
            // collect all servers which user wants to test
            otherDNSVbox.getChildren().filtered(node -> !node.isDisabled()).forEach(node -> {
                NameServerVBox nsVBox = (NameServerVBox) node;
                if (nsVBox.getSelectedIP(isIPv4) != null) {
                    List<Result> aux = new LinkedList<>();
                    domains.forEach(s -> aux.add(new Result(nsVBox.getNameServer(), nsVBox.getSelectedIP(isIPv4), s)));
                    results.add(aux);
                    observableList.addAll(aux);
                }
            });
            if (observableList.isEmpty()) {
                showAlert("ChooseDNSServer");
                return;
            }
            // now decide what user wants to test
            LOGGER.info("Creating main mass testing task");
            TRANSPORT_PROTOCOL transport_protocol = TRANSPORT_PROTOCOL.TCP;
            APPLICATION_PROTOCOL application_protocol = APPLICATION_PROTOCOL.DNS;
            if (dnsUdpButton.isSelected()) {
                transport_protocol = TRANSPORT_PROTOCOL.UDP;
            } else if (dnsDohButton.isSelected()) {
                application_protocol = APPLICATION_PROTOCOL.DOH;
            } else if (dnsDotButton.isSelected()) {
                application_protocol = APPLICATION_PROTOCOL.DOT;
            }
            // start progress bar and start tester task
            Platform.runLater(()->progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));
            RequestSettings rs = new RequestSettings.RequestSettingsBuilder()
                    .recursion(recursive)
                    .adFlag(adFlag)
                    .cdFlag(cdFlag)
                    .doFlag(doFlag)
                    .domain(domain)
                    .types(records)
                    .build();
            ConnectionSettings cs = new ConnectionSettings.ConnectionSettingsBuilder()
                    .holdConnection(holdConnection)
                    .transport_protocol(transport_protocol)
                    .application_protocol(application_protocol)
                    .netInterface(getInterface())
                    .build();
            Task<Void> task = new TesterTask(rs, cs, (int)duration, results, cooldown, this);
            // Task<Void> task = new TesterTask(recursive, adFlag, cdFlag, doFlag, holdConnection, domain, records, transport_protocol, application_protocol, getInterface(), (int)duration, results, cooldown, this);
            //DNSTaskBase dnsTask = new TcpTester(false,adFlag,cdFlag,doFlag,holdConnection,domain,records, TRANSPORT_PROTOCOL.TCP, APPLICATION_PROTOCOL.DNS,"")
            resultsTableView.setItems(observableList);
            thread = new Thread(task);
            LOGGER.info("Starting main mass testing task");
            thread.start();
            buttonText = sendButton.getText();
            // turn send button to stop buttor
            Platform.runLater(() -> sendButton.setText("Stop"));
        } catch (NonRecordSelectedException | UnsupportedEncodingException | MoreRecordsTypesWithPTRException | NotValidDomainNameException e) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            //String fullClassName = e.getClass().getSimpleName();
            //LOGGER.info(fullClassName);
            //e.printStackTrace();
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
    * Marks all servers to be tested
    * */
    @FXML
    private void testAllFired(ActionEvent event) {
        otherDNSVbox.getChildren().filtered(node -> !node.isDisabled()).forEach(node -> {
            NameServerVBox nameServerVBox = (NameServerVBox) node;
            if (IPv4RadioButton.isSelected()) {
                nameServerVBox.getIPv4radioButton().setSelected(true);
                if (nameServerVBox.getSelectedIP(IPv4RadioButton.isSelected()) == null) {
                    if (nameServerVBox.getIPv4ToggleButtonsHBox() != null) {
                        ((IPToggleButton) nameServerVBox.getIPv4ToggleButtonsHBox().getChildren().getFirst()).setSelected(true);
                    } else {
                        nameServerVBox.getIPv4radioButton().setSelected(true);
                    }
                }
            } else {
                nameServerVBox.getIPv6radioButton().setSelected(true);
                if (nameServerVBox.getSelectedIP(IPv4RadioButton.isSelected()) == null) {
                    if (nameServerVBox.getIPv6ToggleButtonsHBox() != null) {
                        ((IPToggleButton) nameServerVBox.getIPv6ToggleButtonsHBox().getChildren().getFirst()).setSelected(true);
                    } else {
                        nameServerVBox.getIPv6radioButton().setSelected(true);
                    }
                }
            }

        });
    }

    /*
     *
     * + and - buttons
     */
    @FXML
    private void addButton(ActionEvent event) {
        int duration = Integer.parseInt(numberOfRequests.getText());
        numberOfRequests.setText(Integer.toString(duration + 1));
    }
    /*
     *
     * + and - buttons
     */
    @FXML
    private void subButton(ActionEvent event) {
        int duration = Integer.parseInt(numberOfRequests.getText());
        if (duration > 1) {
            numberOfRequests.setText(Integer.toString(duration - 1));
        }
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    protected String getDomain(String domain) throws NotValidDomainNameException, UnsupportedEncodingException {
        try {
            LOGGER.info("Domain name: " + domain);
            if (domain.equals(".")) {
                return domain;
            }
            if (domain.isEmpty()) {
                throw new NotValidDomainNameException();
            }

            if ((domain.contains(".arpa")) && ptrCheckBox.isSelected()) {
                return domain;
            }
            if ((Ip.isIPv4Address(domain) || Ip.isIpv6Address(domain)) && ptrCheckBox.isSelected()) {
                LOGGER.info("PTR record request");
                return Ip.getIpReversed(domain);
            }
            if (DomainConvert.isValidDomainName(domain)) {
                return DomainConvert.encodeIDN(domain);
            } else {
                throw new NotValidDomainNameException();
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
            throw new NotValidDomainNameException();
        }
    }

    /*
    * erase saved domain names
    * */
    @FXML
    private void deleteDomainNameHistoryFired(Event event) {
        settings.eraseLOADDomainNames();
        savedDomainNamesChoiseBox.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
    }

}
