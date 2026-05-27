package com.gui.particles.users.application;

import com.gui.particles.users.domain.UserProfile;
import com.gui.particles.users.domain.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileReadServiceTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileReadServiceImpl userProfileReadService;

    @Test
    void findsProfileSummaryByUsername() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.create(
                userId,
                "alice",
                "Alice Example",
                "Bio",
                "https://example.com/alice.png"
        );
        when(userProfileRepository.findByUsername("alice")).thenReturn(Optional.of(profile));

        Optional<UserProfileSummary> summary = userProfileReadService.findSummaryByUsername("alice");

        assertThat(summary).contains(new UserProfileSummary(
                userId,
                "alice",
                "Alice Example",
                "https://example.com/alice.png"
        ));
    }

    @Test
    void findsProfileSummariesByIds() {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        UserProfile alice = UserProfile.create(aliceId, "alice", "Alice Example", null, null);
        UserProfile bob = UserProfile.create(bobId, "bob", "Bob Example", null, "https://example.com/bob.png");
        when(userProfileRepository.findAllById(List.of(aliceId, bobId))).thenReturn(List.of(alice, bob));

        List<UserProfileSummary> summaries = userProfileReadService.findSummariesByIds(List.of(aliceId, bobId));

        assertThat(summaries).containsExactly(
                new UserProfileSummary(aliceId, "alice", "Alice Example", null),
                new UserProfileSummary(bobId, "bob", "Bob Example", "https://example.com/bob.png")
        );
    }

    @Test
    void checksProfileExistenceById() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(true);

        assertThat(userProfileReadService.existsById(userId)).isTrue();
    }
}
