package com.rxclinic.controller;

import com.rxclinic.model.Card;
import com.rxclinic.model.Category;
import com.rxclinic.repository.CardRepository;
import com.rxclinic.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
}