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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ScheduleController {

    private final UserScheduleRepository userScheduleRepository;
    private final UserAccountRepository userAccountRepository;
    private final ApprovedRosterRepository approvedRosterRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AlgorithmService algorithmService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping({ "/", "/schedule" })
    public String getSchedule(Model model, Principal principal) {
        String username = principal.getName();
        UserAccount account = userAccountRepository.findById(username).orElse(null);
        if (account != null && "VIEWER".equals(account.getRole())) {
            return "redirect:/roster";
        }

        UserSchedule schedule = userScheduleRepository.findById(username)
                .orElse(new UserSchedule(username, "0000000", "0000000", "0000000", "0000000", "0000000"));

        model.addAttribute("schedule", schedule);
        return "schedule";
    }

    @GetMapping("/roster")
    public String getRoster(Model model, @RequestParam(required = false) Integer seed) {
        if (seed == null) {
            seed = new java.util.Random().nextInt(255) + 1;
        }

        List<UserSchedule> schedules = userScheduleRepository.findAll();
        List<UserAccount> accounts = userAccountRepository.findAll();
        List<String> userNames = schedules.stream().map(UserSchedule::getUsername).toList();
        ApprovedRoster approved = approvedRosterRepository.findById(1).orElse(null);

        java.util.Map<String, String> nameMap = accounts.stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getUserId, UserAccount::getDisplayName));

        model.addAttribute("nameMap", nameMap);
        model.addAttribute("users", userNames);
        model.addAttribute("seed", seed);

        // --- Pending Roster Data ---
        List<List<String>> allTasks = List.of(
                schedules.stream().map(UserSchedule::getNoMarket).toList(),
                schedules.stream().map(UserSchedule::getNoCookNoon).toList(),
                schedules.stream().map(UserSchedule::getNoWashNoon).toList(),
                schedules.stream().map(UserSchedule::getNoCookNight).toList(),
                schedules.stream().map(UserSchedule::getNoWashNight).toList());

        java.util.Map<String, String> results = algorithmService.generateFullRoster(userNames, allTasks, seed);

        model.addAttribute("resMarket", results.get("resMarket"));
        model.addAttribute("resCookNoon", results.get("resCookNoon"));
        model.addAttribute("resWashNoon", results.get("resWashNoon"));
        model.addAttribute("resCookNight", results.get("resCookNight"));
        model.addAttribute("resWashNight", results.get("resWashNight"));

        // --- Approved Roster Data ---
        model.addAttribute("appRoster", approved);

        return "roster";
    }

    @PostMapping("/schedule")
    public String saveSchedule(Principal principal,
            @RequestParam(value = "noMarket", required = false) List<Integer> noMarket,
            @RequestParam(value = "noCookNoon", required = false) List<Integer> noCookNoon,
            @RequestParam(value = "noWashNoon", required = false) List<Integer> noWashNoon,
            @RequestParam(value = "noCookNight", required = false) List<Integer> noCookNight,
            @RequestParam(value = "noWashNight", required = false) List<Integer> noWashNight) {

        String username = principal.getName();
        UserSchedule schedule = new UserSchedule(
                username,
                convertToBinaryString(noMarket),
                convertToBinaryString(noCookNoon),
                convertToBinaryString(noWashNoon),
                convertToBinaryString(noCookNight),
                convertToBinaryString(noWashNight));

        userScheduleRepository.save(schedule);
        simpMessagingTemplate.convertAndSend("/topic/roster", "UPDATED");
        return "redirect:/schedule";
    }

    @PostMapping("/schedule/clear")
    public String clearSchedule(Principal principal) {
        String username = principal.getName();
        userScheduleRepository.deleteById(username);
        simpMessagingTemplate.convertAndSend("/topic/roster", "UPDATED");
        return "redirect:/schedule?cleared";
    }

    private String convertToBinaryString(List<Integer> selectedDays) {
        if (selectedDays == null)
            return "0000000";
        StringBuilder sb = new StringBuilder("0000000");
        for (Integer day : selectedDays) {
            if (day >= 0 && day < 7)
                sb.setCharAt(day, '1');
        }
        return sb.toString();
    }
}
