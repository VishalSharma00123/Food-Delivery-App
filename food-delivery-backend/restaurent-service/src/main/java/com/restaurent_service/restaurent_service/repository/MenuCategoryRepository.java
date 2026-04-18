package com.restaurent_service.restaurent_service.repository;

import com.restaurent_service.restaurent_service.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    List<MenuCategory> findByRestaurantId(Long restaurantId);
}
