package main.twinbackend.repository;

import main.twinbackend.entity.ParkingSlotHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotHistoryRepository extends JpaRepository<ParkingSlotHistory, Long> {

    List<ParkingSlotHistory> findByStatus(String status);
    Page<ParkingSlotHistory> findByStatus(String status, Pageable pageable);

    List<ParkingSlotHistory> findAllByOrderByTimestampDesc();

    Optional<ParkingSlotHistory> findFirstByIdAndStatusOrderByUniqueIdDesc(String id, String status);

    @Query("SELECT COALESCE(MAX(h.uniqueId), 0) FROM ParkingSlotHistory h")
    Long getMaxUniqueId();
}
