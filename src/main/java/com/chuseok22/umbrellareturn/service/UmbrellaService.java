package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.entity.UmbrellaStatus;
import com.chuseok22.umbrellareturn.exception.CustomException;
import com.chuseok22.umbrellareturn.exception.ErrorCode;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UmbrellaService {

    private static final Logger log = LoggerFactory.getLogger(UmbrellaService.class);

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
            throw new CustomException(ErrorCode.UMBRELLA_ALREADY_EXISTS);
        }
        umbrellaRepository.save(new Umbrella(number));
        log.info("우산 등록 완료: number={}", number);
    }

    @Transactional
    public void delete(Long id) {
        Umbrella umbrella = umbrellaRepository.findById(id)
            .orElseThrow(() -> new CustomException(ErrorCode.UMBRELLA_NOT_FOUND));
        if (!umbrella.isAvailable()) {
            throw new CustomException(ErrorCode.UMBRELLA_IN_USE);
        }
        umbrellaRepository.delete(umbrella);
        log.info("우산 삭제 완료: id={}, number={}", id, umbrella.getNumber());
    }

    public long countAvailable() {
        return umbrellaRepository.countByStatus(UmbrellaStatus.AVAILABLE);
    }

    public long countTotal() {
        return umbrellaRepository.count();
    }
}
