package com.gui.particles;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationModuleBoundaryTests {

    private static final JavaClasses PROJECT_CLASSES = new ClassFileImporter()
            .importPackages("com.gui.particles");

    @Test
    void verifiesApplicationModuleBoundaries() {
        ApplicationModules modules = ApplicationModules.of(ParticlesApplication.class);

        modules.verify();

        assertThat(modules.getModuleByName("common"))
                .hasValueSatisfying(module -> assertThat(module.isOpen()).isTrue());
    }

    @Test
    void commentModuleDoesNotDependOnArticleInternals() {
        noClasses()
                .that()
                .resideInAPackage("..comment..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..article.domain..", "..article.api..")
                .because("comments should use the comment article read port instead of article entities, repositories, or DTOs")
                .check(PROJECT_CLASSES);
    }
}
