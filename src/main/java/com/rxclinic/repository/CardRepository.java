package com.rxclinic.repository;


import com.rxclinic.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByCategoryCode(String categoryCode);
}