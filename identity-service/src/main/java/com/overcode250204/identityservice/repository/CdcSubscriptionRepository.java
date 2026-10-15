package com.overcode250204.identityservice.repository;

import com.overcode250204.identityservice.entity.CdcSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CdcSubscriptionRepository extends JpaRepository<CdcSubscription, UUID> {
}
