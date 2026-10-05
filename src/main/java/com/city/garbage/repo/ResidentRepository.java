package com.city.garbage.repo;

import com.city.garbage.domain.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    Optional<Resident> findByCardNo(String cardNo);
}
