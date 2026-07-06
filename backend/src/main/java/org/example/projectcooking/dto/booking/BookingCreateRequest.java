package org.example.projectcooking.dto.booking;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.domain.enums.EquipmentChoice;

/** Создание брони (POST /bookings, CLIENT — UC-01). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequest {

    @NotNull
    private UUID slotId;

    @NotNull
    private EquipmentChoice equipmentChoice;
}
