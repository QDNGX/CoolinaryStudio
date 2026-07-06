package org.example.projectcooking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.ClientProfile;
import org.example.projectcooking.domain.Program;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.User;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.EquipmentChoice;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Transactional
@Import(IntegrationTestConfig.class)
class BookingIntegrationTest extends IntegrationTestSupport {

    @Test
    void guestCanReadSlotAndClientCanBookAndCancel() throws Exception {
        Program program = createProgram("Program " + UUID.randomUUID());
        ChefProfile chef = createChef("chef");
        ClientProfile client = createClient("client");
        Slot slot = createSlot(program, chef, Instant.now().plus(2, ChronoUnit.DAYS), 3, 2, 650.0, SlotStatus.SCHEDULED);
        String clientToken = loginToken(client.getUser().getEmail());
        String chefToken = loginToken(chef.getUser().getEmail());

        mockMvc.perform(get("/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(slot.getId().toString()));

        mockMvc.perform(get("/slots/" + slot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.program.id").value(program.getId().toString()))
                .andExpect(jsonPath("$.id").value(slot.getId().toString()));

        BookingResponse booking = createBookingViaApi(clientToken, slot.getId(), EquipmentChoice.RENTAL);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getRentalPriceSnapshot()).isEqualTo(650.0);

        Slot afterBooking = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(afterBooking.getFreeSpots()).isEqualTo(2);
        assertThat(afterBooking.getRentalSetsAvailable()).isEqualTo(1);

        mockMvc.perform(get("/me/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(booking.getId().toString()));

        mockMvc.perform(post("/bookings/" + booking.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED_BY_CLIENT"));

        Slot afterCancel = slotRepository.findById(slot.getId()).orElseThrow();
        Booking cancelled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(afterCancel.getFreeSpots()).isEqualTo(3);
        assertThat(afterCancel.getRentalSetsAvailable()).isEqualTo(2);
        assertThat(cancelled.getCancelledAt()).isNotNull();

        mockMvc.perform(post("/bookings")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json(new org.example.projectcooking.dto.booking.BookingCreateRequest(slot.getId(), EquipmentChoice.OWN))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(chefToken))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json(new org.example.projectcooking.dto.booking.BookingCreateRequest(slot.getId(), EquipmentChoice.OWN))))
                .andExpect(status().isForbidden());
    }
}
