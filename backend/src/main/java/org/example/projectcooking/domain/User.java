package org.example.projectcooking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.projectcooking.domain.enums.UserRole;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Базовая учётная запись всех трёх ролей (ER-модель, tz-11 §2.1).
 * Пароля нет ни у кого (БТ-06) — поля passwordHash не существует.
 * Клиент создаётся молчаливо при первом входе (ФТ-21); шеф — только админом (D-08).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /** У нового клиента пусто до ответа «Как к вам обращаться?» (Р-12, ФТ-21); ≤ 100 (openapi). */
    @Column(length = 100)
    private String name;

    /** Логин (ФТ-13) и канал уведомлений (D-07); unique; не редактируется (Р-16). */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
