package com.sokmeak.quizapp.modules.user.mapper;

import com.sokmeak.quizapp.modules.user.dto.UserResponse;
import com.sokmeak.quizapp.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct in the style of the official docs: @Mapper plus an INSTANCE constant.
 * Records are supported as targets - MapStruct calls the canonical constructor.
 * The Role enum becomes a String with no configuration.
 */

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    UserResponse userToUserResponse(User user);
}

