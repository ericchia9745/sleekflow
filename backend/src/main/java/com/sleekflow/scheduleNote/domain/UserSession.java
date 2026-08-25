package com.sleekflow.scheduleNote.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A logged-in session. The row holds the SHA-256 of the bearer token rather
 * than the token, so the table is useless to anyone who reads it.
 */
@Entity
@Table(name = "user_sessions")
public class UserSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected UserSession() {
	}

	public UserSession(String tokenHash, User user, Instant expiresAt) {
		Instant now = Instant.now();
		this.tokenHash = tokenHash;
		this.user = user;
		this.createdAt = now;
		this.lastSeenAt = now;
		this.expiresAt = expiresAt;
	}

	public boolean isUsableAt(Instant moment) {
		return this.revokedAt == null && this.expiresAt.isAfter(moment);
	}

	public void revoke() {
		this.revokedAt = Instant.now();
	}

	/**
	 * Records activity, and slides the expiry forward so an active user is not
	 * logged out mid-session.
	 */
	public void touch(Instant now, Instant newExpiry) {
		this.lastSeenAt = now;
		if (newExpiry.isAfter(this.expiresAt)) {
			this.expiresAt = newExpiry;
		}
	}

	public Long getId() {
		return this.id;
	}

	public User getUser() {
		return this.user;
	}

	public Instant getExpiresAt() {
		return this.expiresAt;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getLastSeenAt() {
		return this.lastSeenAt;
	}
}
