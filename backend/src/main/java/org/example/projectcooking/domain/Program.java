package org.example.projectcooking.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.projectcooking.domain.enums.DifficultyLevel;

/**
 * Каталог программ (tz-11 §2.5). Создаёт/редактирует только ADMIN (ФТ-01); архивации нет (Р-15).
 * requiresComplexEquipment=true → лимит вместимости 8 вместо 12 (ФТ-02), только для новых слотов (Р-04).
 */
@Entity
@Table(name = "programs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    /** Свободный текст (ФТ-01). */
    @Column(nullable = false)
    private String cuisineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficultyLevel;

    @Column(nullable = false)
    private boolean requiresComplexEquipment;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "program_dishes", joinColumns = @JoinColumn(name = "program_id"))
    @Column(name = "dish", length = 500)
    private List<String> dishes = new ArrayList<>();

    /** ≤ 10 элементов (Р-16) — проверяется на уровне DTO. */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "program_photos", joinColumns = @JoinColumn(name = "program_id"))
    @Column(name = "photo_url", length = 1000)
    private List<String> photos = new ArrayList<>();
}
