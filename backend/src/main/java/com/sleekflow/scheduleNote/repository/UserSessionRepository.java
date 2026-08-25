package com.sleekflow.scheduleNote.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.sleekflow.scheduleNote.domain.User;
import com.sleekflow.scheduleNote.domain.UserSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

	/** Fetches the user too: every authenticated request needs both. */
	@Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.tokenHash = :tokenHash")
	Optional<UserSession> findByTokenHash(@Param("tokenHash") String tokenHash);

	List<UserSession> findByUserAndRevokedAtIsNull(User user);

	@Modifying
	@Query("DELETE FROM UserSession s WHERE s.expiresAt < :cutoff")
	int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
