package com.msp.mappers;

import com.msp.models.User;
import com.msp.payloads.dtos.UserDto;

public class UserMapper {
    public static UserDto toDTO(User savedUser) {
        if (savedUser == null) {
            return null;
        }
        UserDto userDto = new UserDto();
        userDto.setId(savedUser.getId());
        userDto.setFirstName(savedUser.getFirstName());
        userDto.setLastName(savedUser.getLastName());
        userDto.setEmail(savedUser.getEmail());
        userDto.setRole(savedUser.getRole());
        userDto.setUserStatus(savedUser.getUserStatus());
        userDto.setCreatedAt(savedUser.getCreatedAt());
        userDto.setPassword(savedUser.getPassword());
        userDto.setUpdatedAt(savedUser.getUpdatedAt());
        userDto.setLastLogin(savedUser.getLastLogin());
        userDto.setPhone(savedUser.getPhone());
        userDto.setBranchId(savedUser.getBranch() != null ? savedUser.getBranch().getId() : null);
        userDto.setStoreId(savedUser.getStore() != null ? savedUser.getStore().getId() : null);
        return userDto;
    }

    public static User toEntity(UserDto userDto) {
        User createdUser = new User();
        createdUser.setEmail(userDto.getEmail());
        createdUser.setFirstName(userDto.getFirstName());
        createdUser.setLastName(userDto.getLastName());
        createdUser.setRole(userDto.getRole());
        createdUser.setCreatedAt(userDto.getCreatedAt());
        createdUser.setUpdatedAt(userDto.getUpdatedAt());
        createdUser.setLastLogin(userDto.getLastLogin());
        createdUser.setPhone(userDto.getPhone());
        createdUser.setPassword(userDto.getPassword());

        return createdUser;
    }
}
