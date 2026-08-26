package com.example.NexSpend.Service.User;


import com.example.NexSpend.DTO.AuthResponseDTO;
import com.example.NexSpend.DTO.LoginRequestDTO;
import com.example.NexSpend.DTO.UserDTO.ChangePasswordRequestDTO;
import com.example.NexSpend.DTO.UserDTO.UserRequestDTO;
import com.example.NexSpend.DTO.UserDTO.UserResponseDTO;
import com.example.NexSpend.Entity.User;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface UserService {
    UserResponseDTO register(UserRequestDTO dto);

    AuthResponseDTO login(LoginRequestDTO dto);

    void activateUser(String token);

    UserResponseDTO getUserById(Long id,
                                Authentication authentication);

    UserResponseDTO getCurrentUser(Authentication authentication);

    UserResponseDTO updateUser(
            Long id,
            UserRequestDTO dto,
            Authentication authentication
    );

    void deleteUser(Long id,
                    Authentication authentication);

    void changePassword(Long id, ChangePasswordRequestDTO dto, Authentication authentication);

}
