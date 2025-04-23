package com.rxclinic.controller;

import com.rxclinic.model.Card;
import com.rxclinic.model.Category;
import com.rxclinic.repository.CardRepository;
import com.rxclinic.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CardRepository cardRepository;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/Uploads/cards/";

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        log.info("Fetching all categories");
        try {
            List<Category> categories = categoryRepository.findAll();
            log.info("Found {} categories", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error fetching categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/cards")
    public ResponseEntity<List<Card>> getCards(@RequestParam(required = false) String category) {
        log.info("Fetching cards with category: {}", category);
        try {
            List<Card> cards;
            if (category == null || category.equals("All")) {
                cards = cardRepository.findAll();
            } else {
                cards = cardRepository.findByCategoryCode(category);
            }
            log.info("Found {} cards", cards.size());
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            log.error("Error fetching cards", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        log.info("Deleting card with id: {}", id);
        try {
            if (cardRepository.existsById(id)) {
                Card card = cardRepository.findById(id).orElse(null);
                if (card != null && card.getImage() != null) {
                    String imagePath = getImagePath(card.getImage());
                    try {
                        Files.deleteIfExists(Paths.get(imagePath));
                        log.info("Deleted image for card {}: {}", id, imagePath);
                    } catch (IOException e) {
                        log.error("Failed to delete image for card {}: {}", id, e.getMessage());
                    }
                }
                cardRepository.deleteById(id);
                log.info("Card deleted successfully");
                return ResponseEntity.ok().build();
            }
            log.warn("Card with id {} not found", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting card", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> getCardById(@PathVariable Long id) {
        log.info("Fetching card with id: {}", id);
        try {
            return cardRepository.findById(id)
                    .map(card -> {
                        log.info("Card found: {}", card.getTitle());
                        return ResponseEntity.ok(card);
                    })
                    .orElseGet(() -> {
                        log.warn("Card with id {} not found", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("Error fetching card", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> createCard(
            @RequestPart("title") String title,
            @RequestPart("description") String description,
            @RequestPart("price") String price,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart("categoryCode") String categoryCode) {
        log.info("Creating new card with title: {}", title);
        try {
            // Валидация цены
            Double.parseDouble(price);

            // Валидация categoryCode
            if (!categoryRepository.existsById(categoryCode)) {
                log.warn("Category with code {} not found", categoryCode);
                return ResponseEntity.badRequest().body(null);
            }

            // Проверка изображения
            if (image != null && !image.isEmpty()) {
                String fileName = image.getOriginalFilename();
                if (fileName == null || !isImageFile(fileName)) {
                    log.warn("Invalid image file: {}", fileName);
                    return ResponseEntity.badRequest().body(null);
                }
            }

            Card newCard = new Card();
            newCard.setTitle(title);
            newCard.setDescription(description);
            newCard.setPrice(price);
            newCard.setCategoryCode(categoryCode);

            if (image != null && !image.isEmpty()) {
                try {
                    String uploadDir = UPLOAD_DIR;
                    Path uploadPath = Paths.get(uploadDir);

                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                        log.info("Created directory: {}", uploadPath.toAbsolutePath());
                    }

                    if (!Files.isWritable(uploadPath)) {
                        log.error("Directory is not writable: {}", uploadPath.toAbsolutePath());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                    }

                    String uniqueFileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                    Path filePath = uploadPath.resolve(uniqueFileName);

                    log.info("Saving image to: {}", filePath.toAbsolutePath());
                    image.transferTo(filePath.toFile());
                    newCard.setImage("/Uploads/cards/" + uniqueFileName);
                    log.info("Image saved: {}", newCard.getImage());
                } catch (IOException e) {
                    log.error("Failed to save image: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }

            Card savedCard = cardRepository.save(newCard);
            log.info("Card created with id: {}", savedCard.getId());
            return ResponseEntity.ok(savedCard);
        } catch (NumberFormatException e) {
            log.warn("Invalid price format: {}", price);
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Failed to create card: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> updateCard(
            @PathVariable Long id,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "price", required = false) String price,
            @RequestPart(value = "categoryCode", required = false) String categoryCode,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        log.info("Updating card with id: {}", id);
        try {
            return cardRepository.findById(id)
                    .map(card -> {
                        if (title != null && !title.isBlank()) {
                            card.setTitle(title);
                        }
                        if (description != null && !description.isBlank()) {
                            card.setDescription(description);
                        }
                        if (price != null && !price.isBlank()) {
                            try {
                                Double.parseDouble(price);
                                card.setPrice(price);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid price format: {}", price);
                                throw new IllegalArgumentException("Price must be a valid number");
                            }
                        }
                        if (categoryCode != null && !categoryCode.isBlank()) {
                            if (!categoryRepository.existsById(categoryCode)) {
                                log.warn("Category with code {} not found", categoryCode);
                                throw new IllegalArgumentException("Category not found");
                            }
                            card.setCategoryCode(categoryCode);
                        }
                        if (image != null && !image.isEmpty()) {
                            String fileName = image.getOriginalFilename();
                            if (fileName == null || !isImageFile(fileName)) {
                                log.warn("Invalid image file: {}", fileName);
                                throw new IllegalArgumentException("Image must be PNG, JPEG, or JPG");
                            }
                            try {
                                // Удаляем старое изображение, если оно есть
                                if (card.getImage() != null) {
                                    String oldImagePath = getImagePath(card.getImage());
                                    Files.deleteIfExists(Paths.get(oldImagePath));
                                    log.info("Deleted old image for card {}: {}", id, oldImagePath);
                                }

                                String uploadDir = UPLOAD_DIR;
                                Path uploadPath = Paths.get(uploadDir);

                                if (!Files.exists(uploadPath)) {
                                    Files.createDirectories(uploadPath);
                                    log.info("Created directory: {}", uploadPath.toAbsolutePath());
                                }

                                if (!Files.isWritable(uploadPath)) {
                                    log.error("Directory is not writable: {}", uploadPath.toAbsolutePath());
                                    throw new IOException("Directory is not writable");
                                }

                                String uniqueFileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                                Path filePath = uploadPath.resolve(uniqueFileName);

                                log.info("Saving image to: {}", filePath.toAbsolutePath());
                                image.transferTo(filePath.toFile());
                                card.setImage("/Uploads/cards/" + uniqueFileName);
                                log.info("Image saved: {}", card.getImage());
                            } catch (IOException e) {
                                log.error("Failed to save image: {}", e.getMessage(), e);
                                throw new RuntimeException("Failed to save image");
                            }
                        }
                        Card savedCard = cardRepository.save(card);
                        log.info("Card updated successfully");
                        return ResponseEntity.ok(savedCard);
                    })
                    .orElseGet(() -> {
                        log.warn("Card with id {} not found", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("Error updating card: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> createCategory(@RequestBody Category newCategory) {
        log.info("Creating new category with code: {}", newCategory.getCode());
        try {
            if (newCategory.getCode() == null || newCategory.getCode().trim().isEmpty()) {
                log.warn("Category code is empty");
                return ResponseEntity.badRequest().body(null);
            }
            if (newCategory.getName() == null || newCategory.getName().trim().isEmpty()) {
                log.warn("Category name is empty");
                return ResponseEntity.badRequest().body(null);
            }
            if (categoryRepository.existsById(newCategory.getCode())) {
                log.warn("Category with code {} already exists", newCategory.getCode());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
            }
            Category savedCategory = categoryRepository.save(newCategory);
            log.info("Category created with code: {}", savedCategory.getCode());
            return ResponseEntity.ok(savedCategory);
        } catch (Exception e) {
            log.error("Failed to create category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/categories/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable String code) {
        log.info("Deleting category with code: {}", code);
        try {
            if (!categoryRepository.existsById(code)) {
                log.warn("Category with code {} not found", code);
                return ResponseEntity.notFound().build();
            }

            List<Card> cardsToDelete = cardRepository.findByCategoryCode(code);
            for (Card card : cardsToDelete) {
                if (card.getImage() != null) {
                    String imagePath = getImagePath(card.getImage());
                    try {
                        Files.deleteIfExists(Paths.get(imagePath));
                        log.info("Deleted image for card {}: {}", card.getId(), imagePath);
                    } catch (IOException e) {
                        log.error("Failed to delete image for card {}: {}", card.getId(), e.getMessage());
                    }
                }
                cardRepository.delete(card);
                log.info("Deleted card with id: {}", card.getId());
            }

            categoryRepository.deleteById(code);
            log.info("Category deleted with code: {}", code);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean isImageFile(String fileName) {
        String[] allowedExtensions = {".jpg", ".jpeg", ".png"};
        return List.of(allowedExtensions).stream()
                .anyMatch(fileName.toLowerCase()::endsWith);
    }

    private String getImagePath(String image) {
        if (image == null || image.isEmpty()) {
            return "";
        }
        // Очищаем путь от возможного полного URL
        String cleanedPath = image.replaceAll("^(http://localhost:8080)+", "");
        // Извлекаем имя файла из пути
        String fileName = Paths.get(cleanedPath).getFileName().toString();
        return UPLOAD_DIR + fileName;
    }
}