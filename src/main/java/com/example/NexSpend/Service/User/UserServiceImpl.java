package com.example.NexSpend.Service.User;


import com.example.NexSpend.DTO.AuthResponseDTO;
import com.example.NexSpend.DTO.LoginRequestDTO;
import com.example.NexSpend.DTO.UserDTO.ChangePasswordRequestDTO;
import com.example.NexSpend.DTO.UserDTO.UserRequestDTO;
import com.example.NexSpend.DTO.UserDTO.UserResponseDTO;
import com.example.NexSpend.Entity.RefreshToken;
import com.example.NexSpend.Entity.User;
import com.example.NexSpend.Exception.*;
import com.example.NexSpend.Mapper.UserMapper;
import com.example.NexSpend.Repository.UserRepository;
import com.example.NexSpend.Repository.ExpenseRepository;
import com.example.NexSpend.Repository.BudgetRepository;
import com.example.NexSpend.Repository.RecurringExpenseRepository;
import com.example.NexSpend.Repository.RefreshTokenRepository;
import com.example.NexSpend.Service.CustomUserDetailsService;
import com.example.NexSpend.Service.Email.EmailService;
import com.example.NexSpend.Service.JWT.JwtService;
import com.example.NexSpend.Service.RefreshToken.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final ExpenseRepository expenseRepository;

    private final BudgetRepository budgetRepository;

    private final RecurringExpenseRepository recurringExpenseRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final CustomUserDetailsService userDetailsService;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final EmailService emailService;

    private final AuthenticationManager authenticationManager;

    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDTO register(UserRequestDTO dto) {

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        LocalDateTime activationExpiry =
                LocalDateTime.now().plusHours(24);

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(
                        passwordEncoder.encode(dto.getPassword())
                )
                .enabled(false)
                .activationToken(UUID.randomUUID().toString())
                .activationTokenExpiry(activationExpiry)
                .build();

        User savedUser =
                userRepository.save(user);

        emailService.sendActivationEmail(
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getActivationToken()
        );

        return userMapper.mapToDto(savedUser);
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO dto) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getEmail(),
                        dto.getPassword()
                )
        );

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        if (!user.isEnabled()) {
            throw new AccountNotActivatedException(
                    "Please verify your email before signing in"
            );
        }

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        user.getEmail()
                );

        String token = jwtService.generateToken(userDetails);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthResponseDTO.builder()
                .accessToken(token)
                .refreshToken(refreshToken.getToken())
                .message("Login successful")
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    @Override
    @Transactional
    public void activateUser(String token) {

        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() ->
                        new InvalidTokenException(
                                "Invalid activation token"
                        )
                );

        if (user.isEnabled()) {
            throw new InvalidTokenException(
                    "Account has already been activated"
            );
        }

        if (user.getActivationTokenExpiry() == null ||
                LocalDateTime.now()
                        .isAfter(user.getActivationTokenExpiry())) {

            throw new InvalidTokenException(
                    "Activation token has expired. Please register again."
            );
        }

        user.setEnabled(true);

        user.setActivationToken(null);

        user.setActivationTokenExpiry(null);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id, Authentication authentication) {
        User current = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!current.getId().equals(id)) {
            throw new UnauthorizedActionException("Access denied");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userMapper.mapToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return userMapper.mapToDto(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(
            Long id,
            UserRequestDTO dto,
            Authentication authentication
    ) {

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!currentUser.getId().equals(id)) {
            throw new UnauthorizedActionException("Access denied");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(dto.getEmail());
        boolean passwordChanged = dto.getPassword() != null && !dto.getPassword().isBlank();

        if (emailChanged && userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        if (passwordChanged) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User updatedUser = userRepository.save(user);


        if (emailChanged || passwordChanged) {
            refreshTokenRepository.deleteByUser(user);
        }

        return userMapper.mapToDto(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(
            Long id,
            Authentication authentication
    ) {

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );

        if (!currentUser.getId().equals(id)) {
            throw new UnauthorizedActionException("Access denied");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );
        refreshTokenRepository.permanentlyDeleteByUserId(id);

        recurringExpenseRepository.permanentlyDeleteByUserId(id);

        budgetRepository.permanentlyDeleteByUserId(id);

        expenseRepository.permanentlyDeleteByUserId(id);

        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void changePassword(
            Long id,
            ChangePasswordRequestDTO dto,
            Authentication authentication
    ) {

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!currentUser.getId().equals(id)) {
            throw new UnauthorizedActionException("Access denied");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), currentUser.getPassword())) {
            throw new UnauthorizedActionException("Current password is incorrect");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        currentUser.setPassword(passwordEncoder.encode(dto.getNewPassword()));

        userRepository.save(currentUser);
        refreshTokenRepository.deleteByUser(currentUser);

        try {
            emailService.sendPasswordChangedEmail(
                    currentUser.getEmail(),
                    currentUser.getName()
            );
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(UserServiceImpl.class)
                    .warn("Password changed but confirmation email could not be sent for user {}",
                            currentUser.getId(), e);
        }
    }

}
