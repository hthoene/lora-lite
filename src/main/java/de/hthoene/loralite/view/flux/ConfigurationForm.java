package de.hthoene.loralite.view.flux;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import de.hthoene.loralite.aitoolkit.JobConfiguration;
import de.hthoene.loralite.template.TemplateService;
import de.hthoene.loralite.util.ApplicationInformation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ConfigurationForm extends VerticalLayout {

    private final ComboBox<String> templateSelect = new ComboBox<>("Template");
    private final IntegerField resolutionXField = new IntegerField("Resolution Width");
    private final IntegerField resolutionYField = new IntegerField("Resolution Height");
    private final IntegerField stepsField = new IntegerField("Steps");
    private final IntegerField rankField = new IntegerField("Rank");
    private final NumberField learningRateField = new NumberField("Learning Rate");
    private final TextArea samplePromptsField = new TextArea("Sample Prompts (one per line)");

    private final TemplateService templateService;
    private final UiSettingsService settingsService;
    private final UiSettings settings;

    private JobConfiguration currentTemplate;

    public ConfigurationForm(TemplateService templateService,
                             UiSettingsService settingsService) {
        this.templateService = templateService;
        this.settingsService = settingsService;
        this.settings = settingsService.load();

        initializeTemplateSelect();
        initializeFields();
        initializeSettingsFromTemplateIfEmpty();
        applySettingsToFields();
        registerSettingsUpdateListeners();

        FormLayout formLayout = new FormLayout(
                templateSelect,
                resolutionXField,
                resolutionYField,
                stepsField,
                rankField,
                learningRateField,
                samplePromptsField
        );
        formLayout.setWidthFull();
        formLayout.setColspan(samplePromptsField, 3);

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("900px", 3)
        );

        add(formLayout);
        setWidthFull();
    }

    private void initializeTemplateSelect() {
        List<String> templates = templateService.listTemplateNames();
        templateSelect.setItems(templates);

        String initialTemplate = settings.getTemplateName();
        if (initialTemplate != null && !initialTemplate.isBlank() && templates.contains(initialTemplate)) {
            templateSelect.setValue(initialTemplate);
        } else if (!templates.isEmpty()) {
            templateSelect.setValue(templates.get(0));
        }

        if (templateSelect.getValue() != null) {
            currentTemplate = templateService.loadTemplate(templateSelect.getValue());
        }

        templateSelect.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                return;
            }

            currentTemplate = templateService.loadTemplate(event.getValue());
            settings.setTemplateName(event.getValue());

            applyTemplateDefaultsToSettings(currentTemplate);
            settingsService.save(settings);

            applySettingsToFields();
        });
    }

    private void initializeFields() {
        resolutionXField.setMin(128);
        resolutionXField.setMax(2048);
        resolutionXField.setStep(64);

        resolutionYField.setMin(128);
        resolutionYField.setMax(2048);
        resolutionYField.setStep(64);

        stepsField.setMin(100);

        rankField.setMin(1);
        rankField.setMax(64);

        learningRateField.setMin(1e-6);
        learningRateField.setMax(1e-3);

        samplePromptsField.setPlaceholder("photo of <token>\nphoto of <token> in a garden\n...");
        samplePromptsField.setWidthFull();
        samplePromptsField.setHeight("140px");
    }

    private void initializeSettingsFromTemplateIfEmpty() {
        boolean hasAnySetting =
                settings.getResolutionX() != null ||
                        settings.getResolutionY() != null ||
                        settings.getSteps() != null ||
                        settings.getRank() != null ||
                        settings.getLearningRate() != null ||
                        (settings.getSamplePrompts() != null && !settings.getSamplePrompts().isBlank());

        if (!hasAnySetting && currentTemplate != null) {
            applyTemplateDefaultsToSettings(currentTemplate);
            settingsService.save(settings);
        }
    }

    private void applyTemplateDefaultsToSettings(JobConfiguration template) {
        if (template == null
                || template.getConfig() == null
                || template.getConfig().getProcess() == null
                || template.getConfig().getProcess().isEmpty()) {
            return;
        }

        JobConfiguration.ProcessItem processItem = template.getConfig().getProcess().get(0);
        JobConfiguration.Train train = processItem.getTrain();
        JobConfiguration.Sample sample = processItem.getSample();
        JobConfiguration.Dataset dataset = processItem.getDatasets() != null
                && !processItem.getDatasets().isEmpty()
                ? processItem.getDatasets().getFirst()
                : null;
        JobConfiguration.Network network = processItem.getNetwork();

        if (train != null && train.getSteps() != null) {
            settings.setSteps(train.getSteps());
        }
        if (network != null && network.getLinear() != null) {
            settings.setRank(network.getLinear());
        }
        if (train != null && train.getLr() != null) {
            settings.setLearningRate(train.getLr());
        }

        if (dataset != null
                && dataset.getResolution() != null
                && dataset.getResolution().size() == 2) {
            settings.setResolutionX(dataset.getResolution().get(0));
            settings.setResolutionY(dataset.getResolution().get(1));
        }

        if (sample != null && sample.getPrompts() != null && !sample.getPrompts().isEmpty()) {
            String prompts = String.join("\n", sample.getPrompts());
            settings.setSamplePrompts(prompts);
        }
    }

    private void applySettingsToFields() {
        resolutionXField.setValue(
                settings.getResolutionX() != null ? settings.getResolutionX() : 512);
        resolutionYField.setValue(
                settings.getResolutionY() != null ? settings.getResolutionY() : 512);
        stepsField.setValue(
                settings.getSteps() != null ? settings.getSteps() : 2000);
        rankField.setValue(
                settings.getRank() != null ? settings.getRank() : 8);
        learningRateField.setValue(
                settings.getLearningRate() != null ? settings.getLearningRate() : 5e-5);

        String savedPrompts = settings.getSamplePrompts();
        samplePromptsField.setValue(
                savedPrompts != null && !savedPrompts.isBlank()
                        ? savedPrompts
                        : "photo of <token>"
        );
    }

    private void registerSettingsUpdateListeners() {
        resolutionXField.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                settings.setResolutionX(event.getValue());
                settingsService.save(settings);
            }
        });
        resolutionYField.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                settings.setResolutionY(event.getValue());
                settingsService.save(settings);
            }
        });
        stepsField.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                settings.setSteps(event.getValue());
                settingsService.save(settings);
            }
        });
        rankField.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                settings.setRank(event.getValue());
                settingsService.save(settings);
            }
        });
        learningRateField.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                settings.setLearningRate(event.getValue());
                settingsService.save(settings);
            }
        });
        samplePromptsField.addValueChangeListener(event -> {
            settings.setSamplePrompts(event.getValue());
            settingsService.save(settings);
        });
    }

    public JobConfiguration buildConfig() {
        if (currentTemplate == null && templateSelect.getValue() != null) {
            currentTemplate = templateService.loadTemplate(templateSelect.getValue());
        }

        JobConfiguration template = currentTemplate;
        JobConfiguration.Config cfg = template.getConfig();
        JobConfiguration.ProcessItem base = cfg.getProcess().getFirst();

        List<String> prompts = Arrays.stream(
                        Optional.ofNullable(samplePromptsField.getValue()).orElse("").split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (prompts.isEmpty()) {
            prompts = List.of("photo of <token>");
        }

        JobConfiguration.Network network = base.getNetwork().toBuilder()
                .linear(rankField.getValue())
                .linearAlpha(rankField.getValue())
                .build();

        JobConfiguration.Train train = base.getTrain().toBuilder()
                .steps(stepsField.getValue())
                .lr(learningRateField.getValue())
                .build();

        JobConfiguration.Sample sample = base.getSample().toBuilder()
                .width(resolutionXField.getValue())
                .height(resolutionYField.getValue())
                .prompts(prompts)
                .build();

        JobConfiguration.Dataset dataset = base.getDatasets().getFirst().toBuilder()
                .resolution(List.of(resolutionXField.getValue(), resolutionYField.getValue()))
                .build();

        JobConfiguration.ProcessItem process = base.toBuilder()
                .network(network)
                .train(train)
                .sample(sample)
                .datasets(List.of(dataset))
                .build();

        JobConfiguration.Config newCfg = cfg.toBuilder()
                .process(List.of(process))
                .name("latest")
                .build();

        JobConfiguration.Meta meta = template.getMeta().toBuilder()
                .name("LoRA-Lite")
                .version(ApplicationInformation.VERSION)
                .build();

        return template.toBuilder()
                .config(newCfg)
                .meta(meta)
                .build();
    }
}
