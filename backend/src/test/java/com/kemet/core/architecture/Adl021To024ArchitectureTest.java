package com.kemet.core.architecture;

import com.kemet.core.companion.CompanionService;
import com.kemet.core.config.SeedDataLoader;
import com.kemet.core.repository.FacultyContentRepository;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Adl021To024ArchitectureTest {

    private static final String USER_SERVICE = "com.kemet.core.user.UserService";
    private static final String APP_USER_REPOSITORY = "com.kemet.core.repository.AppUserRepository";

    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.kemet.core");

    @Test
    void adl021_noServerSideJournalComponentOrEndpointNames() {
        List<String> matchingClassNames = IMPORTED_CLASSES.stream()
                .map(JavaClass::getFullName)
                .filter(name -> containsJournal(name))
                .toList();

        assertThat(matchingClassNames)
                .withFailMessage("ADL-021 violated: found server-side class/package names containing 'Journal': %s",
                        matchingClassNames)
                .isEmpty();

        List<String> matchingEndpointPaths = IMPORTED_CLASSES.stream()
                .map(JavaClass::reflect)
                .filter(type -> type.isAnnotationPresent(RestController.class))
                .flatMap(type -> endpointPaths(type).stream())
                .filter(Adl021To024ArchitectureTest::containsJournal)
                .toList();

        assertThat(matchingEndpointPaths)
                .withFailMessage("ADL-021 violated: found REST endpoint paths containing 'Journal': %s",
                        matchingEndpointPaths)
                .isEmpty();
    }

    @Test
    void adl022_onlyUserServiceMayReferenceAppUserRepository() {
        List<String> violations = IMPORTED_CLASSES.stream()
                .filter(sourceClass -> sourceClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> dependency.getTargetClass().getFullName().equals(APP_USER_REPOSITORY)))
                .filter(sourceClass -> !sourceClass.getFullName().equals(USER_SERVICE))
                .map(JavaClass::getFullName)
                .toList();

        assertThat(violations)
                .withFailMessage("ADL-022 violated: AppUserRepository is referenced outside %s: %s",
                        USER_SERVICE, violations)
                .isEmpty();
    }

    @Test
    void adl023_onlySeedDataLoaderMayCallFacultyContentRepositoryWriteMethods() {
        Set<String> writeMethodNames = Set.of("save", "saveAll", "delete", "deleteAll", "deleteById", "deleteAllById");
        List<String> violations = new ArrayList<>();

        for (JavaClass sourceClass : IMPORTED_CLASSES) {
            sourceClass.getMethodCallsFromSelf().forEach(call -> {
                if (!call.getTargetOwner().getFullName().equals(FacultyContentRepository.class.getName())) {
                    return;
                }
                if (!writeMethodNames.contains(call.getName())) {
                    return;
                }
                if (!call.getOriginOwner().getFullName().equals(SeedDataLoader.class.getName())) {
                    violations.add(call.getOriginOwner().getFullName() + " -> "
                            + call.getTargetOwner().getSimpleName() + "." + call.getName());
                }
            });
        }

        assertThat(violations)
                .withFailMessage("ADL-023 violated: FacultyContentRepository write methods may only be called from %s but found: %s",
                        SeedDataLoader.class.getName(), violations)
                .isEmpty();
    }

    @Test
    void adl024_onlyCompanionServiceMayUseOpenAiHttpClientAndEndpoint() throws IOException {
        List<String> httpClientViolations = IMPORTED_CLASSES.stream()
                .filter(sourceClass -> sourceClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> dependency.getTargetClass().getFullName().equals(HttpClient.class.getName())))
                .filter(sourceClass -> !sourceClass.getFullName().equals(CompanionService.class.getName()))
                .map(JavaClass::getFullName)
                .toList();

        assertThat(httpClientViolations)
                .withFailMessage("ADL-024 violated: HttpClient usage outside %s: %s",
                        CompanionService.class.getName(), httpClientViolations)
                .isEmpty();

        Path coreSources = Path.of(System.getProperty("user.dir"), "src", "main", "java", "com", "kemet", "core");
        List<Path> filesContainingOpenAiHost;
        try (var paths = Files.walk(coreSources)) {
            filesContainingOpenAiHost = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("api.openai.com");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }

        assertThat(filesContainingOpenAiHost)
                .withFailMessage("ADL-024 violated: 'api.openai.com' must appear only in CompanionService, found in %s",
                        filesContainingOpenAiHost)
                .containsExactly(coreSources.resolve(Path.of("companion", "CompanionService.java")));
    }

    private static boolean containsJournal(String value) {
        return value.toLowerCase(Locale.ROOT).contains("journal");
    }

    private static Set<String> endpointPaths(Class<?> controllerType) {
        Set<String> paths = new LinkedHashSet<>();
        addPaths(paths, controllerType.getAnnotation(RequestMapping.class));

        for (var method : controllerType.getDeclaredMethods()) {
            addPaths(paths, method.getAnnotation(RequestMapping.class));
            addPaths(paths, method.getAnnotation(GetMapping.class));
            addPaths(paths, method.getAnnotation(PostMapping.class));
            addPaths(paths, method.getAnnotation(PutMapping.class));
            addPaths(paths, method.getAnnotation(PatchMapping.class));
            addPaths(paths, method.getAnnotation(DeleteMapping.class));
        }

        return paths;
    }

    private static void addPaths(Set<String> paths, RequestMapping mapping) {
        if (mapping == null) {
            return;
        }
        addValues(paths, mapping.path());
        addValues(paths, mapping.value());
    }

    private static void addPaths(Set<String> paths, GetMapping mapping) {
        if (mapping == null) {
            return;
        }
        addValues(paths, mapping.path());
        addValues(paths, mapping.value());
    }

    private static void addPaths(Set<String> paths, PostMapping mapping) {
        if (mapping == null) {
            return;
        }
        addValues(paths, mapping.path());
        addValues(paths, mapping.value());
    }

    private static void addPaths(Set<String> paths, PutMapping mapping) {
        if (mapping == null) {
            return;
        }
        addValues(paths, mapping.path());
        addValues(paths, mapping.value());
    }

    private static void addPaths(Set<String> paths, PatchMapping mapping) {
        if (mapping == null) {
            return;
        }
        addValues(paths, mapping.path());
        addValues(paths, mapping.value());
    }

    private static void addPaths(Set<String> paths, DeleteMapping mapping) {
        if (mapping == null) {
            return;
        }
        addValues(paths, mapping.path());
        addValues(paths, mapping.value());
    }

    private static void addValues(Set<String> paths, String[] values) {
        for (String value : values) {
            if (!value.isBlank()) {
                paths.add(value);
            }
        }
    }
}
