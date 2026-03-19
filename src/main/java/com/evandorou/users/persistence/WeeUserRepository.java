package com.evandorou.users.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WeeUserRepository extends JpaRepository<WeeUser, UUID> {

    Optional<WeeUser> findByExternalUserId(String externalUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from WeeUser u where u.externalUserId = :externalUserId")
    Optional<WeeUser> findByExternalUserIdForUpdate(@Param("externalUserId") String externalUserId);
}
