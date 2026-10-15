package com.overcode250204.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cdc_subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdcSubscription {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "monitored_tables", nullable = false)
    private String monitoredTables;

    @Column(nullable = false)
    private String status;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    public List<String> getTablesList() {
        if (monitoredTables == null || monitoredTables.isBlank()) {
            return List.of();
        }
        return Arrays.asList(monitoredTables.split(","));
    }

    public void setTablesList(List<String> tables) {
        if (tables == null || tables.isEmpty()) {
            this.monitoredTables = "";
        } else {
            this.monitoredTables = String.join(",", tables);
        }
    }
}
