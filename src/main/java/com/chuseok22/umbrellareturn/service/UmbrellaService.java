package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.entity.UmbrellaStatus;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UmbrellaService {

    private final UmbrellaRepository umbrellaRepository;

    public UmbrellaService(UmbrellaRepository umbrellaRepository) {
        this.umbrellaRepository = umbrellaRepository;
    }

    public List<Umbrella> findAvailable() {
        return umbrellaRepository.findByStatusOrderByNumberAsc(UmbrellaStatus.AVAILABLE);
    }

    public List<Umbrella> findRented() {
        return umbrellaRepository.findByStatusOrderByNumberAsc(UmbrellaStatus.RENTED);
    }

    public List<Umbrella> findAll() {
        return umbrellaRepository.findAllByOrderByNumberAsc();
    }

    @Transactional
    public void register(String number) {
        if (umbrellaRepository.existsByNumber(number)) {
            throw new IllegalArgumentException("이미 등록된 우산 번호입니다: " + number);
        }
        umbrellaRepository.save(new Umbrella(number));
    }

    @Transactional
    public void delete(Long id) {
        Umbrella umbrella = umbrellaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 우산입니다."));
        if (!umbrella.isAvailable()) {
            throw new IllegalStateException("대여 중인 우산은 삭제할 수 없습니다.");
        }
        umbrellaRepository.delete(umbrella);
    }

    public long countAvailable() {
        return umbrellaRepository.countByStatus(UmbrellaStatus.AVAILABLE);
    }

    public long countTotal() {
        return umbrellaRepository.count();
    }
}
