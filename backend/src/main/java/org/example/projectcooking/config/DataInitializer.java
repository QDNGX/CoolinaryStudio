package org.example.projectcooking.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.ClientProfile;
import org.example.projectcooking.domain.Program;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.StudioSettings;
import org.example.projectcooking.domain.User;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.DifficultyLevel;
import org.example.projectcooking.domain.enums.EquipmentChoice;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ClientProfileRepository;
import org.example.projectcooking.repository.ProgramRepository;
import org.example.projectcooking.repository.SlotRepository;
import org.example.projectcooking.repository.StudioSettingsRepository;
import org.example.projectcooking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Стартовые данные для локального запуска/проверки: синглтон настроек, ADMIN (для admin-эндпоинтов —
 * D-08: chef/admin не создаются самовходом), демо-шеф/программа/слоты. Отдельный COMPLETED-слот с
 * COMPLETED-бронью — чтобы можно было проверить сценарий отзыва (D-05) при отложенном автозавершении.
 * Идемпотентно: фиксированные демо-пользователи чинятся при каждом старте, демо-контент не дублируется.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ChefProfileRepository chefProfileRepository;
    private final StudioSettingsRepository studioSettingsRepository;
    private final ProgramRepository programRepository;
    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!studioSettingsRepository.existsById(StudioSettings.SINGLETON_ID)) {
            studioSettingsRepository.save(StudioSettings.builder()
                    .id(StudioSettings.SINGLETON_ID)
                    .address("Москва, ул. Гастрономическая, 1")
                    .contactPhone("+7 495 000-00-00")
                    .contactEmail("hello@chefstol.local")
                    .build());
        }

        User admin = ensureUser("admin@chefstol.local", "Администратор", UserRole.ADMIN);
        User chefUser = ensureUser("chef@chefstol.local", "Шеф Иван", UserRole.CHEF);
        ChefProfile chef = ensureChefProfile(chefUser);
        User clientUser = ensureUser("client@chefstol.local", "Гость Пётр", UserRole.CLIENT);
        ClientProfile client = ensureClientProfile(clientUser);

        Program program = ensureProgram();

        if (slotRepository.count() > 0) {
            log.info("Seed готов: admin={}, chef={}, client={} (роли проверены, демо-слоты уже есть)",
                    admin.getEmail(), chefUser.getEmail(), clientUser.getEmail());
            return;
        }

        Instant now = Instant.now();
        slotRepository.save(Slot.builder()
                .program(program).chef(chef)
                .startAt(now.plus(3, ChronoUnit.DAYS))
                .durationMinutes(Slot.FIXED_DURATION_MINUTES)
                .capacityTotal(12).freeSpots(12)
                .status(SlotStatus.SCHEDULED)
                .rentalSetsAvailable(4).rentalPricePerSet(500.0)
                .build());

        Slot completed = slotRepository.save(Slot.builder()
                .program(program).chef(chef)
                .startAt(now.minus(1, ChronoUnit.DAYS))
                .durationMinutes(Slot.FIXED_DURATION_MINUTES)
                .capacityTotal(8).freeSpots(7)
                .status(SlotStatus.COMPLETED)
                .rentalSetsAvailable(0)
                .build());
        bookingRepository.save(Booking.builder()
                .client(client).slot(completed)
                .createdAt(now.minus(3, ChronoUnit.DAYS))
                .status(BookingStatus.COMPLETED)
                .equipmentChoice(EquipmentChoice.OWN)
                .build());

        log.info("Seed готов: admin={}, chef={}, client={} (коды входа печатаются в лог при /auth/code/request)",
                admin.getEmail(), chefUser.getEmail(), clientUser.getEmail());
    }

    private User ensureUser(String email, String name, UserRole role) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> User.builder()
                        .email(email)
                        .name(name)
                        .role(role)
                        .enabled(true)
                        .build());
        user.setName(name);
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private ChefProfile ensureChefProfile(User chefUser) {
        ChefProfile chef = chefProfileRepository.findById(chefUser.getId())
                .orElseGet(() -> ChefProfile.builder().user(chefUser).build());
        chef.setUser(chefUser);
        if (chef.getBio() == null || chef.getBio().isBlank()) {
            chef.setBio("Итальянская кухня, 10 лет практики");
        }
        return chefProfileRepository.save(chef);
    }

    private ClientProfile ensureClientProfile(User clientUser) {
        ClientProfile client = clientProfileRepository.findById(clientUser.getId())
                .orElseGet(() -> ClientProfile.builder().user(clientUser).build());
        client.setUser(clientUser);
        return clientProfileRepository.save(client);
    }

    private Program ensureProgram() {
        return programRepository.findAllByOrderByTitleAsc().stream()
                .filter(program -> "Паста с нуля".equalsIgnoreCase(program.getTitle()))
                .findFirst()
                .orElseGet(() -> programRepository.save(Program.builder()
                        .title("Паста с нуля")
                        .description("Готовим свежую пасту и два соуса.")
                        .cuisineType("Итальянская")
                        .difficultyLevel(DifficultyLevel.BEGINNER)
                        .requiresComplexEquipment(false)
                        .dishes(new ArrayList<>(List.of("Тальятелле", "Рагу болоньезе")))
                        .photos(new ArrayList<>())
                        .build()));
    }
}
