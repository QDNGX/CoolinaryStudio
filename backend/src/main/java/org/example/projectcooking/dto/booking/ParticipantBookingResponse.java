package org.example.projectcooking.dto.booking;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.EquipmentChoice;

/** Бронь глазами шефа/админа — участник класса (openapi {@code ParticipantBooking}, SCR-10/13). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantBookingResponse {

    private UUID id;
    private String clientName;

    /** Заметка об аллергии участника (ФТ-18). */
    private String allergyNote;

    private BookingStatus status;
    private EquipmentChoice equipmentChoice;
}
