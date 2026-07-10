package com.microservice.demo.repository;

import com.microservice.demo.entity.DemoOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for demo order persistence verification.
 *
 * @author Andy Yang
 */
public interface DemoOrderRepository extends JpaRepository<DemoOrderEntity, Long> {

    Optional<DemoOrderEntity> findByOrderNo(String orderNo);
}
