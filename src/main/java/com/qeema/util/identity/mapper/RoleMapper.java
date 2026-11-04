package com.qeema.util.identity.mapper;

import org.mapstruct.Mapper;

import com.qeema.util.identity.dto.RoleResponse;
import com.qeema.util.identity.entity.AppRole;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toResponse(AppRole role);
}
