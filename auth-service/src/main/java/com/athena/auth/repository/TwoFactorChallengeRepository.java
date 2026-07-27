package com.athena.auth.repository;

import com.athena.auth.entity.TwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, UUID> {
}
