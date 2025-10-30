package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.models.CheckInStatus;
import com.gym.service.gymmanagementservice.services.CheckInService;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/check-in") // Prefix cho trang Check-in
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')") // Yêu cầu Admin hoặc Staff
public class CheckInWebController {

    private final CheckInService checkInService;

    /**
     * Hiển thị trang Check-in chính
     * (Chúng ta thêm 1 DTO trống để form binding)
     */
    @GetMapping
    public String getCheckInPage(Model model) {
        // Cung cấp một đối tượng trống cho form
        if (!model.containsAttribute("checkInRequest")) {
            model.addAttribute("checkInRequest", new CheckInRequestDTO());
        }

        model.addAttribute("pageTitle", "Check-in Hội viên");
        model.addAttribute("contentView", "check-in"); // Dùng file check-in.html
        return "fragments/layout";
    }

    /**
     * Xử lý quét mã/nhập SĐT
     */
    @PostMapping
    public String processCheckIn(@Valid @ModelAttribute("checkInRequest") CheckInRequestDTO request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        // Nếu SĐT/barcode trống
        if (bindingResult.hasErrors()) {
            // Chuyển lỗi về trang GET
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.checkInRequest", bindingResult);
            redirectAttributes.addFlashAttribute("checkInRequest", request);
            return "redirect:/check-in";
        }

        try {
            // 1. Gọi service check-in
            CheckInResponseDTO response = checkInService.performCheckIn(request);

            // 2. Gửi kết quả về
            if (response.getStatus() == CheckInStatus.SUCCESS) {
                redirectAttributes.addFlashAttribute("checkInSuccess", true);
            } else {
                redirectAttributes.addFlashAttribute("checkInError", true);
            }
            redirectAttributes.addFlashAttribute("checkInResponse", response);

        } catch (Exception e) {
            // Lỗi hệ thống (hiếm)
            redirectAttributes.addFlashAttribute("checkInError", true);
            // Tạo response lỗi thủ công
            CheckInResponseDTO errorResponse = CheckInResponseDTO.builder()
                    .status(null) // Lỗi hệ thống
                    .message("Lỗi hệ thống: " + e.getMessage())
                    .build();
            redirectAttributes.addFlashAttribute("checkInResponse", errorResponse);
        }

        // 3. Tải lại trang check-in (để sẵn sàng cho lần quét tiếp theo)
        return "redirect:/check-in";
    }
}