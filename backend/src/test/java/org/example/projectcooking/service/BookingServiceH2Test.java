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
import org.example.projectcooking.dto.booking.BookingCreateRequest;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.BookingMapper;
import org.example.projectcooking.mapper.ChefMapper;
import org.example.projectcooking.mapper.ProgramMapper;
import org.example.projectcooking.mapper.SlotMapper;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ClientProfileRepository;
import org.example.projectcooking.repository.ProgramRepository;
import org.example.projectcooking.repository.ReviewRepository;
import org.example.projectcooking.repository.SlotRepository;
import org.example.projectcooking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Transactional
class BookingServiceH2Test {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ClientProfileRepository clientProfileRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChefProfileRepository chefProfileRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private BookingService service;

    @BeforeEach
    void setUp() {
        BookingMapper bookingMapper = new BookingMapper(new SlotMapper(new ChefMapper(), new ProgramMapper()));
        service = new BookingService(
                bookingRepository,
                slotRepository,
                clientProfileRepository,
                reviewRepository,
                bookingMapper,
                new TransactionTemplate(transactionManager));
    }

    @Test
    void createBookingWithOwnEquipmentDecreasesFreeSpots() {
        Fixture fixture = createFixture(Instant.now().plus(2, ChronoUnit.DAYS), 3, 2, 500.0);

        BookingResponse response = service.createBooking(
                fixture.client().getUserId(),
                new BookingCreateRequest(fixture.slot().getId(), EquipmentChoice.OWN));

        Slot slot = slotRepository.findById(fixture.slot().getId()).orElseThrow();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.getRentalPriceSnapshot()).isNull();
        assertThat(slot.getFreeSpots()).isEqualTo(2);
        assertThat(slot.getRentalSetsAvailable()).isEqualTo(2);
    }

    @Test
    void createBookingWithRentalDecreasesRentalSetsAndStoresPriceSnapshot() {
        Fixture fixture = createFixture(Instant.now().plus(2, ChronoUnit.DAYS), 3, 2, 750.0);

        BookingResponse response = service.createBooking(
                fixture.client().getUserId(),
                new BookingCreateRequest(fixture.slot().getId(), EquipmentChoice.RENTAL));

        Slot slot = slotRepository.findById(fixture.slot().getId()).orElseThrow();
        assertThat(response.getRentalPriceSnapshot()).isEqualTo(750.0);
        assertThat(slot.getFreeSpots()).isEqualTo(2);
        assertThat(slot.getRentalSetsAvailable()).isEqualTo(1);
    }

    @Test
    void cancelBookingReturnsSeatAndRentalSet() {
        Fixture fixture = createFixture(Instant.now().plus(2, ChronoUnit.DAYS), 3, 2, 600.0);
        Booking booking = saveBooking(fixture.client(), fixture.slot(), BookingStatus.CONFIRMED, EquipmentChoice.RENTAL);
        fixture.slot().setFreeSpots(2);
        fixture.slot().setRentalSetsAvailable(1);
        slotRepository.save(fixture.slot());

        BookingResponse response = service.cancelBooking(fixture.client().getUserId(), booking.getId());

        Slot slot = slotRepository.findById(fixture.slot().getId()).orElseThrow();
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED_BY_CLIENT);
        assertThat(updated.getCancelledAt()).isNotNull();
        assertThat(slot.getFreeSpots()).isEqualTo(3);
        assertThat(slot.getRentalSetsAvailable()).isEqualTo(2);
    }

    @Test
    void lateCancellationMarksBookingAndRegistersViolation() {
        Fixture fixture = createFixture(Instant.now().plus(1, ChronoUnit.HOURS), 3, 0, null);
        Booking booking = saveBooking(fixture.client(), fixture.slot(), BookingStatus.CONFIRMED, EquipmentChoice.OWN);

        BookingResponse response = service.cancelBooking(fixture.client().getUserId(), booking.getId());

        ClientProfile client = clientProfileRepository.findById(fixture.client().getUserId()).orElseThrow();
        assertThat(response.getIsLateCancellation()).isTrue();
        assertThat(client.getLateCancelCount()).isEqualTo(1);
    }

    @Test
    void cancelBookingRejectsOtherClient() {
        Fixture fixture = createFixture(Instant.now().plus(2, ChronoUnit.DAYS), 3, 0, null);
        ClientProfile otherClient = createClient("other");
        Booking booking = saveBooking(fixture.client(), fixture.slot(), BookingStatus.CONFIRMED, EquipmentChoice.OWN);

        assertThatThrownBy(() -> service.cancelBooking(otherClient.getUserId(), booking.getId()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("FORBIDDEN"));
    }

    @Test
    void markNoShowRequiresOwnChefAndStartedClass() {
        Fixture fixture = createFixture(Instant.now().minus(1, ChronoUnit.HOURS), 3, 0, null);
        Booking booking = saveBooking(fixture.client(), fixture.slot(), BookingStatus.CONFIRMED, EquipmentChoice.OWN);
        ChefProfile otherChef = createChef("other-chef");

        assertThatThrownBy(() -> service.markNoShow(otherChef.getUserId(), booking.getId()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("FORBIDDEN"));

        service.markNoShow(fixture.chef().getUserId(), booking.getId());

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        ClientProfile client = clientProfileRepository.findById(fixture.client().getUserId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(client.getLateCancelCount()).isEqualTo(1);
    }

    @Test
    void markNoShowRejectsFutureClass() {
        Fixture fixture = createFixture(Instant.now().plus(1, ChronoUnit.HOURS), 3, 0, null);
        Booking booking = saveBooking(fixture.client(), fixture.slot(), BookingStatus.CONFIRMED, EquipmentChoice.OWN);

        assertThatThrownBy(() -> service.markNoShow(fixture.chef().getUserId(), booking.getId()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getCode()).isEqualTo("CLASS_NOT_STARTED"));
    }

    private Fixture createFixture(Instant startAt, int capacity, int rentalSets, Double rentalPrice) {
        ClientProfile client = createClient("client");
        ChefProfile chef = createChef("chef");
        Program program = programRepository.save(Program.builder()
                .title("Program " + UUID.randomUUID())
                .description("Description")
                .cuisineType("Italian")
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .requiresComplexEquipment(false)
                .dishes(new ArrayList<>(List.of("Pasta")))
                .photos(new ArrayList<>())
                .build());
        Slot slot = slotRepository.save(Slot.builder()
                .program(program)
                .chef(chef)
                .startAt(startAt)
                .durationMinutes(Slot.FIXED_DURATION_MINUTES)
                .capacityTotal(capacity)
                .freeSpots(capacity)
                .status(SlotStatus.SCHEDULED)
                .rentalSetsAvailable(rentalSets)
                .rentalPricePerSet(rentalPrice)
                .build());
        return new Fixture(client, chef, slot);
    }

    private Booking saveBooking(ClientProfile client, Slot slot, BookingStatus status, EquipmentChoice equipmentChoice) {
        return bookingRepository.save(Booking.builder()
                .client(client)
                .slot(slot)
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .status(status)
                .equipmentChoice(equipmentChoice)
                .rentalPriceSnapshot(equipmentChoice == EquipmentChoice.RENTAL ? slot.getRentalPricePerSet() : null)
                .build());
    }

    private ClientProfile createClient(String label) {
        User user = userRepository.save(User.builder()
                .email(label + "-" + UUID.randomUUID() + "@test.local")
                .name("Client " + label)
                .role(UserRole.CLIENT)
                .enabled(true)
                .build());
        return clientProfileRepository.save(ClientProfile.builder().user(user).build());
    }

    private ChefProfile createChef(String label) {
        User user = userRepository.save(User.builder()
                .email(label + "-" + UUID.randomUUID() + "@test.local")
                .name("Chef " + label)
                .role(UserRole.CHEF)
                .enabled(true)
                .build());
        return chefProfileRepository.save(ChefProfile.builder().user(user).build());
    }

    private record Fixture(ClientProfile client, ChefProfile chef, Slot slot) {
    }
}
