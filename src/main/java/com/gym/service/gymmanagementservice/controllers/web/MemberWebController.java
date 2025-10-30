package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.*; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.models.PackageType; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.services.*; // <-- IMPORT MỚI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class MemberWebController {

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;
    private final PackageService packageService;
    private final StaffService staffService;
    private final PtSessionService ptSessionService; // (Nếu bạn đã tạo ở bước 6)

    /**
     * Hiển thị trang Danh sách Hội viên
     */
    @GetMapping
    public String getMembersPage(Model model) {
        List<MemberResponseDTO> members = memberService.getAllMembers();
        model.addAttribute("members", members);
        model.addAttribute("pageTitle", "Quản lý Hội viên");
        model.addAttribute("contentView", "members");
        return "fragments/layout";
    }

    @GetMapping("/create")
    public String showCreateMemberForm(Model model) {
        model.addAttribute("memberRequest", new MemberRequestDTO());
        model.addAttribute("pageTitle", "Tạo Hội viên mới");
        model.addAttribute("contentView", "member-form");
        return "fragments/layout";
    }
    @PostMapping("/create")
    public String processCreateMember(@Valid @ModelAttribute("memberRequest") MemberRequestDTO memberRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo Hội viên mới");
            model.addAttribute("contentView", "member-form");
            return "fragments/layout";
        }
        try {
            memberService.createMember(memberRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hội viên thành công!");
            return "redirect:/members";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("pageTitle", "Tạo Hội viên mới");
            model.addAttribute("contentView", "member-form");
            return "fragments/layout";
        }
    }
    @GetMapping("/edit/{memberId}")
    public String showEditMemberForm(@PathVariable("memberId") Long memberId, Model model) {
        try {
            MemberResponseDTO member = memberService.getMemberById(memberId);
            MemberRequestDTO memberRequest = new MemberRequestDTO();
            memberRequest.setFullName(member.getFullName());
            memberRequest.setPhoneNumber(member.getPhoneNumber());
            memberRequest.setEmail(member.getEmail());
            memberRequest.setBirthDate(member.getBirthDate());
            memberRequest.setAddress(member.getAddress());
            model.addAttribute("memberRequest", memberRequest);
            model.addAttribute("memberId", memberId);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + member.getFullName());
            model.addAttribute("contentView", "member-form");
            return "fragments/layout";
        } catch (Exception e) { return "redirect:/members"; }
    }
    @PostMapping("/edit/{memberId}")
    public String processEditMember(@PathVariable("memberId") Long memberId, @Valid @ModelAttribute("memberRequest") MemberRequestDTO memberRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("memberId", memberId);
            model.addAttribute("pageTitle", "Chỉnh sửa Hội viên");
            model.addAttribute("contentView", "member-form");
            return "fragments/layout";
        }
        try {
            memberService.updateMember(memberId, memberRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hội viên thành công!");
            return "redirect:/members";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("memberId", memberId);
            model.addAttribute("pageTitle", "Chỉnh sửa Hội viên");
            model.addAttribute("contentView", "member-form");
            return "fragments/layout";
        }
    }


    // --- MỚI: CÁC HÀM CHO TRANG CHI TIẾT HỘI VIÊN ---

    /**
     * MỚI: Hiển thị trang Chi tiết Hội viên
     */
    @GetMapping("/detail/{memberId}")
    public String getMemberDetailPage(@PathVariable("memberId") Long memberId, Model model) {
        try {
            // 1. Lấy thông tin hội viên
            MemberResponseDTO member = memberService.getMemberById(memberId);
            model.addAttribute("memberProfile", member);

            // 2. Lấy các gói tập của hội viên
            List<SubscriptionResponseDTO> subscriptions = subscriptionService.getSubscriptionsByMemberId(memberId);
            model.addAttribute("memberSubscriptions", subscriptions);

            // 3. Lấy data cho các Form (Modals)
            // 3a. Lấy tất cả gói tập đang bán (để Thêm/Gia hạn)
            List<PackageResponseDTO> allPackages = packageService.getAllPackages().stream()
                    .filter(PackageResponseDTO::isActive) // Chỉ lấy gói đang bán
                    .collect(Collectors.toList());
            model.addAttribute("allPackages", allPackages);

            // 3b. Lấy tất cả PT (để gán gói PT)
            List<UserResponseDTO> allPts = staffService.getAllPts();
            model.addAttribute("allPts", allPts);

            // 3c. Thêm DTO trống cho các form
            if (!model.containsAttribute("subRequest")) {
                model.addAttribute("subRequest", new SubscriptionRequestDTO());
            }
            if (!model.containsAttribute("freezeRequest")) {
                model.addAttribute("freezeRequest", new FreezeRequestDTO());
            }
            if (!model.containsAttribute("logPtRequest")) {
                model.addAttribute("logPtRequest", new com.gym.service.gymmanagementservice.dtos.PtLogRequestDTO());
            }

            model.addAttribute("pageTitle", "Hội viên: " + member.getFullName());
            model.addAttribute("contentView", "member-detail"); // Dùng file member-detail.html
            return "fragments/layout";

        } catch (Exception e) {
            return "redirect:/members";
        }
    }

    /**
     * MỚI: Xử lý Thêm/Gia hạn gói tập
     */
    @PostMapping("/subscription/{action}")
    public String processSubscriptionAction(@PathVariable("action") String action,
                                            @Valid @ModelAttribute("subRequest") SubscriptionRequestDTO request,
                                            BindingResult bindingResult,
                                            RedirectAttributes redirectAttributes) {

        String redirectUrl = "redirect:/members/detail/" + request.getMemberId();

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.subRequest", bindingResult);
            redirectAttributes.addFlashAttribute("subRequest", request);
            return redirectUrl;
        }

        try {
            if ("create".equals(action)) {
                subscriptionService.createSubscription(request);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm gói tập thành công!");
            } else if ("renew".equals(action)) {
                subscriptionService.renewSubscription(request);
                redirectAttributes.addFlashAttribute("successMessage", "Gia hạn gói tập thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return redirectUrl;
    }

    /**
     * MỚI: Xử lý Đóng băng
     */
    @PostMapping("/subscription/freeze/{subId}")
    public String processFreeze(@PathVariable("subId") Long subId,
                                @RequestParam("memberId") Long memberId,
                                @Valid @ModelAttribute("freezeRequest") FreezeRequestDTO request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {

        String redirectUrl = "redirect:/members/detail/" + memberId;
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số ngày đóng băng phải lớn hơn 0.");
            return redirectUrl;
        }
        try {
            subscriptionService.freezeSubscription(subId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Đóng băng gói tập thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return redirectUrl;
    }

    /**
     * MỚI: Xử lý Mở đóng băng
     */
    @PostMapping("/subscription/unfreeze/{subId}")
    public String processUnfreeze(@PathVariable("subId") Long subId,
                                  @RequestParam("memberId") Long memberId,
                                  RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.unfreezeSubscription(subId);
            redirectAttributes.addFlashAttribute("successMessage", "Mở băng gói tập thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    /**
     * MỚI: Xử lý Hủy gói
     */
    @PostMapping("/subscription/cancel/{subId}")
    public String processCancel(@PathVariable("subId") Long subId,
                                @RequestParam("memberId") Long memberId,
                                RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.cancelSubscription(subId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy gói tập thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }

    /**
     * MỚI: Xử lý Ghi buổi tập PT (Log PT)
     */
    @PostMapping("/subscription/log-pt/{subId}")
    public String processLogPtSession(@PathVariable("subId") Long subId,
                                      @RequestParam("memberId") Long memberId,
                                      @ModelAttribute("logPtRequest") com.gym.service.gymmanagementservice.dtos.PtLogRequestDTO request,
                                      RedirectAttributes redirectAttributes) {
        try {
            // (Service PtSessionService.logPtSession được tạo ở Bước 6)
            ptSessionService.logPtSession(subId, request.getNotes());
            redirectAttributes.addFlashAttribute("successMessage", "Đã ghi nhận 1 buổi tập PT.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/members/detail/" + memberId;
    }
}