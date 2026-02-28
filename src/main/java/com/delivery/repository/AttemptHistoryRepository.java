package com.delivery.repository;

import com.delivery.entity.AttemptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AttemptHistoryRepository
        extends JpaRepository<AttemptHistory, Long>,
        JpaSpecificationExecutor<AttemptHistory> {
}
