package com.evandorou.bets.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BetRepository extends JpaRepository<Bet, UUID> {

    @Query("select b from Bet b join fetch b.user u where b.eventId = :eventId and b.marketKey = :marketKey and b.status = :status order by u.id")
    List<Bet> findPendingByEventAndMarketOrderByUserId(
            @Param("eventId") String eventId,
            @Param("marketKey") String marketKey,
            @Param("status") BetStatus status
    );
}
