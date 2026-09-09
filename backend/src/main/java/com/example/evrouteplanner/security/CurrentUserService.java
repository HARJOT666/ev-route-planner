package com.example.evrouteplanner.security;

import com.example.evrouteplanner.exception.ResourceNotFoundException;
import com.example.evrouteplanner.model.User;
import com.example.evrouteplanner.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Small helper to fetch the logged-in User. The JWT filter stored the user's
 * email in the security context, so we read it back and load the full User.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));
    }
}
