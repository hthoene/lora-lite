package de.hthoene.loralite;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Theme(value = "loralite", variant = Lumo.DARK)
@Push(PushMode.AUTOMATIC)
@SpringBootApplication
public class LoRaLiteApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(LoRaLiteApplication.class, args);
    }

}
