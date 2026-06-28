package main.twinbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ParkingRealtimeMessage {

    private List<ParkingSlotHistoryDto> slots;

    private List<ParkingSlotHistoryDto> history;

}

