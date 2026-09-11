package com.restaurent_service.restaurent_service.repository;

import com.restaurent_service.restaurent_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByRestaurant_IdOrderByNameAsc(Long restaurantId);

    Optional<Category> findByIdAndRestaurant_Id(Long id, Long restaurantId);

    boolean existsByRestaurant_IdAndNameIgnoreCase(Long restaurantId, String name);
}
