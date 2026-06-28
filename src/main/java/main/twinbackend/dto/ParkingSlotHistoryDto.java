package main.twinbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlotHistoryDto {
    private String uniqueId;
    private Integer frameId;
    private String id; // Slot ID code (e.g. A01, D03)
    private Integer occupied; // 1 = occupied, 0 = vacant
    private LocalDateTime timestamp;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status; // "active" or "inactive"
}
