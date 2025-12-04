package de.hthoene.loralite.util;

import lombok.Data;

import java.util.List;

@Data
public class GpuStats {
    private String backend;
    private List<GpuInfo> gpus;
}
