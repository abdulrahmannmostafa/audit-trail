package com.qeema.util.identity.mapper;

import org.mapstruct.Mapper;

import com.qeema.util.identity.dto.UserResponse;
import com.qeema.util.identity.entity.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
