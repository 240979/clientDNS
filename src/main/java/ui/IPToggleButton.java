package ui;
/*
 * Author - Patricia Ramosova
 * Link - https://github.com/xramos00/DNS_client
 * */
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPToggleButton extends ToggleButton {

    private RadioButton controlButton;

    public IPToggleButton(RadioButton controlButton)
    {
        super();
        this.controlButton = controlButton;
    }

}
