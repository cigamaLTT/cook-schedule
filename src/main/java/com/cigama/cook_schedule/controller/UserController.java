package com.cigama.cook_schedule.controller;

import com.cigama.cook_schedule.entity.UserAccount;
import com.cigama.cook_schedule.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserAccountRepository userAccountRepository;
    private final com.cigama.cook_schedule.repository.UserScheduleRepository userScheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public String profilePage(Principal principal, Model model) {
        String userId = principal.getName();
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", account);
        return "profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(Principal principal,
            @RequestParam String displayName,
            @RequestParam(required = false) String newPassword) {
        String userId = principal.getName();
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        account.setDisplayName(displayName);
        if (newPassword != null && !newPassword.isBlank()) {
            account.setPassword(passwordEncoder.encode(newPassword));
        }

        userAccountRepository.save(account);
        return "redirect:/user/profile?success";
    }

    @GetMapping("/profile/{username}")
    public String publicProfile(@PathVariable String username, Model model) {
        UserAccount account = userAccountRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("VIEWER".equals(account.getRole())) {
            return "redirect:/roster";
        }

        com.cigama.cook_schedule.entity.UserSchedule schedule = userScheduleRepository.findById(username).orElse(null);

        model.addAttribute("targetUser", account);
        model.addAttribute("schedule", schedule);
        return "user-profile";
    }
}
