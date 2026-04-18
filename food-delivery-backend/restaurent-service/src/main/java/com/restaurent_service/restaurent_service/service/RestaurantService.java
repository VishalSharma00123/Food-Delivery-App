package com.restaurent_service.restaurent_service.service;

import com.restaurent_service.restaurent_service.dto.RestaurantDto;
import java.util.List;

public interface RestaurantService {
    RestaurantDto createRestaurant(RestaurantDto restaurantDto);
    RestaurantDto updateRestaurant(Long id, RestaurantDto restaurantDto);
    RestaurantDto getRestaurantById(Long id);
    List<RestaurantDto> getAllRestaurants();
    RestaurantDto updateStatus(Long id, String status);
    void deleteRestaurant(Long id);
}
