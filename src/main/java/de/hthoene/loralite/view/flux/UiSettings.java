package de.hthoene.loralite.view.flux;

import lombok.Data;

@Data
public class UiSettings {
    private Integer resolutionX;
    private Integer resolutionY;
    private Integer steps;
    private Integer rank;
    private Double learningRate;
    private String samplePrompts;
    private String templateName;
}
