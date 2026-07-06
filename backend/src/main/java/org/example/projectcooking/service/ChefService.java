package org.example.projectcooking.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.User;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.dto.chef.ChefCreateRequest;
import org.example.projectcooking.dto.chef.ChefResponse;
import org.example.projectcooking.dto.chef.ChefUpdateRequest;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.ChefMapper;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Аккаунты и профили шефов — ведёт ADMIN (UC-08, ФТ-14/25, D-08). */
@Service
@RequiredArgsConstructor
public class ChefService {

    private final UserRepository userRepository;
    private final ChefProfileRepository chefProfileRepository;
    private final ChefMapper chefMapper;

    @Transactional(readOnly = true)
    public List<ChefResponse> list() {
        return chefProfileRepository.findAllByOrderByUser_CreatedAtAsc().stream()
                .map(chefMapper::toChefResponse).toList();
    }

    /** Заведение аккаунта шефа по email (D-08); пароля нет — первый вход по коду. */
    @Transactional
    public ChefResponse create(ChefCreateRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw ApiException.conflict("EMAIL_TAKEN", "Пользователь с таким email уже существует");
        }
        User user = userRepository.save(User.builder()
                .role(UserRole.CHEF)
                .email(req.getEmail())
                .name(req.getName())
                .enabled(true)
                .build());
        ChefProfile chef = chefProfileRepository.save(ChefProfile.builder()
                .user(user)
                .photo(req.getPhoto())
                .bio(req.getBio())
                .build());
        return chefMapper.toChefResponse(chef);
    }

    /** Редактирование фото/био (Р-05). Агрегаты рейтинга недоступны (ФТ-12 — только система). */
    @Transactional
    public ChefResponse update(UUID chefId, ChefUpdateRequest req) {
        ChefProfile chef = chefProfileRepository.findById(chefId)
                .orElseThrow(() -> ApiException.notFound("Шеф не найден"));
        chef.setPhoto(req.getPhoto());
        chef.setBio(req.getBio());
        return chefMapper.toChefResponse(chefProfileRepository.save(chef));
    }
}
