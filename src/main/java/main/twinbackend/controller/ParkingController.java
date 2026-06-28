package main.twinbackend.controller;

import lombok.RequiredArgsConstructor;
import main.twinbackend.dto.ParkingRealtimeMessage;
import main.twinbackend.dto.ParkingSlotHistoryDto;
import main.twinbackend.dto.ResponseApi;
import main.twinbackend.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ParkingController {

    private final DashboardService dashboardService;

    @GetMapping("/status")
    public ParkingRealtimeMessage getStatus() {
        return dashboardService.getDashboardMessage();
    }

    @GetMapping("/slot")
    public ResponseEntity<ResponseApi> getSlots() {
        List<ParkingSlotHistoryDto> parkingSlotHistoryDtoS = dashboardService.getSlots();

        return ResponseEntity.ok(new ResponseApi(parkingSlotHistoryDtoS, ResponseApi.Meta.builder().size(parkingSlotHistoryDtoS.size()).build()));
    }

    @GetMapping("/history")
    public ResponseEntity<ResponseApi> getHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(new ResponseApi(dashboardService.getHistories(pageable)));
    }

//    @PostMapping("/slots")
//    public ParkingSlotHistory saveOrUpdateSlot(@RequestBody Map<String, Object> payload) {
//        return dashboardService.saveOrUpdateSlot(payload);
//    }
//
//    @PostMapping("/events")
//    public ParkingSlotHistory createEvent(@RequestBody Map<String, Object> payload) {
//        return dashboardService.createEvent(payload);
//    }
//
//    @PostMapping("/history")
//    public ParkingSlotHistory saveHistory(@RequestBody ParkingSlotHistory history) {
//        return dashboardService.saveOrUpdateSlotHistory(history);
}
