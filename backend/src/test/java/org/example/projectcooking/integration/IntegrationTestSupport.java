package org.example.projectcooking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
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
import org.example.projectcooking.dto.auth.AuthCodeRequest;
import org.example.projectcooking.dto.auth.AuthCodeVerify;
import org.example.projectcooking.dto.auth.AuthResult;
import org.example.projectcooking.dto.booking.BookingCreateRequest;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.example.projectcooking.dto.program.ProgramInput;
import org.example.projectcooking.dto.program.ProgramResponse;
import org.example.projectcooking.dto.slot.SlotCancelRequest;
import org.example.projectcooking.dto.slot.SlotCreateRequest;
import org.example.projectcooking.dto.slot.SlotDetailsResponse;
import org.example.projectcooking.dto.slot.SlotSummaryResponse;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ClientProfileRepository;
import org.example.projectcooking.repository.ProgramRepository;
import org.example.projectcooking.repository.SlotRepository;
import org.example.projectcooking.repository.UserRepository;
import org.example.projectcooking.service.TokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

abstract class IntegrationTestSupport {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ClientProfileRepository clientProfileRepository;

    @Autowired
    protected ChefProfileRepository chefProfileRepository;

    @Autowired
    protected ProgramRepository programRepository;

    @Autowired
    protected SlotRepository slotRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected TokenStore tokenStore;

    @Autowired
    protected IntegrationTestConfig.RecordingEmailService emailService;

    @BeforeEach
    void clearRecordedEmails() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        emailService.clear();
    }

    protected AuthResult authenticate(String email) throws Exception {
        requestLoginCode(email);
        String code = emailService.loginCodeFor(email);
        assertThat(code).as("login code for %s", email).isNotBlank();

        MvcResult result = mockMvc.perform(post("/auth/code/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AuthCodeVerify(email, code))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResult.class);
    }

    protected String loginToken(String email) throws Exception {
        return authenticate(email).getAccessToken();
    }

    protected void requestLoginCode(String email) throws Exception {
        mockMvc.perform(post("/auth/code/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AuthCodeRequest(email))))
                .andExpect(status().isAccepted());
    }

    protected User createUser(String email, UserRole role, String name) {
        return userRepository.save(User.builder()
                .email(email)
                .name(name)
                .role(role)
                .enabled(true)
                .build());
    }

    protected ClientProfile createClient(String label) {
        String email = label + "-" + UUID.randomUUID() + "@test.local";
        User user = createUser(email, UserRole.CLIENT, "Client " + label);
        return clientProfileRepository.save(ClientProfile.builder().user(user).build());
    }

    protected ChefProfile createChef(String label) {
        String email = label + "-" + UUID.randomUUID() + "@test.local";
        User user = createUser(email, UserRole.CHEF, "Chef " + label);
        return chefProfileRepository.save(ChefProfile.builder()
                .user(user)
                .bio("Bio " + label)
                .build());
    }

    protected User createAdmin(String label) {
        String email = label + "-" + UUID.randomUUID() + "@test.local";
        return createUser(email, UserRole.ADMIN, "Admin " + label);
    }

    protected Program createProgram(String title) {
        return programRepository.save(Program.builder()
                .title(title)
                .description("Description for " + title)
                .cuisineType("Italian")
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .requiresComplexEquipment(false)
                .dishes(new ArrayList<>(List.of("Pasta")))
                .photos(new ArrayList<>())
                .build());
    }

    protected Slot createSlot(Program program, ChefProfile chef, Instant startAt, int capacityTotal,
                              int rentalSetsAvailable, Double rentalPricePerSet, SlotStatus status) {
        return slotRepository.save(Slot.builder()
                .program(program)
                .chef(chef)
                .startAt(startAt)
                .durationMinutes(Slot.FIXED_DURATION_MINUTES)
                .capacityTotal(capacityTotal)
                .freeSpots(capacityTotal)
                .status(status)
                .rentalSetsAvailable(rentalSetsAvailable)
                .rentalPricePerSet(rentalPricePerSet)
                .build());
    }

    protected Booking createBooking(ClientProfile client, Slot slot, BookingStatus status, EquipmentChoice equipmentChoice) {
        return bookingRepository.save(Booking.builder()
                .client(client)
                .slot(slot)
                .createdAt(Instant.now().minusSeconds(3600))
                .status(status)
                .equipmentChoice(equipmentChoice)
                .rentalPriceSnapshot(equipmentChoice == EquipmentChoice.RENTAL ? slot.getRentalPricePerSet() : null)
                .build());
    }

    protected ProgramResponse createProgramViaApi(String token, ProgramInput input) throws Exception {
        MvcResult result = mockMvc.perform(post("/programs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(input)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ProgramResponse.class);
    }

    protected SlotDetailsResponse createSlotViaApi(String token, SlotCreateRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/slots")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), SlotDetailsResponse.class);
    }

    protected BookingResponse createBookingViaApi(String token, UUID slotId, EquipmentChoice choice) throws Exception {
        MvcResult result = mockMvc.perform(post("/bookings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new BookingCreateRequest(slotId, choice))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);
    }

    protected SlotSummaryResponse cancelSlotViaApi(String token, UUID slotId, String reason) throws Exception {
        MvcResult result = mockMvc.perform(post("/slots/" + slotId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SlotCancelRequest(reason))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), SlotSummaryResponse.class);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
