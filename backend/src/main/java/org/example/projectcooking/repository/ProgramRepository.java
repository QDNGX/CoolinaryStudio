package org.example.projectcooking.repository;

import java.util.List;
import java.util.UUID;
import org.example.projectcooking.domain.Program;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    List<Program> findAllByOrderByTitleAsc();
}
