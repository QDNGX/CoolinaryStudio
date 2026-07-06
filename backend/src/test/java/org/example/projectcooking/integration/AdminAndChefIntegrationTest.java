package org.example.projectcooking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.ClientProfile;
import org.example.projectcooking.domain.Program;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.User;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.DifficultyLevel;
import org.example.projectcooking.domain.enums.EquipmentChoice;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.example.projectcooking.dto.program.ProgramInput;
import org.example.projectcooking.dto.program.ProgramResponse;
import org.example.projectcooking.dto.slot.SlotCreateRequest;
import org.example.projectcooking.dto.slot.SlotDetailsResponse;
import org.example.projectcooking.dto.slot.SlotSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@Transactional
@Import(IntegrationTestConfig.class)
class AdminAndChefIntegrationTest extends IntegrationTestSupport {

    @Test
    void adminCanCreateProgramAndSlotAndCancelItWhileChefCanMarkNoShow() throws Exception {
        User admin = createAdmin("admin");
        String adminToken = loginToken(admin.getEmail());

        ProgramResponse createdProgram = createProgramViaApi(adminToken, new ProgramInput(
                "Program " + UUID.randomUUID(),
                "Admin-created program",
                "Italian",
                DifficultyLevel.ADVANCED,
                false,
                List.of("Tagliatelle"),
                List.of()));

        ChefProfile chef = createChef("chef");
        SlotDetailsResponse createdSlot = createSlotViaApi(adminToken, new SlotCreateRequest(
                createdProgram.getId(),
                chef.getUser().getId(),
                Instant.now().plus(3, ChronoUnit.DAYS),
                4,
                2,
                700.0));

        MvcResult programsResult = mockMvc.perform(get("/programs"))
                .andExpect(status().isOk())
                .andReturn();
        ProgramResponse[] programs = objectMapper.readValue(programsResult.getResponse().getContentAsString(), ProgramResponse[].class);
        assertThat(List.of(programs)).anyMatch(program -> program.getId().equals(createdProgram.getId()));

        ClientProfile client = createClient("client");
        String clientToken = loginToken(client.getUser().getEmail());
        BookingResponse booking = createBookingViaApi(clientToken, createdSlot.getSummary().getId(), EquipmentChoice.OWN);

        SlotSummaryResponse cancelledSlot = cancelSlotViaApi(adminToken, createdSlot.getSummary().getId(), "Chef unavailable");
        assertThat(cancelledSlot.getStatus()).isEqualTo(SlotStatus.CANCELLED_BY_STUDIO);
        assertThat(cancelledSlot.getCancellationReason()).isEqualTo("Chef unavailable");
        assertThat(emailService.slotCancellationMessages()).isNotEmpty();
        assertThat(emailService.slotCancellationMessages().get(0)).contains("Chef unavailable");

        Slot storedSlot = slotRepository.findById(createdSlot.getSummary().getId()).orElseThrow();
        Booking cancelledBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(storedSlot.getStatus()).isEqualTo(SlotStatus.CANCELLED_BY_STUDIO);
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED_BY_STUDIO);

        Slot pastSlot = createSlot(programRepository.findById(createdProgram.getId()).orElseThrow(), chef,
                Instant.now().minus(2, ChronoUnit.HOURS), 4, 0, null, SlotStatus.SCHEDULED);
        Booking pastBooking = createBooking(client, pastSlot, BookingStatus.CONFIRMED, EquipmentChoice.OWN);

        mockMvc.perform(post("/bookings/" + pastBooking.getId() + "/no-show")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(chef.getUser().getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));

        mockMvc.perform(get("/slots/" + pastSlot.getId() + "/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(loginToken(createChef("other-chef").getUser().getEmail()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/slots/" + pastSlot.getId() + "/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(pastBooking.getId().toString()));
    }
}
