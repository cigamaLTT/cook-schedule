package com.cigama.cook_schedule.controller;

import com.cigama.cook_schedule.entity.ApprovedRoster;
import com.cigama.cook_schedule.entity.UserAccount;
import com.cigama.cook_schedule.entity.UserSchedule;
import com.cigama.cook_schedule.repository.ApprovedRosterRepository;
import com.cigama.cook_schedule.repository.UserAccountRepository;
import com.cigama.cook_schedule.repository.UserScheduleRepository;
import com.cigama.cook_schedule.service.AlgorithmService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserAccountRepository userAccountRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final ApprovedRosterRepository approvedRosterRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlgorithmService algorithmService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final Map<String, String> DEFAULT_PASSWORDS = Map.of(
            "admin", "admin123",
            "user1", "123456",
            "user2", "123456");

    @GetMapping
    public String adminPage(Model model) {
        model.addAttribute("users", userAccountRepository.findAll());
        return "admin";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String userId,
            @RequestParam String displayName,
            @RequestParam String password,
            @RequestParam String role) {
        if (userAccountRepository.existsById(userId)) {
            return "redirect:/admin?error=exists";
        }
        UserAccount account = new UserAccount(userId, displayName, passwordEncoder.encode(password), role);
        userAccountRepository.save(account);
        return "redirect:/admin?createSuccess=" + userId;
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String userId) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String defaultPass = DEFAULT_PASSWORDS.getOrDefault(userId, "welcome123");
        account.setPassword(passwordEncoder.encode(defaultPass));
        userAccountRepository.save(account);

        return "redirect:/admin?resetSuccess=" + userId;
    }

    @PostMapping("/approve-roster")
    public String approveRoster() {
        List<UserSchedule> schedules = userScheduleRepository.findAll();
        List<String> userNames = schedules.stream().map(UserSchedule::getUsername).toList();

        String market = algorithmService.findOptimalAssignment(userNames,
                schedules.stream().map(UserSchedule::getNoMarket).toList());
        String cookNoon = algorithmService.findOptimalAssignment(userNames,
                schedules.stream().map(UserSchedule::getNoCookNoon).toList());
        String washNoon = algorithmService.findOptimalAssignment(userNames,
                schedules.stream().map(UserSchedule::getNoWashNoon).toList());
        String cookNight = algorithmService.findOptimalAssignment(userNames,
                schedules.stream().map(UserSchedule::getNoCookNight).toList());
        String washNight = algorithmService.findOptimalAssignment(userNames,
                schedules.stream().map(UserSchedule::getNoWashNight).toList());

        ApprovedRoster approved = new ApprovedRoster(1, market, cookNoon, washNoon, cookNight, washNight);
        approvedRosterRepository.save(approved);

        simpMessagingTemplate.convertAndSend("/topic/roster", "UPDATED");
        return "redirect:/roster?approved";
    }

    @PostMapping("/cancel-roster")
    public String cancelRoster() {
        approvedRosterRepository.deleteById(1);
        simpMessagingTemplate.convertAndSend("/topic/roster", "UPDATED");
        return "redirect:/roster?cancelled";
    }

    @GetMapping("/edit-roster")
    public String editRoster(Model model) {
        List<UserSchedule> schedules = userScheduleRepository.findAll();
        List<UserAccount> accounts = userAccountRepository.findAll();
        List<String> userNames = schedules.stream().map(UserSchedule::getUsername).toList();
        ApprovedRoster approved = approvedRosterRepository.findById(1).orElse(null);

        Map<String, String> nameMap = accounts.stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getUserId, UserAccount::getDisplayName));

        model.addAttribute("users", userNames);
        model.addAttribute("nameMap", nameMap);
        model.addAttribute("schedules", schedules);
        model.addAttribute("appRoster", approved);

        return "admin-roster-edit";
    }

    @PostMapping("/save-roster")
    public String saveRoster(@RequestParam(defaultValue = "0000000") String noMarket,
            @RequestParam(defaultValue = "0000000") String noCookNoon,
            @RequestParam(defaultValue = "0000000") String noWashNoon,
            @RequestParam(defaultValue = "0000000") String noCookNight,
            @RequestParam(defaultValue = "0000000") String noWashNight) {
        ApprovedRoster approved = new ApprovedRoster(1, noMarket, noCookNoon, noWashNoon, noCookNight, noWashNight);
        approvedRosterRepository.save(approved);
        simpMessagingTemplate.convertAndSend("/topic/roster", "UPDATED");
        return "redirect:/admin/edit-roster?success";
    }

    @PostMapping("/users/clear-all-schedules")
    public String clearAllSchedules() {
        userScheduleRepository.deleteAll();
        simpMessagingTemplate.convertAndSend("/topic/roster", "UPDATED");
        return "redirect:/admin?clearAllSuccess";
    }
}
