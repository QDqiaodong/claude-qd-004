package com.dairy.farm.repository;

import com.dairy.farm.entity.MilkingShift;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
