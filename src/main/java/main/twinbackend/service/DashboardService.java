package main.twinbackend.service;

import lombok.RequiredArgsConstructor;
import main.twinbackend.dto.ParkingRealtimeMessage;
import main.twinbackend.dto.ParkingSlotHistoryDto;
import main.twinbackend.entity.ParkingSlotHistory;
import main.twinbackend.repository.ParkingSlotHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ParkingSlotHistoryRepository historyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ParkingRealtimeMessage getDashboardMessage() {
        List<ParkingSlotHistoryDto> activeSlots = historyRepository.findByStatus("active")
                .stream()
                .map(this::toHistoryDto)
                .toList();

        List<ParkingSlotHistoryDto> history = historyRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(this::toHistoryDto)
                .toList();

        return ParkingRealtimeMessage.builder()
                .slots(activeSlots)
                .history(history)
                .build();
    }

    public List<ParkingSlotHistoryDto> getSlots() {

        return historyRepository.findByStatus("active")
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    public Page<ParkingSlotHistoryDto> getHistories(Pageable pageable) {
        Page<ParkingSlotHistory> parkingSlotHistoryPage = historyRepository.findByStatus("inactive", pageable);
        return parkingSlotHistoryPage.map(this::toHistoryDto);
    }

    public void pushDashboard() {
        ParkingRealtimeMessage dto = getDashboardMessage();
        messagingTemplate.convertAndSend("/parking/dashboard", dto);
    }

    private ParkingSlotHistoryDto toHistoryDto(ParkingSlotHistory e) {
        if (e == null)
            return null;
        return ParkingSlotHistoryDto.builder()
                .uniqueId(e.getUniqueId())
                .frameId(e.getFrameId())
                .id(e.getId())
                .occupied(e.getOccupied())
                .timestamp(e.getTimestamp())
                .startDate(e.getStartDate())
                .status(e.getStatus())
                .build();
    }

    @Transactional
    public ParkingSlotHistory saveOrUpdateSlotHistory(ParkingSlotHistory incoming) {
        if (incoming.getId() == null) {
            throw new IllegalArgumentException("Slot id cannot be null");
        }

        // Find the current active record for this slot id
        Optional<ParkingSlotHistory> activeOpt = historyRepository
                .findFirstByIdAndStatusOrderByUniqueIdDesc(incoming.getId(), "active");

        ParkingSlotHistory saved;
        if (activeOpt.isPresent()) {
            ParkingSlotHistory activeRecord = activeOpt.get();

            // Check if the occupied state changed (0 -> 1 or 1 -> 0)
            boolean isOccupiedChanged = activeRecord.getOccupied() == null ||
                    !activeRecord.getOccupied().equals(incoming.getOccupied());

            if (isOccupiedChanged) {
                // SCD Type 2: Expire the old active record by setting status to "inactive"
                activeRecord.setStatus("inactive");
                historyRepository.save(activeRecord);

                // Insert the new active record
                incoming.setUniqueId(null); // Let the database auto-generate the ID
                incoming.setStatus("active");
                if (incoming.getStartDate() == null) {
                    incoming.setStartDate(LocalDateTime.now());
                }
                if (incoming.getTimestamp() == null) {
                    incoming.setTimestamp(LocalDateTime.now());
                }
                saved = historyRepository.save(incoming);
            } else {
                // If occupancy status hasn't changed, optimize by updating the active record's
                // timestamps and frame ID instead of inserting a duplicate
                activeRecord
                        .setTimestamp(incoming.getTimestamp() != null ? incoming.getTimestamp() : LocalDateTime.now());
                if (incoming.getFrameId() != null) {
                    activeRecord.setFrameId(incoming.getFrameId());
                }
                saved = historyRepository.save(activeRecord);
            }
        } else {
            // No active record exists yet for this slot ID, insert it as the new active
            // record
            incoming.setUniqueId(null);
            incoming.setStatus("active");
            if (incoming.getStartDate() == null) {
                incoming.setStartDate(LocalDateTime.now());
            }
            if (incoming.getTimestamp() == null) {
                incoming.setTimestamp(LocalDateTime.now());
            }
            saved = historyRepository.save(incoming);
        }

        pushDashboard();
        return saved;
    }

    @Transactional
    public ParkingSlotHistory saveOrUpdateSlot(Map<String, Object> payload) {
        ParkingSlotHistory mapped = mapToHistory(payload);
        return saveOrUpdateSlotHistory(mapped);
    }

    @Transactional
    public ParkingSlotHistory createEvent(Map<String, Object> payload) {
        ParkingSlotHistory mapped = mapToHistory(payload);
        return saveOrUpdateSlotHistory(mapped);
    }

    private ParkingSlotHistory mapToHistory(Map<String, Object> payload) {
        ParkingSlotHistory history = new ParkingSlotHistory();

        // 1. Map id / slotId
        if (payload.containsKey("id") && payload.get("id") != null) {
            history.setId(String.valueOf(payload.get("id")));
        } else if (payload.containsKey("slotId") && payload.get("slotId") != null) {
            history.setId(String.valueOf(payload.get("slotId")));
        }

        // 2. Map occupied (can be Boolean true/false or Number 1/0)
        if (payload.containsKey("occupied") && payload.get("occupied") != null) {
            Object occ = payload.get("occupied");
            if (occ instanceof Boolean) {
                history.setOccupied((Boolean) occ ? 1 : 0);
            } else if (occ instanceof Number) {
                history.setOccupied(((Number) occ).intValue());
            } else {
                try {
                    history.setOccupied(Integer.parseInt(String.valueOf(occ)));
                } catch (Exception e) {
                    history.setOccupied(0);
                }
            }
        } else {
            history.setOccupied(0);
        }

        // 3. Map frameId / frame_id
        if (payload.containsKey("frameId") && payload.get("frameId") != null) {
            try {
                history.setFrameId(((Number) payload.get("frameId")).intValue());
            } catch (Exception e) {
            }
        } else if (payload.containsKey("frame_id") && payload.get("frame_id") != null) {
            try {
                history.setFrameId(((Number) payload.get("frame_id")).intValue());
            } catch (Exception e) {
            }
        }

        // 4. Map timestamp / timestampt / eventTime / occupiedSince / lastUpdated
        LocalDateTime ts = null;
        String[] timeKeys = { "timestampt", "timestamp", "eventTime", "occupiedSince", "lastUpdated" };
        for (String key : timeKeys) {
            if (payload.containsKey(key) && payload.get(key) != null) {
                String val = String.valueOf(payload.get(key));
                try {
                    ts = LocalDateTime.parse(val);
                    break;
                } catch (Exception e) {
                    try {
                        ts = java.time.OffsetDateTime.parse(val).toLocalDateTime();
                        break;
                    } catch (Exception ex) {
                    }
                }
            }
        }
        if (ts == null) {
            ts = LocalDateTime.now();
        }
        history.setTimestamp(ts);

        // 5. Map startdate / startDate
        LocalDateTime sd = null;
        if (payload.containsKey("startdate") && payload.get("startdate") != null) {
            try {
                sd = LocalDateTime.parse(String.valueOf(payload.get("startdate")));
            } catch (Exception e) {
            }
        } else if (payload.containsKey("startDate") && payload.get("startDate") != null) {
            try {
                sd = LocalDateTime.parse(String.valueOf(payload.get("startDate")));
            } catch (Exception e) {
            }
        }
        if (sd == null) {
            sd = LocalDateTime.now();
        }
        history.setStartDate(sd);

        // 6. Map status
        if (payload.containsKey("status") && payload.get("status") != null) {
            history.setStatus(String.valueOf(payload.get("status")));
        } else {
            history.setStatus("active");
        }

        return history;
    }
}
