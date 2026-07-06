package org.example.projectcooking.mapper;

import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.ClientProfile;
import org.example.projectcooking.domain.User;
import org.example.projectcooking.dto.user.ChefProfileResponse;
import org.example.projectcooking.dto.user.ClientProfileResponse;
import org.example.projectcooking.dto.user.CurrentUserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    /** clientProfile присутствует только для CLIENT, chefProfile — только для CHEF. */
    public CurrentUserResponse toCurrentUser(User u, ClientProfile clientProfile, ChefProfile chefProfile) {
        CurrentUserResponse.CurrentUserResponseBuilder b = CurrentUserResponse.builder()
                .id(u.getId())
                .role(u.getRole())
                .name(u.getName())
                .email(u.getEmail())
                .enabled(u.isEnabled())
                .createdAt(u.getCreatedAt());

        if (clientProfile != null) {
            b.clientProfile(ClientProfileResponse.builder()
                    .allergyNote(clientProfile.getAllergyNote())
                    .lateCancelCount(clientProfile.getLateCancelCount())
                    .blockedUntil(clientProfile.getBlockedUntil())
                    .build());
        }
        if (chefProfile != null) {
            b.chefProfile(ChefProfileResponse.builder()
                    .photo(chefProfile.getPhoto())
                    .bio(chefProfile.getBio())
                    .averageRating(chefProfile.getAverageRating())
                    .reviewsCount(chefProfile.getReviewsCount())
                    .build());
        }
        return b.build();
    }
}
