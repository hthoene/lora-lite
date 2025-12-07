package de.hthoene.loralite.view.flux;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.hthoene.loralite.aitoolkit.JobConfiguration;
import de.hthoene.loralite.template.TemplateService;
import de.hthoene.loralite.util.ApplicationInformation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ConfigurationForm extends VerticalLayout {

    private static final String DEFAULT_PROMPT = "photo of <token>";
    private static final String DEFAULT_TRIGGER_WORD = "TOK";
    private static final String CONFIG_NAME = "latest";
    private static final String META_NAME = "LoRA-Lite";

    private final ComboBox<String> templateSelect = new ComboBox<>("Template");

    private final IntegerField resolutionXField = new IntegerField("Resolution Width");
    private final IntegerField resolutionYField = new IntegerField("Resolution Height");
    private final IntegerField stepsField = new IntegerField("Steps");
    private final IntegerField rankField = new IntegerField("Rank");
    private final IntegerField batchSizeField = new IntegerField("Batch");
    private final NumberField learningRateField = new NumberField("Learning Rate");
    private final TextField triggerWordField = new TextField("Trigger Word");

    private final Checkbox gradientCheckpointingField = new Checkbox("Gradient Checkpointing");
    private final ComboBox<JobConfiguration.Optimizer> optimizerField = new ComboBox<>("Optimizer");
    private final ComboBox<JobConfiguration.DType> dtypeField = new ComboBox<>("Precision (dtype)");
    private final Checkbox lowVramField = new Checkbox("Low VRAM Mode");
    private final Checkbox quantizeField = new Checkbox("Quantize Model");

    private final IntegerField sampleEveryNStepsField = new IntegerField("Sample Every N Steps");
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

        setWidthFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("gap", "0");

        initializeTemplateSelect();
        initializeFields();
        initializeTooltips();
        initializeSettingsFromTemplateIfEmpty();
        applySettingsToFields();
        registerSettingsUpdateListeners();

        VerticalLayout templateGroup = createGroup(null, createSimpleForm(templateSelect));

        FormLayout imageAndTrainingLayout = new FormLayout(
                resolutionXField,
                resolutionYField,
                stepsField,
                rankField,
                batchSizeField,
                learningRateField,
                triggerWordField
        );
        imageAndTrainingLayout.setWidthFull();
        imageAndTrainingLayout.getStyle().set("margin", "0").set("padding", "0");
        imageAndTrainingLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("900px", 3)
        );
        VerticalLayout imageAndTrainingGroup = createGroup("Image & Training", imageAndTrainingLayout);

        FormLayout performanceLayout = new FormLayout(
                optimizerField,
                dtypeField,
                gradientCheckpointingField,
                lowVramField,
                quantizeField
        );
        performanceLayout.setWidthFull();
        performanceLayout.getStyle().set("margin", "0").set("padding", "0");
        performanceLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("900px", 3)
        );
        VerticalLayout performanceGroup = createGroup("Performance & VRAM", performanceLayout);

        FormLayout samplingLayout = new FormLayout(
                sampleEveryNStepsField,
                samplePromptsField
        );
        samplingLayout.setWidthFull();
        samplingLayout.setColspan(samplePromptsField, 2);
        samplingLayout.getStyle().set("margin", "0").set("padding", "0");
        samplingLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        VerticalLayout samplingGroup = createGroup("Sampling", samplingLayout);
        samplingGroup.getStyle().set("margin-bottom", "0");

        add(templateGroup, imageAndTrainingGroup, performanceGroup, samplingGroup);
    }

    private VerticalLayout createGroup(String title, Component content) {
        VerticalLayout group = new VerticalLayout();
        group.setPadding(true);
        group.setSpacing(false);
        group.setMargin(false);
        group.setWidthFull();
        group.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0.75rem 1rem")
                .set("margin-bottom", "1rem");

        if (title != null) {
            Span label = new Span(title);
            label.getStyle()
                    .set("font-weight", "600")
                    .set("font-size", "var(--lumo-font-size-m)")
                    .set("margin-bottom", "0.25rem");
            group.add(label);
        }

        group.add(content);
        return group;
    }

    private FormLayout createSimpleForm(Component... components) {
        FormLayout layout = new FormLayout(components);
        layout.setWidthFull();
        layout.getStyle().set("margin", "0").set("padding", "0");
        return layout;
    }

    private void initializeTemplateSelect() {
        List<String> templates = templateService.listTemplateNames();
        templateSelect.setItems(templates);

        String initialTemplate = settings.getTemplateName();
        if (initialTemplate != null && !initialTemplate.isBlank() && templates.contains(initialTemplate)) {
            templateSelect.setValue(initialTemplate);
        } else if (!templates.isEmpty()) {
            templateSelect.setValue(templates.getFirst());
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

        batchSizeField.setMin(1);
        batchSizeField.setMax(4);

        learningRateField.setMin(1e-6);
        learningRateField.setMax(1e-3);

        sampleEveryNStepsField.setMin(10);
        sampleEveryNStepsField.setMax(10_000);

        samplePromptsField.setPlaceholder("photo of <token>\nphoto of <token> in a garden\n...");
        samplePromptsField.setWidthFull();
        samplePromptsField.setHeight("140px");

        optimizerField.setItems(JobConfiguration.Optimizer.values());
        dtypeField.setItems(JobConfiguration.DType.values());
    }

    private void initializeTooltips() {
        resolutionXField.setTitle("Image width in pixels. Higher values improve detail but use more VRAM and slow down training.");
        resolutionYField.setTitle("Image height in pixels. Higher values improve detail but use more VRAM and slow down training.");
        stepsField.setTitle("Number of training steps. More steps can improve quality but increase training time.");
        rankField.setTitle("LoRA rank. Higher values can capture more detail but use slightly more VRAM and time.");
        batchSizeField.setTitle("Number of images processed at once. Higher batch uses more VRAM but can stabilize training.");
        learningRateField.setTitle("Speed of learning. Higher values train faster but may be unstable; lower values are safer but slower.");
        triggerWordField.setTitle(
                "Unique keyword used in captions and prompts to activate this LoRA. " +
                        "Use the same token (e.g. <token>) in your training captions and sample prompts."
        );

        gradientCheckpointingField.getElement()
                .setProperty("title", "Saves VRAM by recomputing activations. Reduces VRAM usage but makes training slower.");
        optimizerField.getElement()
                .setProperty("title", "Optimization algorithm. 8-bit or memory-efficient optimizers reduce VRAM usage with minor speed/behavior changes.");
        dtypeField.getElement()
                .setProperty("title", "Precision used for training. Lower precision (float16/bf16) saves VRAM and can be faster, with slight loss in numerical accuracy.");
        lowVramField.getElement()
                .setProperty("title", "Enables additional memory-saving tricks. Uses less VRAM at the cost of some speed.");
        quantizeField.getElement()
                .setProperty("title", "Quantizes model weights to lower precision. Greatly reduces VRAM usage with potential small quality or stability impact.");

        sampleEveryNStepsField.setTitle("How often to generate preview samples. Lower values show more previews but slow training slightly.");
        samplePromptsField.setTitle("Prompts used for preview samples during training. Affects only sampling time, not VRAM usage much.");
    }

    private void initializeSettingsFromTemplateIfEmpty() {
        if (!hasAnySettings(settings) && currentTemplate != null) {
            applyTemplateDefaultsToSettings(currentTemplate);
            settingsService.save(settings);
        }
    }

    private boolean hasAnySettings(UiSettings settings) {
        return settings.getResolutionX() != null ||
                settings.getResolutionY() != null ||
                settings.getSteps() != null ||
                settings.getRank() != null ||
                settings.getBatchSize() != null ||
                settings.getLearningRate() != null ||
                settings.getSampleEveryNSteps() != null ||
                (settings.getSamplePrompts() != null && !settings.getSamplePrompts().isBlank()) ||
                settings.getGradientCheckpointing() != null ||
                settings.getOptimizer() != null ||
                settings.getDtype() != null ||
                settings.getLowVram() != null ||
                settings.getQuantize() != null ||
                settings.getTriggerWord() != null;
    }

    private void applyTemplateDefaultsToSettings(JobConfiguration template) {
        if (template == null
                || template.getConfig() == null
                || template.getConfig().getProcess() == null
                || template.getConfig().getProcess().isEmpty()) {
            return;
        }

        JobConfiguration.ProcessItem processItem = template.getConfig().getProcess().getFirst();
        JobConfiguration.Train train = processItem.getTrain();
        JobConfiguration.Sample sample = processItem.getSample();
        JobConfiguration.Dataset dataset = processItem.getDatasets() != null
                && !processItem.getDatasets().isEmpty()
                ? processItem.getDatasets().getFirst()
                : null;
        JobConfiguration.Network network = processItem.getNetwork();
        JobConfiguration.Model model = processItem.getModel();

        if (train != null && train.getSteps() != null) {
            settings.setSteps(train.getSteps());
        }
        if (network != null && network.getLinear() != null) {
            settings.setRank(network.getLinear());
        }
        if (train != null && train.getLr() != null) {
            settings.setLearningRate(train.getLr());
            settings.setBatchSize(train.getBatchSize());
        }

        if (dataset != null
                && dataset.getResolution() != null
                && dataset.getResolution().size() == 2) {
            settings.setResolutionX(dataset.getResolution().get(0));
            settings.setResolutionY(dataset.getResolution().get(1));
        }

        if (sample != null && sample.getPrompts() != null && !sample.getPrompts().isEmpty()) {
            String prompts = String.join("\n", sample.getPrompts());
            settings.setSampleEveryNSteps(sample.getSampleEvery());
            settings.setSamplePrompts(prompts);
        }

        if (train != null && train.getGradientCheckpointing() != null) {
            settings.setGradientCheckpointing(train.getGradientCheckpointing());
        }
        if (train != null && train.getOptimizer() != null) {
            settings.setOptimizer(train.getOptimizer());
        }
        if (train != null && train.getDtype() != null) {
            settings.setDtype(train.getDtype());
        }
        if (model != null && model.getLowVram() != null) {
            settings.setLowVram(model.getLowVram());
        }
        if (model != null && model.getQuantize() != null) {
            settings.setQuantize(model.getQuantize());
        }
        if (processItem.getTriggerWord() != null) {
            settings.setTriggerWord(processItem.getTriggerWord());
        }
    }

    private int orDefault(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private double orDefault(Double value, double fallback) {
        return value != null ? value : fallback;
    }

    private boolean orDefault(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }

    private String orDefault(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private void applySettingsToFields() {
        resolutionXField.setValue(orDefault(settings.getResolutionX(), 512));
        resolutionYField.setValue(orDefault(settings.getResolutionY(), 512));
        stepsField.setValue(orDefault(settings.getSteps(), 2000));
        rankField.setValue(orDefault(settings.getRank(), 8));
        batchSizeField.setValue(orDefault(settings.getBatchSize(), 1));
        learningRateField.setValue(orDefault(settings.getLearningRate(), 1e-4));
        sampleEveryNStepsField.setValue(orDefault(settings.getSampleEveryNSteps(), 100));

        samplePromptsField.setValue(orDefault(settings.getSamplePrompts(), DEFAULT_PROMPT));
        triggerWordField.setValue(orDefault(settings.getTriggerWord(), DEFAULT_TRIGGER_WORD));

        gradientCheckpointingField.setValue(orDefault(settings.getGradientCheckpointing(), false));
        lowVramField.setValue(orDefault(settings.getLowVram(), false));
        quantizeField.setValue(orDefault(settings.getQuantize(), false));

        optimizerField.setValue(settings.getOptimizer());
        dtypeField.setValue(settings.getDtype());
    }

    private void bind(IntegerField field, Consumer<Integer> setter) {
        field.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                setter.accept(e.getValue());
                settingsService.save(settings);
            }
        });
    }

    private void bind(NumberField field, Consumer<Double> setter) {
        field.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                setter.accept(e.getValue());
                settingsService.save(settings);
            }
        });
    }

    private void bind(TextField field, Consumer<String> setter) {
        field.addValueChangeListener(e -> {
            setter.accept(e.getValue());
            settingsService.save(settings);
        });
    }

    private void bind(TextArea field, Consumer<String> setter) {
        field.addValueChangeListener(e -> {
            setter.accept(e.getValue());
            settingsService.save(settings);
        });
    }

    private void bind(Checkbox field, Consumer<Boolean> setter) {
        field.addValueChangeListener(e -> {
            setter.accept(e.getValue());
            settingsService.save(settings);
        });
    }

    private <T> void bind(ComboBox<T> field, Consumer<T> setter) {
        field.addValueChangeListener(e -> {
            setter.accept(e.getValue());
            settingsService.save(settings);
        });
    }

    private void registerSettingsUpdateListeners() {
        bind(resolutionXField, settings::setResolutionX);
        bind(resolutionYField, settings::setResolutionY);
        bind(stepsField, settings::setSteps);
        bind(rankField, settings::setRank);
        bind(batchSizeField, settings::setBatchSize);
        bind(learningRateField, settings::setLearningRate);
        bind(triggerWordField, settings::setTriggerWord);
        bind(sampleEveryNStepsField, settings::setSampleEveryNSteps);
        bind(samplePromptsField, settings::setSamplePrompts);
        bind(gradientCheckpointingField, settings::setGradientCheckpointing);
        bind(optimizerField, settings::setOptimizer);
        bind(dtypeField, settings::setDtype);
        bind(lowVramField, settings::setLowVram);
        bind(quantizeField, settings::setQuantize);
    }

    public JobConfiguration buildConfig() {
        if (currentTemplate == null && templateSelect.getValue() != null) {
            currentTemplate = templateService.loadTemplate(templateSelect.getValue());
        }
        if (currentTemplate == null
                || currentTemplate.getConfig() == null
                || currentTemplate.getConfig().getProcess() == null
                || currentTemplate.getConfig().getProcess().isEmpty()) {
            throw new IllegalStateException("No valid template configuration selected");
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
            prompts = List.of(DEFAULT_PROMPT);
        }

        JobConfiguration.Network network = base.getNetwork().toBuilder()
                .linear(rankField.getValue())
                .linearAlpha(rankField.getValue())
                .build();

        JobConfiguration.Train train = base.getTrain().toBuilder()
                .steps(stepsField.getValue())
                .lr(learningRateField.getValue())
                .batchSize(batchSizeField.getValue())
                .gradientCheckpointing(gradientCheckpointingField.getValue())
                .optimizer(optimizerField.getValue() != null ? optimizerField.getValue() : base.getTrain().getOptimizer())
                .dtype(dtypeField.getValue() != null ? dtypeField.getValue() : base.getTrain().getDtype())
                .build();

        JobConfiguration.Sample sample = base.getSample().toBuilder()
                .width(resolutionXField.getValue())
                .height(resolutionYField.getValue())
                .sampleEvery(sampleEveryNStepsField.getValue())
                .prompts(prompts)
                .seed(ThreadLocalRandom.current().nextInt())
                .build();

        JobConfiguration.Dataset dataset = base.getDatasets().getFirst().toBuilder()
                .resolution(List.of(resolutionXField.getValue(), resolutionYField.getValue()))
                .build();

        JobConfiguration.Model model = base.getModel().toBuilder()
                .lowVram(lowVramField.getValue())
                .quantize(quantizeField.getValue())
                .build();

        JobConfiguration.ProcessItem process = base.toBuilder()
                .network(network)
                .train(train)
                .sample(sample)
                .datasets(List.of(dataset))
                .model(model)
                .triggerWord(triggerWordField.getValue())
                .build();

        JobConfiguration.Config newCfg = cfg.toBuilder()
                .process(List.of(process))
                .name(CONFIG_NAME)
                .build();

        JobConfiguration.Meta meta = template.getMeta().toBuilder()
                .name(META_NAME)
                .version(ApplicationInformation.VERSION)
                .build();

        return template.toBuilder()
                .config(newCfg)
                .meta(meta)
                .build();
    }
}
