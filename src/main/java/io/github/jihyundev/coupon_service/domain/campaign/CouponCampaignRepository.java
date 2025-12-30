package io.github.jihyundev.coupon_service.domain.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface CouponCampaignRepository extends JpaRepository<CouponCampaign, Long> {
    Optional<CouponCampaign> findByCouponId(String couponId);

    /**
     * 리컨실 대상 (DRAFT) - 가까운 미래 오픈 예정 포함, 이미 끝난 캠페인 제외
     * @param status (DRAFT)
     * @param now
     * @param horizon
     * @return
     */
    @Query("""
        select c
        from CouponCampaign c
        where c.status = :status
        and   c.startAt <= :horizon
        and   c.endAt >= :now
        """)
    List<CouponCampaign> findDraftInWindow(@Param("status") CampaignStatus status, @Param("now") Instant now, @Param("horizon") Instant horizon);

    /**
     * 리컨실 대상(OPEN) - 가까운 미래 종료 예정 포함
     * @param status (OPEN)
     * @param horizon
     * @return
     */
    @Query("""
        select c
        from CouponCampaign c
        where c.status = :status
        and c.endAt <= :horizon
    """)
    List<CouponCampaign> findOpenEndingByHorizon(@Param("status") CampaignStatus status, @Param("horizon") Instant horizon);

    List<CouponCampaign> findByCouponIdIn(Collection<String> couponIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CouponCampaign c
            set c.status = :to,
                c.updatedAt = :now
            where c.status = :from
    """)
    int updateStatusIfMatch(@Param("couponId") String couponId, @Param("from") CampaignStatus from, @Param("to") CampaignStatus to, @Param("now") Instant now);
}
