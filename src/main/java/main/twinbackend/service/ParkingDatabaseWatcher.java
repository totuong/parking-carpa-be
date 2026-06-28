package main.twinbackend.service;

import lombok.RequiredArgsConstructor;
import main.twinbackend.entity.ParkingSlotHistory;
import main.twinbackend.repository.ParkingSlotHistoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ParkingDatabaseWatcher {

    private final ParkingSlotHistoryRepository historyRepository;
    private final DashboardService dashboardService;

    // Cache to hold the state of active parking slots (slotId -> stateString)
    private final Map<String, String> lastActiveStates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        List<ParkingSlotHistory> activeSlots = historyRepository.findByStatus("active");
        if (activeSlots != null) {
            for (ParkingSlotHistory slot : activeSlots) {
                if (slot.getId() != null) {
                    lastActiveStates.put(slot.getId(), getSlotStateRepresentation(slot));
                }
            }
        }
    }

    @Scheduled(fixedDelay = 1000) // Poll every 1 second
    public void watchDatabaseChanges() {
        List<ParkingSlotHistory> activeSlots = historyRepository.findByStatus("active");
        
        Map<String, String> currentStates = new ConcurrentHashMap<>();
        if (activeSlots != null) {
            for (ParkingSlotHistory slot : activeSlots) {
                if (slot.getId() != null) {
                    currentStates.put(slot.getId(), getSlotStateRepresentation(slot));
                }
            }
        }

        // Compare size and entries
        boolean changed = false;
        if (currentStates.size() != lastActiveStates.size()) {
            changed = true;
        } else {
            for (Map.Entry<String, String> entry : currentStates.entrySet()) {
                String lastValue = lastActiveStates.get(entry.getKey());
                if (!Objects.equals(lastValue, entry.getValue())) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            lastActiveStates.clear();
            lastActiveStates.putAll(currentStates);
            dashboardService.pushDashboard();
        }
    }

    private String getSlotStateRepresentation(ParkingSlotHistory slot) {
        int occupiedVal = slot.getOccupied() != null ? slot.getOccupied() : 0;
        String uniqueIdVal = slot.getUniqueId() != null ? slot.getUniqueId() : "";
        return "occupied=" + occupiedVal + ";uniqueId=" + uniqueIdVal;
    }
}
