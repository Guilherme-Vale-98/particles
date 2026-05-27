package com.gui.particles.friendship.application;

import java.util.List;
import java.util.UUID;

public interface FriendshipReadService {

    List<UUID> acceptedFriendIds(UUID userId);
}
