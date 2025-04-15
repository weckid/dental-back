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

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CardRepository cardRepository;

    @GetMapping("/categories")
    public List<Category> getCategories() {
        log.info("Fetching all categories");
        List<Category> categories = categoryRepository.findAll();
        log.info("Found {} categories", categories.size());
        return categories;
    }

    @GetMapping("/cards")
    public List<Card> getCards(@RequestParam(required = false) String category) {
        log.info("Fetching cards with category: {}", category);
        if (category == null || category.equals("All")) {
            List<Card> cards = cardRepository.findAll();
            log.info("Found {} cards for all categories", cards.size());
            return cards;
        }
        List<Card> cards = cardRepository.findByCategoryCode(category);
        log.info("Found {} cards for category {}", cards.size(), category);
        return cards;
    }

    @DeleteMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        log.info("Deleting card with id: {}", id);
        if (cardRepository.existsById(id)) {
            cardRepository.deleteById(id);
            log.info("Card deleted successfully");
            return ResponseEntity.ok().build();
        }
        log.warn("Card with id {} not found", id);
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody Card updatedCard) {
        log.info("Updating card with id: {}", id);
        return cardRepository.findById(id)
                .map(card -> {
                    card.setTitle(updatedCard.getTitle());
                    card.setDescription(updatedCard.getDescription());
                    try {
                        Double.parseDouble(updatedCard.getPrice());
                        card.setPrice(updatedCard.getPrice());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid price format: {}", updatedCard.getPrice());
                        throw new IllegalArgumentException("Price must be a valid number");
                    }
                    card.setImage(updatedCard.getImage());
                    card.setCategoryCode(updatedCard.getCategoryCode());
                    Card savedCard = cardRepository.save(card);
                    log.info("Card updated successfully");
                    return ResponseEntity.ok(savedCard);
                })
                .orElseGet(() -> {
                    log.warn("Card with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> getCardById(@PathVariable Long id) {
        log.info("Fetching card with id: {}", id);
        return cardRepository.findById(id)
                .map(card -> {
                    log.info("Card found: {}", card.getTitle());
                    return ResponseEntity.ok(card);
                })
                .orElseGet(() -> {
                    log.warn("Card with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
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
            Double.parseDouble(price);
        } catch (NumberFormatException e) {
            log.warn("Invalid price format: {}", price);
            return ResponseEntity.badRequest().body(null);
        }

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
                String uploadDir = "/home/stud/Рабочий стол/dental-back-main/Uploads/cards/";
                File dir = new File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String uniqueFileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                File destination = new File(uploadDir + uniqueFileName);
                image.transferTo(destination);
                newCard.setImage("http://localhost:8080/Uploads/cards/" + uniqueFileName);
                log.info("Image saved: {}", newCard.getImage());
            } catch (IOException e) {
                log.error("Failed to save image", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

        Card savedCard = cardRepository.save(newCard);
        log.info("Card created with id: {}", savedCard.getId());
        return ResponseEntity.ok(savedCard);
    }

    private boolean isImageFile(String fileName) {
        String[] allowedExtensions = {".jpg", ".jpeg", ".png"};
        return List.of(allowedExtensions).stream()
                .anyMatch(fileName.toLowerCase()::endsWith);
    }
}