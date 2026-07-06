package org.example.projectcooking.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.Review;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.dto.review.ReviewCreateRequest;
import org.example.projectcooking.dto.review.ReviewResponse;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.ReviewMapper;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Отзывы по завершённым броням (UC-04, D-05, ФТ-11/12, Р-07). */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final ChefProfileRepository chefProfileRepository;
    private final ReviewMapper reviewMapper;

    /**
     * Создание отзыва (UC-04). Проверки D-05: бронь клиента → COMPLETED и не отменена → нет отзыва.
     * После создания пересчитывается агрегат рейтинга шефа (ФТ-12).
     */
    @Transactional
    public ReviewResponse create(UUID clientId, UUID bookingId, ReviewCreateRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Бронь не найдена"));
        if (!booking.getClient().getUserId().equals(clientId)) {
            throw ApiException.forbidden("Бронь принадлежит другому клиенту");
        }
        if (reviewRepository.existsByBooking_Id(bookingId)) {
            throw ApiException.conflict("REVIEW_ALREADY_EXISTS", "Отзыв на эту бронь уже существует");
        }
        Slot slot = booking.getSlot();
        if (slot.getStatus() != SlotStatus.COMPLETED || booking.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.unprocessable("REVIEW_NOT_AVAILABLE",
                    "Отзыв доступен только по завершённому классу с неотменённой бронью");
        }

        Review review = reviewRepository.save(Review.builder()
                .booking(booking)
                .chefRating(req.getChefRating())
                .programRating(req.getProgramRating())
                .comment(req.getComment())
                .build());

        recomputeChefAggregate(slot.getChef());
        return reviewMapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getByBooking(UUID clientId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Бронь не найдена"));
        if (!booking.getClient().getUserId().equals(clientId)) {
            throw ApiException.forbidden("Бронь принадлежит другому клиенту");
        }
        Review review = reviewRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> ApiException.notFound("Отзыв не найден"));
        return reviewMapper.toResponse(review);
    }

    /** Все отзывы с текстами — только ADMIN (Р-07). Фильтры по шефу/программе. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> listReviews(UUID chefId, UUID programId) {
        List<Review> reviews;
        if (chefId != null) {
            reviews = reviewRepository.findByBooking_Slot_Chef_UserIdOrderByCreatedAtDesc(chefId);
        } else if (programId != null) {
            reviews = reviewRepository.findByBooking_Slot_Program_IdOrderByCreatedAtDesc(programId);
        } else {
            reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        }
        return reviews.stream().map(reviewMapper::toResponse).toList();
    }

    /** Агрегат рейтинга шефа — среднее Review.chefRating и число отзывов (ФТ-12). */
    private void recomputeChefAggregate(ChefProfile chef) {
        List<Review> reviews = reviewRepository.findByBooking_Slot_Chef_UserId(chef.getUserId());
        chef.setReviewsCount(reviews.size());
        chef.setAverageRating(reviews.isEmpty()
                ? null
                : reviews.stream().mapToInt(Review::getChefRating).average().orElse(0));
        chefProfileRepository.save(chef);
    }
}
