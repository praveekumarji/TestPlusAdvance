package com.testpulse.repository;

import com.testpulse.model.ShortQa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortQaRepository extends JpaRepository<ShortQa, Long> {
    List<ShortQa> findByClassIdOrderByIdAsc(Long classId);
}