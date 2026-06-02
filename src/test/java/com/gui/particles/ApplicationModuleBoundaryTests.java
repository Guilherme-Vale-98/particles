package com.gui.particles;

import com.gui.particles.article.application.NotificationArticleReadService;
import com.gui.particles.comment.application.NotificationCommentReadService;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.NamedInterface;
import org.springframework.modulith.core.ApplicationModules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationModuleBoundaryTests {

    private static final JavaClasses PROJECT_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
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

    @Test
    void notificationModuleDoesNotDependOnOtherModuleInternals() {
        noClasses()
                .that()
                .resideInAPackage("..notification..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..article.domain..",
                        "..article.api..",
                        "..comment.domain..",
                        "..comment.api..",
                        "..friendship.domain..",
                        "..friendship.api..",
                        "..reaction.domain..",
                        "..reaction.api..",
                        "..users.domain..",
                        "..users.api..",
                        "..feed.domain..",
                        "..feed.api.."
                )
                .because("notifications should consume other modules through application contracts and events only")
                .check(PROJECT_CLASSES);
    }

    @Test
    void notificationReadContractsAreExposedThroughNamedApplicationInterfaces() {
        assertApplicationNamedInterface(NotificationArticleReadService.class);
        assertApplicationNamedInterface(NotificationCommentReadService.class);
    }

    private void assertApplicationNamedInterface(Class<?> contractType) {
        NamedInterface namedInterface = contractType.getPackage().getAnnotation(NamedInterface.class);

        assertThat(namedInterface)
                .as("%s should live in a named application interface", contractType.getName())
                .isNotNull();
        assertThat(namedInterface.value()).containsExactly("application");
    }
}
