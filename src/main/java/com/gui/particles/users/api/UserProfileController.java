package com.gui.particles.users.api;

import com.gui.particles.users.application.UserProfileLinkingService;
import com.gui.particles.users.application.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Read public profiles and manage the current authenticated user's profile.")
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
    @Operation(summary = "Get a public user profile", description = "Returns a public user profile by username.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned"),
            @ApiResponse(responseCode = "404", description = "User profile not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public UserProfileResponse getByUsername(@PathVariable String username) {
        return userProfileService.getByUsername(username);
    }

    @GetMapping("/me")
    @Operation(summary = "Get or initialize current user profile", description = "Returns the current user's profile, creating the provider identity mapping and initial profile if needed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user profile returned"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public UserProfileResponse getCurrentUserProfile() {
        return userProfileLinkingService.getCurrentUserProfile();
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates the current authenticated user's profile fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user profile updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "404", description = "Current user profile not found"),
            @ApiResponse(responseCode = "409", description = "Username is already taken"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public UserProfileResponse updateCurrentUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateCurrentUserProfile(request);
    }

}
