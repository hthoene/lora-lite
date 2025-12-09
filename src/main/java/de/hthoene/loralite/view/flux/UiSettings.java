package de.hthoene.loralite.view.flux;

import de.hthoene.loralite.aitoolkit.JobConfiguration;
import lombok.Data;

@Data
public class UiSettings {
    private Integer resolutionX;
    private Integer resolutionY;
    private Integer steps;
    private Integer rank;
    private Integer batchSize;
    private Double learningRate;
    private Integer sampleEveryNSteps;
    private String samplePrompts;
    private String triggerWord;
    private String templateName;

    private Boolean gradientCheckpointing;
    private JobConfiguration.Optimizer optimizer;
    private JobConfiguration.DType dtype;
    private Boolean lowVram;
    private Boolean quantize;

    private Integer saveEvery;
    private Integer maxStepSavesToKeep;
}
