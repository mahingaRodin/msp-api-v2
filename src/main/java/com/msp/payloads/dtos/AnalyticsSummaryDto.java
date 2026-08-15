package com.msp.payloads.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDto {
    private double revenue;
    private long orderCount;
    private long storeCount;
    private long branchCount;
    private long productCount;

    @Builder.Default
    private List<DailyMetricDto> daily = new ArrayList<>();

    @Builder.Default
    private List<NamedMetricDto> byBranch = new ArrayList<>();

    @Builder.Default
    private List<NamedMetricDto> topProducts = new ArrayList<>();
}
