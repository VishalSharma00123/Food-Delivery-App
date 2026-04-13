package com.auth_service.auth_service.mapper;

import com.food.auth.dto.UserResponse;
import com.food.auth.entity.UserCredential;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(role -> role.getName()).toList())")
    UserResponse toResponse(UserCredential user);
}
