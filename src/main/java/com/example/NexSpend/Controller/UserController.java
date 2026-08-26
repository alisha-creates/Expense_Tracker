package com.example.NexSpend.Controller;


import com.example.NexSpend.DTO.UserDTO.ChangePasswordRequestDTO;
import com.example.NexSpend.DTO.UserDTO.UserRequestDTO;
import com.example.NexSpend.DTO.UserDTO.UserResponseDTO;
import com.example.NexSpend.Service.User.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserResponseDTO response = userService.getUserById(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid
            @RequestBody UserRequestDTO dto,
            Authentication authentication
    ) {
        UserResponseDTO response =
                userService.updateUser(id, dto, authentication);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id,
            Authentication authentication
    ) {
        userService.deleteUser(id, authentication);

        return ResponseEntity.ok("User deleted successfully");
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequestDTO dto,
            Authentication authentication
    ) {
        userService.changePassword(id, dto, authentication);

        return ResponseEntity.ok("Password changed successfully");
    }

}
