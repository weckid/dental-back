package com.rxclinic.controller;

import com.rxclinic.model.Card;
import com.rxclinic.model.Category;
import com.rxclinic.repository.CardRepository;
import com.rxclinic.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CardRepository cardRepository;

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/cards")
    public List<Card> getCards(@RequestParam(required = false) String category) {
        if (category == null || category.equals("All")) {
            return cardRepository.findAll();
        }
        return cardRepository.findByCategoryCode(category);
    }

    @DeleteMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        if (cardRepository.existsById(id)) {
            cardRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody Card updatedCard) {
        return cardRepository.findById(id)
                .map(card -> {
                    card.setTitle(updatedCard.getTitle());
                    card.setDescription(updatedCard.getDescription());
                    card.setPrice(updatedCard.getPrice());
                    card.setImage(updatedCard.getImage());
                    card.setCategoryCode(updatedCard.getCategoryCode());
                    return ResponseEntity.ok(cardRepository.save(card));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cards/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> getCardById(@PathVariable Long id) {
        return cardRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Card> createCard(
            @RequestPart("title") String title,
            @RequestPart("description") String description,
            @RequestPart("price") String price,
            @RequestPart("image") MultipartFile image,
            @RequestPart("categoryCode") String categoryCode) throws IOException {
        String fileName = image.getOriginalFilename();
        if (fileName == null || !isImageFile(fileName)) {
            return ResponseEntity.badRequest().body(null);
        }

        Card newCard = new Card();
        newCard.setTitle(title);
        newCard.setDescription(description);
        newCard.setPrice(price);
        newCard.setCategoryCode(categoryCode);

        String uploadDir = System.getProperty("user.dir") + "/uploads/cards/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
        File destination = new File(dir, uniqueFileName);
        image.transferTo(destination);
        newCard.setImage("/uploads/cards/" + uniqueFileName);

        Card savedCard = cardRepository.save(newCard);
        return ResponseEntity.ok(savedCard);
    }

    private boolean isImageFile(String fileName) {
        String[] allowedExtensions = {".jpg", ".jpeg", ".png"};
        return List.of(allowedExtensions).stream()
                .anyMatch(fileName.toLowerCase()::endsWith);
    }
}