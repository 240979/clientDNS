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
import javafx.scene.control.*;
import models.ConnectionSettings;
import models.NameServer;
import models.RequestSettings;
import models.WiresharkFilter;
import tasks.DNSOverQUICTask;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

public class DoQController extends GeneralController{
    public static final String FXML_FILE_NAME = "/fxml/DoQ_small.fxml";
    private int resolverPort;
    public DoQController(){
        super();
        PROTOCOL = "DNS over QUIC";
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

        Config.getNameServers().stream().filter(NameServer::isDoq).forEach(nameServer -> otherDNSVbox.getChildren()
                .add(new NameServerVBox(nameServer, dnsserverToggleGroup, this)));
    }

    @Override
    protected void updateCustomParameters() {
        parameters.put(WiresharkFilter.Parameters.UDPPORT, Integer.toString(resolverPort));
    }

    @Override
    public String getProtocol() {
        return "DoQ";
    }

    @Override
    protected void saveDomain(String domain) {
        settings.addDNSDomain(domain);
    }
    @FXML
    private void deleteDomainNameHistoryFired(Event event) {
        settings.eraseDomainNames();
        savedDomainNamesChoiseBox.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
    }
    @FXML
    protected void sendButtonFired(ActionEvent event) {
        super.sendButtonFired(event);
        if(isTerminatingThread())
            return;
        try{

            String dnsServIp = getDnsServerIp();  // Throws: DnsServerIpIsNotValidException | UnknownHostException | NoIpAddrForDomainName | NotValidDomainNameException
            if (dnsServIp == null || dnsServIp.isBlank()) {  // If no DNS server was chosen by radiobutton, then do not bother running
                Platform.runLater(()->sendButton.setText(getButtonText()));
                return;
            }
            int dnsServPort = getDnsServerPort(dnsServIp);
            this.resolverPort = dnsServPort;
            LOGGER.info(dnsServIp + ":" + dnsServPort);
            Q_COUNT[] records = getRecordTypes(); // Throws: exceptions.MoreRecordsTypesWithPTRException, exceptions.NonRecordSelectedException
            TRANSPORT_PROTOCOL transport = TRANSPORT_PROTOCOL.UDP;
            String domain = getDomain(); // Throws: java.io.UnsupportedEncodingException
            boolean caFlag = checkingDisabledCheckBox.isSelected();
            boolean adFlag = authenticateDataCheckBox.isSelected();
            boolean doFlag = DNSSECOkCheckBox.isSelected();
            boolean recursive = recursiveQueryRadioButton.isSelected();
            LOGGER.info("DNS server: " + dnsServIp + "\n" + "Domain: " + domain + "\n" + "Records: " + Arrays.toString(records)
                    + "\n" + "\n" + "DNSSEC: " + adFlag + "\n" + "DNSSEC sig records"
                    + doFlag + "\n" + "Transport protocol: " + transport + "\n"
                    + "\n" + "Application protocol: " + APPLICATION_PROTOCOL.DOQ + "\n" + "Checking disabled: " + caFlag);
            RequestSettings rs = new RequestSettings.RequestSettingsBuilder()
                    .recursion(recursive)
                    .adFlag(adFlag)
                    .cdFlag(caFlag)
                    .doFlag(doFlag)
                    .domain(domain)
                    .types(records)
                    .build();
            ConnectionSettings cs = new ConnectionSettings.ConnectionSettingsBuilder()
                    .transport_protocol(TRANSPORT_PROTOCOL.UDP)
                    .application_protocol(APPLICATION_PROTOCOL.DOQ)
                    .resolverIP(dnsServIp)
                    .resolverPort(dnsServPort)
                    .netInterface(getInterface())
                    .isDomainNameUsed(isDomainNameUsed())
                    .resolverUri(getDnsServerDomainName(getDnsServerIp()))
                    .build();
            task = new DNSOverQUICTask(rs, cs);
            task.setController(this);
            numberOfMessagesValueLabel.textProperty().bind(task.messagesSentPropertyProperty().asString());
            responseTimeValueLabel.textProperty().bind(task.durationPropertyProperty().asString());
            requestTreeView.rootProperty().bind(task.requestPropertyProperty());
            responseTreeView.rootProperty().bind(task.responsePropertyProperty());
            querySizeLabel.textProperty().bind(task.querySizeProperty().asString());
            responseSizeLabel.textProperty().bind(task.responseSizeProperty().asString());
            thread = new Thread(task);
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            task.setProgressBar(progressBar);
            task.setMassTesting(false);
            thread.start();
        }catch (DnsServerIpIsNotValidException
                | UnknownHostException
                | NoIpAddrForDomainName
                | NotValidDomainNameException
                | MoreRecordsTypesWithPTRException
                | NonRecordSelectedException
                | UnsupportedEncodingException
                | CancellationException
                | NotValidIPException e ){
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            showAlert(e);
        }

    }
    public void loadDataFromSettings() {
        savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesDNS());
    }

}
