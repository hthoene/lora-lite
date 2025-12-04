package de.hthoene.loralite.aitoolkit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class JobConfiguration {

    // ===================== Enums =====================

    public enum JobType {
        @JsonProperty("extension")
        EXTENSION("extension");

        private final String value;
        JobType(String value) { this.value = value; }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public enum ProcessType {
        @JsonProperty("sd_trainer")
        SD_TRAINER("sd_trainer");

        private final String value;
        ProcessType(String value) { this.value = value; }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public enum NetworkType {
        @JsonProperty("lora")
        LORA("lora"),
        @JsonProperty("lokr")
        LOKR("lokr"); // optional, falls du LoKR-Configs auch unterstützen willst

        private final String value;
        NetworkType(String value) { this.value = value; }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public enum DType {
        @JsonProperty("float16")
        FLOAT16("float16"),
        @JsonProperty("bf16")
        BF16("bf16");

        private final String value;
        DType(String value) { this.value = value; }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public enum NoiseScheduler {
        @JsonProperty("flowmatch")
        FLOWMATCH("flowmatch");

        private final String value;
        NoiseScheduler(String value) { this.value = value; }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public enum Optimizer {
        @JsonProperty("adafactor")
        ADAFACTOR("adafactor"),
        @JsonProperty("adamw8bit")
        ADAMW_8BIT("adamw8bit"),
        @JsonProperty("adamw")
        ADAMW("adamw");

        private final String value;
        Optimizer(String value) { this.value = value; }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public enum Sampler {
        @JsonProperty("flowmatch")
        FLOWMATCH("flowmatch");

        private final String value;
        Sampler(String value) { this.value = value; }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    // ===================== Root =====================

    @JsonProperty("job")
    private JobType job; // "extension"

    @JsonProperty("config")
    private Config config;

    @JsonProperty("meta")
    private Meta meta;

    // ===================== Nested Classes =====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Config {

        // this name will be the folder and filename name
        @JsonProperty("name")
        private String name;

        @JsonProperty("process")
        private List<ProcessItem> process;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class ProcessItem {

        @JsonProperty("type")
        private ProcessType type;

        @JsonProperty("training_folder")
        private String trainingFolder;

        @JsonProperty("performance_log_every")
        private Integer performanceLogEvery;

        @JsonProperty("device")
        private String device;

        @JsonProperty("trigger_word")
        private String triggerWord;

        @JsonProperty("network")
        private Network network;

        @JsonProperty("save")
        private Save save;

        @JsonProperty("datasets")
        private List<Dataset> datasets;

        @JsonProperty("train")
        private Train train;

        @JsonProperty("model")
        private Model model;

        @JsonProperty("sample")
        private Sample sample;

        // generic bag for all future/undokumentierte Felder
        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Network {

        @JsonProperty("type")
        private NetworkType type;

        @JsonProperty("linear")
        private Integer linear;

        @JsonProperty("linear_alpha")
        private Integer linearAlpha;

        @JsonProperty("network_kwargs")
        private Map<String, Object> networkKwargs;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Save {

        @JsonProperty("dtype")
        private DType dtype;

        @JsonProperty("save_every")
        private Integer saveEvery;

        @JsonProperty("max_step_saves_to_keep")
        private Integer maxStepSavesToKeep;

        @JsonProperty("push_to_hub")
        private Boolean pushToHub;

        @JsonProperty("hf_repo_id")
        private String hfRepoId;

        @JsonProperty("hf_private")
        private Boolean hfPrivate;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Dataset {

        @JsonProperty("folder_path")
        private String folderPath;

        @JsonProperty("caption_ext")
        private String captionExt;

        @JsonProperty("caption_dropout_rate")
        private Double captionDropoutRate;

        @JsonProperty("shuffle_tokens")
        private Boolean shuffleTokens;

        @JsonProperty("cache_latents_to_disk")
        private Boolean cacheLatentsToDisk;

        @JsonProperty("resolution")
        private List<Integer> resolution;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Train {

        @JsonProperty("batch_size")
        private Integer batchSize;

        @JsonProperty("steps")
        private Integer steps;

        @JsonProperty("gradient_accumulation_steps")
        private Integer gradientAccumulationSteps;

        @JsonProperty("train_unet")
        private Boolean trainUnet;

        @JsonProperty("train_text_encoder")
        private Boolean trainTextEncoder;

        @JsonProperty("gradient_checkpointing")
        private Boolean gradientCheckpointing;

        @JsonProperty("noise_scheduler")
        private NoiseScheduler noiseScheduler;

        @JsonProperty("optimizer")
        private Optimizer optimizer;

        @JsonProperty("lr")
        private Double lr;

        @JsonProperty("skip_first_sample")
        private Boolean skipFirstSample;

        @JsonProperty("disable_sampling")
        private Boolean disableSampling;

        @JsonProperty("linear_timesteps")
        private Boolean linearTimesteps;

        @JsonProperty("ema_config")
        private EmaConfig emaConfig;

        @JsonProperty("dtype")
        private DType dtype;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class EmaConfig {

        @JsonProperty("use_ema")
        private Boolean useEma;

        @JsonProperty("ema_decay")
        private Double emaDecay;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Model {

        @JsonProperty("name_or_path")
        private String nameOrPath;

        @JsonProperty("is_flux")
        private Boolean isFlux;

        @JsonProperty("quantize")
        private Boolean quantize;

        @JsonProperty("low_vram")
        private Boolean lowVram;

        @JsonProperty("assistant_lora_path")
        private String assistantLoraPath;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Sample {

        @JsonProperty("sampler")
        private Sampler sampler;

        @JsonProperty("sample_every")
        private Integer sampleEvery;

        @JsonProperty("width")
        private Integer width;

        @JsonProperty("height")
        private Integer height;

        @JsonProperty("prompts")
        private List<String> prompts;

        @JsonProperty("neg")
        private String neg;

        @JsonProperty("seed")
        private Integer seed;

        @JsonProperty("walk_seed")
        private Boolean walkSeed;

        @JsonProperty("guidance_scale")
        private Double guidanceScale;

        @JsonProperty("sample_steps")
        private Integer sampleSteps;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    public static class Meta {

        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;

        @JsonProperty("extra")
        private Map<String, Object> extra;
    }
}
