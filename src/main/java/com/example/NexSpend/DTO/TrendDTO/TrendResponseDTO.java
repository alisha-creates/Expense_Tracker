package com.example.NexSpend.DTO.TrendDTO;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendResponseDTO {
    private int weekOffset;
    private String rangeLabel;
    private List<TrendPointDTO> points;
    private BigDecimal total;
    private boolean isCurrentWeek;
}
