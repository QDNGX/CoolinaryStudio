package org.example.projectcooking.mapper;

import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.example.projectcooking.dto.booking.ParticipantBookingResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final SlotMapper slotMapper;

    public BookingResponse toResponse(Booking b, boolean hasReview) {
        return BookingResponse.builder()
                .id(b.getId())
                .slot(slotMapper.toSummary(b.getSlot()))
                .status(b.getStatus())
                .equipmentChoice(b.getEquipmentChoice())
                .rentalPriceSnapshot(b.getRentalPriceSnapshot())
                .createdAt(b.getCreatedAt())
                .cancelledAt(b.getCancelledAt())
                .isLateCancellation(b.getIsLateCancellation())
                .hasReview(hasReview)
                .build();
    }

    /** Участник класса для шефа/админа — с заметкой об аллергии (ФТ-18), без текстов отзывов (Р-07). */
    public ParticipantBookingResponse toParticipant(Booking b) {
        return ParticipantBookingResponse.builder()
                .id(b.getId())
                .clientName(b.getClient().getUser().getName())
                .allergyNote(b.getClient().getAllergyNote())
                .status(b.getStatus())
                .equipmentChoice(b.getEquipmentChoice())
                .build();
    }
}
