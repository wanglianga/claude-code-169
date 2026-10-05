package com.city.garbage.repo;

import com.city.garbage.domain.DeviceEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceEventLogRepository extends JpaRepository<DeviceEventLog, Long> {
}
