package com.chuseok22.umbrellareturn.repository;

import com.chuseok22.umbrellareturn.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    List<SmsLog> findAllByOrderBySentAtDesc();
}
