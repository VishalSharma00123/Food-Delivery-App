package com.restaurent_service.restaurent_service.service;

import com.restaurent_service.restaurent_service.dto.RestaurantDto;
import com.restaurent_service.restaurent_service.entity.Restaurant;
import com.restaurent_service.restaurent_service.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Override
    public RestaurantDto createRestaurant(RestaurantDto dto) {
        Restaurant restaurant = Restaurant.builder()
                .ownerId(dto.getOwnerId())
                .name(dto.getName())
                .description(dto.getDescription())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .status("ONLINE") // Default status
                .build();
        return mapToDto(restaurantRepository.save(restaurant));
    }

    @Override
    public RestaurantDto updateRestaurant(Long id, RestaurantDto dto) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        
        restaurant.setName(dto.getName());
        restaurant.setDescription(dto.getDescription());
        restaurant.setAddress(dto.getAddress());
        restaurant.setPhone(dto.getPhone());
        
        return mapToDto(restaurantRepository.save(restaurant));
    }

    @Override
    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return mapToDto(restaurant);
    }

    @Override
    public List<RestaurantDto> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RestaurantDto updateStatus(Long id, String status) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        restaurant.setStatus(status);
        return mapToDto(restaurantRepository.save(restaurant));
    }

    @Override
    public void deleteRestaurant(Long id) {
        restaurantRepository.deleteById(id);
    }

    private RestaurantDto mapToDto(Restaurant restaurant) {
        return RestaurantDto.builder()
                .id(restaurant.getId())
                .ownerId(restaurant.getOwnerId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .phone(restaurant.getPhone())
                .status(restaurant.getStatus())
                .createdAt(restaurant.getCreatedAt())
                .build();
    }
}
