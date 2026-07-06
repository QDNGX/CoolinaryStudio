package org.example.projectcooking.repository;

import java.util.UUID;
import org.example.projectcooking.domain.ClientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, UUID> {
}
