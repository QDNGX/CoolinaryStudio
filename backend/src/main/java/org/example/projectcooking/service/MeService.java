package org.example.projectcooking.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.ClientProfile;
import org.example.projectcooking.domain.User;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.dto.user.CurrentUserResponse;
import org.example.projectcooking.dto.user.UpdateMeRequest;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.UserMapper;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ClientProfileRepository;
import org.example.projectcooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Профиль текущего пользователя (GET/PATCH /me — SCR-08). */
@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ChefProfileRepository chefProfileRepository;
    private final UserMapper userMapper;

    @Transactional
    public CurrentUserResponse getMe(UUID userId) {
        return buildCurrentUser(load(userId));
    }

    /**
     * PATCH /me — клиент меняет только name (ФТ-21/Р-12) и allergyNote (ФТ-18). Применяются
     * Email и производные поля недоступны (Р-16). allergyNote можно очистить явным null.
     */
    @Transactional
    public CurrentUserResponse updateMe(UUID userId, UpdateMeRequest req) {
        User user = load(userId);
        if (req.isNameSet() && req.getName() != null) {
            user.setName(req.getName());
        }
        if (user.getRole() == UserRole.CLIENT && req.isAllergyNoteSet()) {
            clientProfileRepository.findById(userId).ifPresent(cp -> cp.setAllergyNote(req.getAllergyNote()));
        }
        return buildCurrentUser(user);
    }

    private CurrentUserResponse buildCurrentUser(User user) {
        Instant now = Instant.now();
        ClientProfile clientProfile = null;
        ChefProfile chefProfile = null;
        if (user.getRole() == UserRole.CLIENT) {
            clientProfile = clientProfileRepository.findById(user.getId()).orElse(null);
            if (clientProfile != null) {
                clientProfile.resetBlockIfExpired(now); // ленивый сброс блока (D-02, КП-5)
            }
        } else if (user.getRole() == UserRole.CHEF) {
            chefProfile = chefProfileRepository.findById(user.getId()).orElse(null);
        }
        return userMapper.toCurrentUser(user, clientProfile, chefProfile);
    }

    private User load(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
    }
}
