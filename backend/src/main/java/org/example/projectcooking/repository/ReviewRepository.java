package org.example.projectcooking.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.projectcooking.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByBooking_Id(UUID bookingId);

    Optional<Review> findByBooking_Id(UUID bookingId);

    /** Все отзывы (ADMIN, с текстами — Р-07), от новых к старым. */
    List<Review> findAllByOrderByCreatedAtDesc();

    /** Фильтр по шефу через путь Review → Booking → Slot → ChefProfile. */
    List<Review> findByBooking_Slot_Chef_UserIdOrderByCreatedAtDesc(UUID chefId);

    /** Фильтр по программе через путь Review → Booking → Slot → Program. */
    List<Review> findByBooking_Slot_Program_IdOrderByCreatedAtDesc(UUID programId);

    /** Все отзывы по шефу — для пересчёта агрегата рейтинга (ФТ-12). */
    List<Review> findByBooking_Slot_Chef_UserId(UUID chefId);
}
