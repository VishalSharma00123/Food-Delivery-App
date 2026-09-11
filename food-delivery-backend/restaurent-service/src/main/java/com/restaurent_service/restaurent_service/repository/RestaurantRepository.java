package com.restaurent_service.restaurent_service.repository;

import com.restaurent_service.restaurent_service.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
