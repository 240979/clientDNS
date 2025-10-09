package ui;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * Methods used from Martin Biolek thesis are marked with comment
 * */
import enums.*;
import exceptions.MoreRecordsTypesWithPTRException;
import exceptions.NonRecordSelectedException;
import exceptions.NotValidDomainNameException;
import exceptions.NotValidIPException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleGroup;
import models.DomainConvert;
import models.Ip;
import models.WiresharkFilter;
import tasks.DNSOverLinkLocal;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

public class LLMNRController extends GeneralController {
    public static final String FXML_FILE_NAME = "/fxml/LLMNR_small.fxml";

    @FXML
    protected CheckBox srvCheckBox;

    public void initialize() {

        super.initialize();
        IPprotToggleGroup = new ToggleGroup();

        IPv4RadioButton.setToggleGroup(IPprotToggleGroup);
        IPv6RadioButton.setToggleGroup(IPprotToggleGroup);
        setLanguageRadioButton();

    }

    protected void setCustomUserDataRecords() {
        srvCheckBox.setUserData(Q_COUNT.SRV);
    }

    @Override
    protected void updateCustomParameters() {
        parameters.put(WiresharkFilter.Parameters.TCPPORT,"5355");
        parameters.put(WiresharkFilter.Parameters.UDPPORT,"5355");
    }


    @FXML
    protected void sendButtonFired(ActionEvent e) {
        super.sendButtonFired(e);
        if (isTerminatingThread()){
            return;
        }
        try{Q_COUNT records[] = getRecordTypes();
        String domain = getDomain();
        boolean caFlag = checkingDisabledCheckBox.isSelected();
        boolean adFlag = authenticateDataCheckBox.isSelected();
        boolean doFlag = DNSSECOkCheckBox.isSelected();
        //logAction(records, domain, doFlag, networkProtocol, mdnsType);

        task = new DNSOverLinkLocal(false, adFlag, caFlag, doFlag, domain,
                records, TRANSPORT_PROTOCOL.UDP, APPLICATION_PROTOCOL.MDNS, getDnsServerIp(), getInterface());
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
        thread.start();
        } catch (NotValidDomainNameException |MoreRecordsTypesWithPTRException  | UnsupportedEncodingException | NotValidIPException | UnknownHostException | NonRecordSelectedException ex){
            //ex.printStackTrace();
            //showAlert(ex.getClass().getSimpleName());
            showAlert(ex);
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
        } catch (Exception exx) {
            Platform.runLater(()->{
                sendButton.setText(getButtonText());
                progressBar.setProgress(0);
            });
            // LOGGER.warning(exx.toString());
            // showAlert("Exception");
            showAlert(exx);
        }
    }

    public void loadDataFromSettings() {
        savedDomainNamesChoiseBox.getItems().addAll(settings.getDomainNamesLLMNR());
    }

    /*
     * Body of method taken from Martin Biolek thesis
     * */
    protected String getDomain() throws UnsupportedEncodingException, NotValidDomainNameException {
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
        return domain;
    }

    @Override
    protected void saveDomain(String domain) {
        settings.addLLMNRDomain(domain);
    }

    @Override
    public String getProtocol() {
        return "LLMNR";
    }

    @Override
    protected String getDnsServerIp() {
        if (IPv4RadioButton.isSelected()) {
            return "224.0.0.252";
        } else {
            return "FF02:0:0:0:0:0:1:3";
        }
    }

    @FXML
    private void deleteDomainNameHistoryFired(Event event) {
        settings.eraseLLMNRDomainNames();
        savedDomainNamesChoiseBox.getItems().removeAll(savedDomainNamesChoiseBox.getItems());
    }
}
