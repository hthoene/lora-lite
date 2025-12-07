package de.hthoene.loralite.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import de.hthoene.loralite.aitoolkit.AiToolkitService;
import de.hthoene.loralite.component.DefaultFooter;
import de.hthoene.loralite.component.LogPanel;
import de.hthoene.loralite.template.TemplateService;
import de.hthoene.loralite.util.ArchiveService;
import de.hthoene.loralite.util.GpuMonitor;
import de.hthoene.loralite.util.WorkspaceProperties;
import de.hthoene.loralite.view.flux.UiSettingsService;
import jakarta.annotation.PreDestroy;
import org.springframework.core.env.Environment;

import java.io.IOException;

@Route("")
public class MainView extends VerticalLayout {
    private final UiSettingsService uiSettingsService;
    private final AiToolkitService aiToolkitService;
    private final ArchiveService archiveService;
    private final Environment environment;
    private final TemplateService templateService;

    private final LogPanel logPanel;
    private final ProcessView processPage;

    private final VerticalLayout primaryLayout = new VerticalLayout();
    private final UI ui;

    public MainView(AiToolkitService aiToolkitService,
                    Environment environment,
                    UiSettingsService uiSettingsService,
                    GpuMonitor gpuMonitor,
                    ArchiveService archiveService,
                    TemplateService templateService,
                    WorkspaceProperties workspaceProperties
                    ) throws IOException {

        this.aiToolkitService = aiToolkitService;
        this.environment = environment;
        this.uiSettingsService = uiSettingsService;
        this.archiveService = archiveService;
        this.templateService = templateService;

        this.ui = UI.getCurrent();

        setSizeFull();

        this.logPanel = new LogPanel(workspaceProperties);
        LogView logView = new LogView(logPanel);
        logView.setAlignItems(Alignment.STRETCH);

        this.processPage = new ProcessView(gpuMonitor, logPanel, workspaceProperties, aiToolkitService);

        SplitLayout splitLayout = new SplitLayout(primaryLayout, logView);
        splitLayout.setSplitterPosition(60);
        splitLayout.setSizeFull();

        primaryLayout.setPadding(false);
        primaryLayout.setAlignItems(Alignment.CENTER);

        createTabs(primaryLayout);
        primaryLayout.add(new DefaultFooter());

        add(splitLayout);

        if (ui != null) {
            ui.setPollInterval(2000);
            ui.addPollListener(event -> processPage.refresh());
        }
    }

    @PreDestroy
    private void cleanup() {
        if (ui != null) {
            ui.setPollInterval(-1);
        }
    }

    private void createTabs(FlexComponent layout) {
        HorizontalLayout tabsAndButton = new HorizontalLayout();
        tabsAndButton.setAlignItems(Alignment.CENTER);

        Tabs tabs = new Tabs();
        Tab datasetTab = new Tab("Dataset");
        Tab configTab = new Tab("Configuration");
        Tab processTab = new Tab("Process");
        tabs.add(datasetTab, configTab, processTab);

        final DatasetView datasetPage = new DatasetView(logPanel, environment);

        Button archiveButton = new Button("Archive Workflow");
        archiveButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        archiveButton.addClickListener(event -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Archive Current Workflow?");
            dialog.setText(
                    "This will archive all current data (datasets, configs, output, logs) and prepare for a new workflow. Are you sure?");
            dialog.setCancelable(true);
            dialog.setConfirmText("Archive");
            dialog.setConfirmButtonTheme("error primary");

            dialog.addConfirmListener(confirmEvent -> {
                try {
                    archiveService.archiveAllLatestFolders();
                    logPanel.clearUi();
                    Notification notification =
                            Notification.show("Workflow successfully archived! You can start a new workflow now.");
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.setDuration(5000);
                    datasetPage.updateDatasetPresentation();
                } catch (IOException e) {
                    Notification notification =
                            Notification.show("Error archiving workflow: " + e.getMessage());
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.setDuration(5000);
                    logPanel.log("Error archiving workflow: " + e.getMessage());
                }
            });

            dialog.open();
        });

        tabsAndButton.add(tabs, archiveButton);

        Div pages = new Div();
        pages.setWidthFull();
        pages.getStyle().set("flex", "1");
        pages.setMaxWidth("80em");

        VerticalLayout configPage =
                new ConfigurationView(templateService, uiSettingsService, aiToolkitService, logPanel);

        pages.add(datasetPage, configPage, processPage);

        datasetPage.setVisible(true);
        configPage.setVisible(false);
        processPage.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            datasetPage.setVisible(event.getSelectedTab() == datasetTab);
            configPage.setVisible(event.getSelectedTab() == configTab);
            processPage.setVisible(event.getSelectedTab() == processTab);
        });

        layout.add(tabsAndButton, pages);
    }
}
