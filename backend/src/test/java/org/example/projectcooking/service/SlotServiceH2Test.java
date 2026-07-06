package org.example.projectcooking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.example.projectcooking.dto.slot.SlotCreateRequest;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.BookingMapper;
import org.example.projectcooking.mapper.ChefMapper;
import org.example.projectcooking.mapper.ProgramMapper;
import org.example.projectcooking.mapper.SlotMapper;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ClientProfileRepository;
import org.example.projectcooking.repository.ProgramRepository;
import org.example.projectcooking.repository.SlotRepository;
import org.example.projectcooking.repository.UserRepository;
import org.example.projectcooking.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SlotServiceH2Test {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ChefProfileRepository chefProfileRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ClientProfileRepository clientProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private SlotService service;
    private CapturingEmailService emailService;

    @BeforeEach
    void setUp() {
        SlotMapper slotMapper = new SlotMapper(new ChefMapper(), new ProgramMapper());
        emailService = new CapturingEmailService();
        service = new SlotService(
                slotRepository,
                programRepository,
                chefProfileRepository,
                bookingRepository,
                slotMapper,
                new BookingMapper(slotMapper),
                emailService);
    }

    @Test
    void createSlotRejectsCapacityAboveComplexProgramLimit() {
        Program program = createProgram(true);
        ChefProfile chef = createChef("chef");

        SlotCreateRequest request = new SlotCreateRequest();
        request.setProgramId(program.getId());
        request.setChefId(chef.getUserId());
        request.setStartAt(Instant.now().plus(2, ChronoUnit.DAYS));
        request.setCapacityTotal(SlotService.CAPACITY_COMPLEX + 1);
        request.setRentalSetsAvailable(0);

        assertThatThrownBy(() -> service.createSlot(request))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("CAPACITY_EXCEEDED"));
    }

    @Test
    void createSlotRejectsOverlappingChefSlot() {
        Program program = createProgram(false);
        ChefProfile chef = createChef("chef");
        Instant startAt = Instant.now().plus(2, ChronoUnit.DAYS);
        saveSlot(program, chef, startAt, SlotStatus.SCHEDULED);

        SlotCreateRequest request = new SlotCreateRequest();
        request.setProgramId(program.getId());
        request.setChefId(chef.getUserId());
        request.setStartAt(startAt.plus(60, ChronoUnit.MINUTES));
        request.setCapacityTotal(8);
        request.setRentalSetsAvailable(0);

        assertThatThrownBy(() -> service.createSlot(request))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("CHEF_SLOT_OVERLAP"));
    }

    @Test
    void cancelSlotMarksSlotAndConfirmedBookingsAsCancelledByStudio() {
        Program program = createProgram(false);
        ChefProfile chef = createChef("chef");
        ClientProfile client = createClient("client");
        Slot slot = saveSlot(program, chef, Instant.now().plus(2, ChronoUnit.DAYS), SlotStatus.SCHEDULED);
        Booking confirmed = saveBooking(client, slot, BookingStatus.CONFIRMED);
        Booking noShow = saveBooking(client, slot, BookingStatus.NO_SHOW);

        service.cancelSlot(slot.getId(), "Chef is unavailable");

        Slot updatedSlot = slotRepository.findById(slot.getId()).orElseThrow();
        Booking updatedConfirmed = bookingRepository.findById(confirmed.getId()).orElseThrow();
        Booking updatedNoShow = bookingRepository.findById(noShow.getId()).orElseThrow();
        assertThat(updatedSlot.getStatus()).isEqualTo(SlotStatus.CANCELLED_BY_STUDIO);
        assertThat(updatedSlot.getCancellationReason()).isEqualTo("Chef is unavailable");
        assertThat(updatedConfirmed.getStatus()).isEqualTo(BookingStatus.CANCELLED_BY_STUDIO);
        assertThat(updatedNoShow.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(emailService.messages)
                .singleElement()
                .satisfies(message -> assertThat(message)
                        .contains("|Program|Chef is unavailable")
                        .startsWith("client-"));
    }

    @Test
    void listSlotBookingsRejectsForeignChefAndAllowsAdmin() {
        Program program = createProgram(false);
        ChefProfile chef = createChef("chef");
        ChefProfile otherChef = createChef("other-chef");
        ClientProfile client = createClient("client");
        User admin = createUser("admin", UserRole.ADMIN);
        Slot slot = saveSlot(program, chef, Instant.now().plus(2, ChronoUnit.DAYS), SlotStatus.SCHEDULED);
        saveBooking(client, slot, BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> service.listSlotBookings(
                slot.getId(),
                new AuthPrincipal(otherChef.getUserId(), UserRole.CHEF)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("FORBIDDEN"));

        assertThat(service.listSlotBookings(slot.getId(), new AuthPrincipal(admin.getId(), UserRole.ADMIN)))
                .hasSize(1)
                .first()
                .satisfies(participant -> {
                    assertThat(participant.getClientName()).isEqualTo("Client client");
                    assertThat(participant.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
                });
    }

    private Slot saveSlot(Program program, ChefProfile chef, Instant startAt, SlotStatus status) {
        return slotRepository.save(Slot.builder()
                .program(program)
                .chef(chef)
                .startAt(startAt)
                .durationMinutes(Slot.FIXED_DURATION_MINUTES)
                .capacityTotal(8)
                .freeSpots(8)
                .status(status)
                .rentalSetsAvailable(0)
                .build());
    }

    private Booking saveBooking(ClientProfile client, Slot slot, BookingStatus status) {
        return bookingRepository.save(Booking.builder()
                .client(client)
                .slot(slot)
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .status(status)
                .equipmentChoice(EquipmentChoice.OWN)
                .build());
    }

    private Program createProgram(boolean complex) {
        return programRepository.save(Program.builder()
                .title("Program")
                .description("Description")
                .cuisineType("Italian")
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .requiresComplexEquipment(complex)
                .dishes(new ArrayList<>(List.of("Pasta")))
                .photos(new ArrayList<>())
                .build());
    }

    private ClientProfile createClient(String label) {
        User user = createUser(label, UserRole.CLIENT);
        return clientProfileRepository.save(ClientProfile.builder().user(user).build());
    }

    private ChefProfile createChef(String label) {
        User user = createUser(label, UserRole.CHEF);
        return chefProfileRepository.save(ChefProfile.builder().user(user).build());
    }

    private User createUser(String label, UserRole role) {
        return userRepository.save(User.builder()
                .email(label + "-" + UUID.randomUUID() + "@test.local")
                .name(role == UserRole.CLIENT ? "Client " + label : "User " + label)
                .role(role)
                .enabled(true)
                .build());
    }

    private static class CapturingEmailService implements EmailService {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendLoginCode(String email, String code) {
        }

        @Override
        public void sendSlotCancellation(String email, String programTitle, String reason) {
            messages.add(email + "|" + programTitle + "|" + reason);
        }

        @Override
        public void sendReminder24h(String email, String programTitle, Instant startAt) {
        }

        @Override
        public void sendReminder2h(String email, String programTitle, Instant startAt) {
        }
    }
}
