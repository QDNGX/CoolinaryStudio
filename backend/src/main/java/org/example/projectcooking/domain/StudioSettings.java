package org.example.projectcooking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Синглтон — одна запись на систему (tz-11 §2.4, ФТ-17). Меняет ADMIN, читают все, включая гостя.
 * Единственность обеспечивается фиксированным первичным ключом {@link #SINGLETON_ID}.
 */
@Entity
@Table(name = "studio_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioSettings {

    /** Фиксированный PK синглтона. */
    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String contactPhone;

    @Column(nullable = false)
    private String contactEmail;
}
