package de.hthoene.loralite.util;

import lombok.Data;

@Data
public class GpuInfo {
    private int index;
    private double util;
    private long mem_used;
    private long mem_total;
}
