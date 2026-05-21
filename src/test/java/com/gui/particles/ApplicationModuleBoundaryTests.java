package com.gui.particles;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationModuleBoundaryTests {

    @Test
    void verifiesApplicationModuleBoundaries() {
        ApplicationModules modules = ApplicationModules.of(ParticlesApplication.class);

        modules.verify();

        assertThat(modules.getModuleByName("common"))
                .hasValueSatisfying(module -> assertThat(module.isOpen()).isTrue());
    }
}
