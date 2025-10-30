package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.AdminUpdateUserRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.dtos.UserResponseDTO;
import com.gym.service.gymmanagementservice.models.PackageType; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.models.Product;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.services.PackageService;
import com.gym.service.gymmanagementservice.services.ProductService;
import com.gym.service.gymmanagementservice.services.StaffService;
import com.gym.service.gymmanagementservice.services.WorkScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebController {

    private final StaffService staffService;
    private final PackageService packageService;
    private final ProductService productService;
    private final WorkScheduleService workScheduleService;

    @GetMapping("/users")
    public String getUsersPage(Model model) {
        List<UserResponseDTO> users = staffService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Quản lý Nhân viên");
        model.addAttribute("contentView", "admin/users");
        return "fragments/layout";
    }

    @GetMapping("/users/edit/{userId}")
    public String showEditUserForm(@PathVariable("userId") Long userId, Model model) {
        try {
            UserResponseDTO user = staffService.getUserById(userId);
            if (user.getRole() == Role.MEMBER) {
                return "redirect:/admin/users";
            }
            AdminUpdateUserRequestDTO userRequest = new AdminUpdateUserRequestDTO();
            userRequest.setFullName(user.getFullName());
            userRequest.setRole(user.getRole());
            userRequest.setLocked(user.isLocked());
            List<Role> staffRoles = Arrays.stream(Role.values())
                    .filter(r -> r != Role.MEMBER)
                    .collect(Collectors.toList());
            model.addAttribute("userRequest", userRequest);
            model.addAttribute("userProfile", user);
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + user.getFullName());
            model.addAttribute("contentView", "admin/user-edit");
            return "fragments/layout";
        } catch (Exception e) {
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/users/edit/{userId}")
    public String processEditUser(@PathVariable("userId") Long userId,
                                  @Valid @ModelAttribute("userRequest") AdminUpdateUserRequestDTO userRequest,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            UserResponseDTO user = staffService.getUserById(userId);
            List<Role> staffRoles = Arrays.stream(Role.values())
                    .filter(r -> r != Role.MEMBER)
                    .collect(Collectors.toList());
            model.addAttribute("userProfile", user);
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + user.getFullName());
            model.addAttribute("contentView", "admin/user-edit");
            return "fragments/layout";
        }
        try {
            staffService.updateUserByAdmin(userId, userRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhân viên thành công!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/admin/users/edit/" + userId;
        }
    }

    // --- CÁC HÀM XỬ LÝ GÓI TẬP (PACKAGES) ---

    @GetMapping("/packages")
    public String getPackagesPage(Model model) {
        List<PackageResponseDTO> packages = packageService.getAllPackages();
        model.addAttribute("packages", packages);
        model.addAttribute("pageTitle", "Quản lý Gói tập");
        model.addAttribute("contentView", "admin/packages");
        return "fragments/layout";
    }

    /**
     * MỚI: Hiển thị form TẠO MỚI gói tập
     */
    @GetMapping("/packages/create")
    public String showCreatePackageForm(Model model) {
        model.addAttribute("packageRequest", new PackageRequestDTO());
        model.addAttribute("allPackageTypes", PackageType.values()); // Gửi Enum qua
        model.addAttribute("pageTitle", "Tạo Gói tập mới");
        model.addAttribute("contentView", "admin/package-form"); // Dùng file form mới
        return "fragments/layout";
    }

    /**
     * MỚI: Xử lý TẠO MỚI gói tập
     */
    @PostMapping("/packages/create")
    public String processCreatePackage(@Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        // Nếu validation (từ DTO) thất bại, trả về form
        if (bindingResult.hasErrors()) {
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Tạo Gói tập mới");
            model.addAttribute("contentView", "admin/package-form");
            return "fragments/layout";
        }

        try {
            packageService.createPackage(packageRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo gói tập thành công!");
            return "redirect:/admin/packages";
        } catch (Exception e) {
            // Bắt lỗi logic (vd: trùng tên, sai logic validate)
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Tạo Gói tập mới");
            model.addAttribute("contentView", "admin/package-form");
            return "fragments/layout";
        }
    }

    /**
     * MỚI: Hiển thị form CHỈNH SỬA gói tập
     */
    @GetMapping("/packages/edit/{packageId}")
    public String showEditPackageForm(@PathVariable("packageId") Long packageId, Model model) {
        try {
            PackageResponseDTO pkg = packageService.getPackageById(packageId);

            // Chuyển từ ResponseDTO sang RequestDTO để điền vào form
            PackageRequestDTO packageRequest = new PackageRequestDTO();
            packageRequest.setName(pkg.getName());
            packageRequest.setDescription(pkg.getDescription());
            packageRequest.setPrice(pkg.getPrice());
            packageRequest.setPackageType(pkg.getPackageType());
            packageRequest.setDurationDays(pkg.getDurationDays());
            packageRequest.setNumberOfSessions(pkg.getNumberOfSessions());
            packageRequest.setStartTimeLimit(pkg.getStartTimeLimit());
            packageRequest.setEndTimeLimit(pkg.getEndTimeLimit());

            model.addAttribute("packageRequest", packageRequest);
            model.addAttribute("packageId", packageId); // Để biết là form Sửa
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Chỉnh sửa: " + pkg.getName());
            model.addAttribute("contentView", "admin/package-form");
            return "fragments/layout";
        } catch (Exception e) {
            return "redirect:/admin/packages";
        }
    }

    /**
     * MỚI: Xử lý CHỈNH SỬA gói tập
     */
    @PostMapping("/packages/edit/{packageId}")
    public String processEditPackage(@PathVariable("packageId") Long packageId,
                                     @Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("packageId", packageId);
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Chỉnh sửa Gói tập");
            model.addAttribute("contentView", "admin/package-form");
            return "fragments/layout";
        }

        try {
            packageService.updatePackage(packageId, packageRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật gói tập thành công!");
            return "redirect:/admin/packages";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("packageId", packageId);
            model.addAttribute("allPackageTypes", PackageType.values());
            model.addAttribute("pageTitle", "Chỉnh sửa Gói tập");
            model.addAttribute("contentView", "admin/package-form");
            return "fragments/layout";
        }
    }

    /**
     * MỚI: Xử lý Ngừng/Mở bán (Toggle Status)
     */
    @PostMapping("/packages/toggle/{packageId}")
    public String togglePackageStatus(@PathVariable("packageId") Long packageId, RedirectAttributes redirectAttributes) {
        try {
            packageService.togglePackageStatus(packageId);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/packages";
    }

    @GetMapping("/products")
    public String getProductsPage(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        model.addAttribute("pageTitle", "Quản lý Sản phẩm (POS)");
        model.addAttribute("contentView", "admin/products");
        return "fragments/layout";
    }

    /**
     * MỚI: Hiển thị trang Quản lý Lịch làm việc
     * (Chúng ta sẽ hiển thị 10 lịch mới nhất, bạn có thể đổi logic này để lọc theo tuần/ngày)
     */
    @GetMapping("/schedules")
    public String getSchedulesPage(Model model) {
        // Lấy 10 lịch làm việc gần đây nhất (ví dụ)
        // (Service của bạn cần hàm .findAll() hoặc .findTop10... - tạm dùng hàm cũ)
        List<com.gym.service.gymmanagementservice.dtos.WorkScheduleResponseDTO> schedules = workScheduleService.getSchedules(
                java.time.OffsetDateTime.now().minusDays(30), // Lấy 30 ngày qua
                java.time.OffsetDateTime.now().plusDays(30)  // Và 30 ngày tới
        );

        model.addAttribute("schedules", schedules);
        model.addAttribute("pageTitle", "Quản lý Lịch làm việc");
        model.addAttribute("contentView", "admin/schedules");
        return "fragments/layout";
    }

    /**
     * MỚI: Hiển thị form TẠO MỚI lịch làm việc
     */
    @GetMapping("/schedules/create")
    public String showCreateScheduleForm(Model model) {
        // Lấy danh sách nhân viên (STAFF, PT) để xếp lịch
        List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                .collect(Collectors.toList());

        model.addAttribute("scheduleRequest", new com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO());
        model.addAttribute("staffAndPt", staffAndPt); // Gửi danh sách NV/PT

        model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
        model.addAttribute("contentView", "admin/schedule-form");
        return "fragments/layout";
    }

    /**
     * MỚI: Xử lý TẠO MỚI lịch làm việc
     */
    @PostMapping("/schedules/create")
    public String processCreateSchedule(@Valid @ModelAttribute("scheduleRequest") com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO scheduleRequest,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        if (bindingResult.hasErrors()) {
            // Nếu lỗi, tải lại danh sách nhân viên
            List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                    .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                    .collect(Collectors.toList());
            model.addAttribute("staffAndPt", staffAndPt);

            model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
            model.addAttribute("contentView", "admin/schedule-form");
            return "fragments/layout";
        }

        try {
            workScheduleService.createSchedule(scheduleRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Xếp lịch thành công!");
            return "redirect:/admin/schedules";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());

            List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                    .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                    .collect(Collectors.toList());
            model.addAttribute("staffAndPt", staffAndPt);

            model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
            model.addAttribute("contentView", "admin/schedule-form");
            return "fragments/layout";
        }
    }

    /**
     * MỚI: Xử lý XÓA lịch làm việc
     */
    @PostMapping("/schedules/delete/{scheduleId}")
    public String deleteSchedule(@PathVariable("scheduleId") Long scheduleId, RedirectAttributes redirectAttributes) {
        try {
            workScheduleService.deleteSchedule(scheduleId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa lịch làm việc.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/schedules";
    }
}