package com.rxclinic.config;

import com.rxclinic.model.Card;
import com.rxclinic.model.Category;
import com.rxclinic.model.Role;
import com.rxclinic.model.User;
import com.rxclinic.repository.CardRepository;
import com.rxclinic.repository.CategoryRepository;
import com.rxclinic.repository.RoleRepository;
import com.rxclinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final CardRepository cardRepository;

    @Bean
    public CommandLineRunner initCatalogData() {
        return args -> {
            // Проверка и загрузка категорий
            if (categoryRepository.count() == 0) {
                categoryRepository.saveAll(List.of(
                        new Category("All", "Все"),
                        new Category("therapy", "Терапевтическая стоматология"),
                        new Category("surgical", "Хирургическая стоматология"),
                        new Category("orthod", "Лечение и реабилитация"),
                        new Category("aesthet", "Эстетическая стоматология")
                ));
                System.out.println("Categories initialized.");
            }

            // Проверка и загрузка карточек
            if (cardRepository.count() == 0) {
                cardRepository.saveAll(List.of(
                        new Card(1L, "therapy", "/caries.jpg", "Лечение кариеса", "Устранение кариозных поражений с установкой пломбы.", "1 500 "),
                        new Card(2L, "therapy", "/Pulpits.jpg", "Лечение пульпита", "Лечение нерва зуба с последующим пломбированием каналов.", "3 500 "),
                        new Card(3L, "therapy", "/Periodontitis.jpg", "Лечение периодонтита", "Терапия воспаления тканей вокруг корней зубов.", "4 000 "),
                        new Card(4L, "surgical", "/simple.jpg", "Удаление зуба (простое)", "Безболезненное удаление зуба под местной анестезией.", "2 000 "),
                        new Card(5L, "surgical", "/hard.jpg", "Удаление зуба (сложное)", "Удаление ретинированных или дистопированных зубов.", "4 000 "),
                        new Card(6L, "surgical", "/implantation.jpg", "Имплантация зуба", "Установка импланта с последующим протезированием.", "25 000 "),
                        new Card(7L, "orthod", "/metal.jpg", "Установка металлических брекетов", "Эффективное исправление прикуса с помощью классических брекетов.", "30 000 "),
                        new Card(8L, "orthod", "/ceramic.jpg", "Установка керамических брекетов", "Менее заметные брекеты для эстетичного лечения.", "40 000 "),
                        new Card(9L, "orthod", "/correction.jpg", "Коррекция прикуса", "Раннее исправление прикуса для предотвращения проблем в будущем.", "20 000 "),
                        new Card(10L, "aesthet", "/whitening.jpg", "Профессиональное отбеливание", "Осветление зубов на несколько тонов за один визит.", "10 000 "),
                        new Card(11L, "aesthet", "/veneers.jpg", "Установка виниров", "Тонкие накладки для идеальной улыбки.", "20 000 "),
                        new Card(12L, "aesthet", "/enamel.jpg", "Микроабразия эмали", "Устранение поверхностных дефектов эмали.", "3 000 ")
                ));
                System.out.println("Cards initialized.");
            }
        };
    }

    @Bean
    public CommandLineRunner initRolesAndAdmin() {
        return args -> {
            // Инициализация ролей
            if (roleRepository.findByName("ROLE_USER").isEmpty()) {
                Role userRole = new Role();
                userRole.setName("ROLE_USER");
                roleRepository.save(userRole);
                System.out.println("ROLE_USER initialized.");
            }

            if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
                Role adminRole = new Role();
                adminRole.setName("ROLE_ADMIN");
                roleRepository.save(adminRole);
                System.out.println("ROLE_ADMIN initialized.");
            }

            if (roleRepository.findByName("ROLE_DOCTOR").isEmpty()) {
                Role doctorRole = new Role();
                doctorRole.setName("ROLE_DOCTOR");
                roleRepository.save(doctorRole);
                System.out.println("ROLE_DOCTOR initialized.");
            }

            // Создание администратора
            if (!userService.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@rxclinic.com");

                Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                        .orElseThrow(() -> new RuntimeException("Admin role not found"));
                admin.setRoles(Set.of(adminRole));

                userService.saveUser(admin);
                System.out.println("Admin user initialized.");
            }

            // Создание врача
            if (!userService.existsByUsername("doctor")) {
                User doctor = new User();
                doctor.setUsername("doctor");
                doctor.setPassword(passwordEncoder.encode("doctor123"));
                doctor.setEmail("doctor@rxclinic.com");

                Role doctorRole = roleRepository.findByName("ROLE_DOCTOR")
                        .orElseThrow(() -> new RuntimeException("Doctor role not found"));
                doctor.setRoles(Set.of(doctorRole));

                userService.saveUser(doctor);
                System.out.println("Doctor user initialized.");
            }
        };
    }
}