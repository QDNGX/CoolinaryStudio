package org.example.projectcooking.repository;

import java.util.List;
import java.util.UUID;
import org.example.projectcooking.domain.ChefProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChefProfileRepository extends JpaRepository<ChefProfile, UUID> {

    /** Список шефов (SCR-16); только растёт (Р-15). */
    List<ChefProfile> findAllByOrderByUser_CreatedAtAsc();
}
