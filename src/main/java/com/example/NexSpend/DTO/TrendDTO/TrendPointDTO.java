package com.example.NexSpend.DTO.TrendDTO;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendPointDTO {
    private LocalDate date;
    private String label;
    private BigDecimal amount;
}
