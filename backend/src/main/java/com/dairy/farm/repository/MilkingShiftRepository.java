package com.dairy.farm.repository;

import com.dairy.farm.entity.MilkingShift;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MilkingShiftRepository extends JpaRepository<MilkingShift, Long> {

    boolean existsByShiftNo(String shiftNo);

    List<MilkingShift> findAllByOrderByUpdatedAtDesc();

    List<MilkingShift> findByMilkingDateOrderByStartMinAsc(LocalDate milkingDate);

    List<MilkingShift> findByStallIdAndMilkingDateAndStatusNotIn(
            Long stallId, LocalDate milkingDate, Collection<String> statuses);

    List<MilkingShift> findByMilkerAndMilkingDateAndStatusNotIn(
            String milker, LocalDate milkingDate, Collection<String> statuses);

    List<MilkingShift> findByBarnIdAndMilkingDateAndStatusNotIn(
            Long barnId, LocalDate milkingDate, Collection<String> statuses);

    /** 锁班次行：同一班次并发插抽检时，先拿到锁的那条落库，后到的按台账现状被拦下。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MilkingShift s where s.id = :id")
    Optional<MilkingShift> findByIdForUpdate(@Param("id") Long id);
}
