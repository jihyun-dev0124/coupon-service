package io.github.jihyundev.coupon_service.domain.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) //동시성 제어 (비관적 쓰기 락)
    @Query("select e from OutboxEvent e where e.status = :status order by e.id asc")
    List<OutboxEvent> findForUpdatedByStatus(@Param("status") OutboxStatus status, PageRequest pageable);

    //flushAutomatically - 영속성 컨텍스트 변경 사항 먼저 flush, clearAutomatically - 쿼리 실행 후 영속성 컨텍스트 clear
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OutboxEvent e set e.status = :to where e.id = :id and e.status = :from")
    int compareAndSetStatus(@Param("id") Long id, @Param("from") OutboxStatus from, @Param("to") OutboxStatus to);
}
