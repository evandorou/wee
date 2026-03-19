package com.evandorou.bets.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BetRepository extends JpaRepository<Bet, UUID> {}
