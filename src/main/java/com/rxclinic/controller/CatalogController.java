package com.rxclinic.controller;

import com.rxclinic.model.Card;
import com.rxclinic.model.Category;
import com.rxclinic.repository.CardRepository;
import com.rxclinic.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}