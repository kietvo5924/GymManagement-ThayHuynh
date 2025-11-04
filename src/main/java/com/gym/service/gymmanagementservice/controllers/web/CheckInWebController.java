package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.models.CheckInStatus;
import com.gym.service.gymmanagementservice.models.Club;
import com.gym.service.gymmanagementservice.services.CheckInService;
import com.gym.service.gymmanagementservice.services.ClubService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/check-in")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Hidden
public class CheckInWebController {

    private final CheckInService checkInService;
    private final ClubService clubService;

    @GetMapping
    public String getCheckInPage(Model model) {
        if (!model.containsAttribute("checkInRequest")) {
            model.addAttribute("checkInRequest", new CheckInRequestDTO());
        }

        // LẤY DANH SÁCH CLB ĐANG HOẠT ĐỘNG
        List<Club> activeClubs = clubService.getAllActiveClubs();
        model.addAttribute("activeClubs", activeClubs);

        model.addAttribute("pageTitle", "Check-in Hội viên");
        model.addAttribute("contentView", "check-in");
        model.addAttribute("activePage", "checkIn");
        return "fragments/layout";
    }

    @PostMapping
    public String processCheckIn(@Valid @ModelAttribute("checkInRequest") CheckInRequestDTO request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) { // <-- THÊM MODEL

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.checkInRequest", bindingResult);
            redirectAttributes.addFlashAttribute("checkInRequest", request);

            return "redirect:/check-in";
        }

        try {
            CheckInResponseDTO response = checkInService.performCheckIn(request);

            if (response.getStatus() == CheckInStatus.SUCCESS) {
                redirectAttributes.addFlashAttribute("checkInSuccess", true);
            } else {
                redirectAttributes.addFlashAttribute("checkInError", true);
            }
            redirectAttributes.addFlashAttribute("checkInResponse", response);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("checkInError", true);
            CheckInResponseDTO errorResponse = CheckInResponseDTO.builder()
                    .status(null)
                    .message("Lỗi hệ thống: " + e.getMessage())
                    .build();
            redirectAttributes.addFlashAttribute("checkInResponse", errorResponse);
        }

        // Giữ lại clubId đã chọn cho lần check-in tiếp theo
        redirectAttributes.addAttribute("clubId", request.getClubId());
        return "redirect:/check-in";
    }
}