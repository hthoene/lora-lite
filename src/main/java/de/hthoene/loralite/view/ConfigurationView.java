package de.hthoene.loralite.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.hthoene.loralite.aitoolkit.AiToolkitService;
import de.hthoene.loralite.aitoolkit.JobConfiguration;
import de.hthoene.loralite.component.LogPanel;
import de.hthoene.loralite.template.TemplateService;
import de.hthoene.loralite.view.flux.ConfigurationForm;
import de.hthoene.loralite.view.flux.UiSettingsService;

import java.io.IOException;

public class ConfigurationView extends VerticalLayout {

    public ConfigurationView(TemplateService templateService,
                             UiSettingsService uiSettingsService,
                             AiToolkitService aiToolkitService,
                             LogPanel logPanel) {

        setAlignItems(Alignment.CENTER);
        setWidthFull();

        ConfigurationForm configurationForm =
                new ConfigurationForm(templateService, uiSettingsService);

        Button startButton = new Button("Start Training", _ -> {
            try {
                JobConfiguration config = configurationForm.buildConfig();
                logPanel.log("Starting training: " + config.getConfig().getName());
                aiToolkitService.train(config);
                Notification notification = Notification.show("Training started");
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IOException e) {
                logPanel.log(e);
                Notification notification = Notification.show("Error starting training: " + e.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(configurationForm, startButton);
    }
}
