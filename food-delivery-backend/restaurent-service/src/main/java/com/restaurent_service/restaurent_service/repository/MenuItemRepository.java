package com.restaurent_service.restaurent_service.repository;

import com.restaurent_service.restaurent_service.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("""
            SELECT m FROM MenuItem m
            JOIN FETCH m.category c
            WHERE m.restaurant.id = :restaurantId
            ORDER BY c.id ASC, m.id ASC
            """)
    List<MenuItem> findByRestaurant_IdWithCategory(@Param("restaurantId") Long restaurantId);

    Optional<MenuItem> findByIdAndRestaurant_Id(Long id, Long restaurantId);
}
