package com.gui.particles.users.api;

import com.gui.particles.users.application.UserProfileLinkingService;
import com.gui.particles.users.application.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileLinkingService userProfileLinkingService;

    public UserProfileController(
            UserProfileService userProfileService,
            UserProfileLinkingService userProfileLinkingService
    ) {
        this.userProfileService = userProfileService;
        this.userProfileLinkingService = userProfileLinkingService;
    }

    @GetMapping("/{username}")
    public UserProfileResponse getByUsername(@PathVariable String username) {
        return userProfileService.getByUsername(username);
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUserProfile() {
        return userProfileLinkingService.getCurrentUserProfile();
    }

    @PutMapping("/me")
    public UserProfileResponse updateCurrentUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateCurrentUserProfile(request);
    }

}
