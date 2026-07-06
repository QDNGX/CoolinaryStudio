package org.example.projectcooking.repository;

import java.util.UUID;
import org.example.projectcooking.domain.StudioSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudioSettingsRepository extends JpaRepository<StudioSettings, UUID> {
}
