package com.gui.particles.common.security;

import java.util.UUID;

public interface CurrentUserProvider {

    UUID currentUserId();

    default CurrentUser currentUser() {
        return new CurrentUser(currentUserId());
    }
}
