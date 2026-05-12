package com.chuseok22.umbrellareturn.repository;

import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.entity.UmbrellaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UmbrellaRepository extends JpaRepository<Umbrella, Long> {
    List<Umbrella> findByStatusOrderByNumberAsc(UmbrellaStatus status);
    List<Umbrella> findAllByOrderByNumberAsc();
    Optional<Umbrella> findByNumber(String number);
    boolean existsByNumber(String number);
}
