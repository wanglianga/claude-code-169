package com.city.binroom.domain.repo;

import com.city.binroom.domain.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    Optional<Resident> findByCardNo(String cardNo);
}
