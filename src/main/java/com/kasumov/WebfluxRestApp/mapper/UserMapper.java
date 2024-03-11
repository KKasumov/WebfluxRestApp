package com.kasumov.WebfluxRestApp.mapper;

import com.kasumov.WebfluxRestApp.dto.UserDTO;
import com.kasumov.WebfluxRestApp.dto.UserRequestDTO;
import com.kasumov.WebfluxRestApp.model.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "eventDTOs", ignore = true)
    UserDTO map(UserEntity user);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "events", ignore = true)
    UserEntity map(UserRequestDTO userRequestDTO);

    @Mapping(target = "events", ignore = true)
    UserEntity map(UserDTO userDTO);

    UserDTO mapToUserDTO(UserEntity userEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "events", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUserEntityFromUserRequestDTO(UserRequestDTO userRequestDTO, @MappingTarget UserEntity userEntity);
}
