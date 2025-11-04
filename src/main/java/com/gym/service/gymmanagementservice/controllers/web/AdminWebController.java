package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.*;
import com.gym.service.gymmanagementservice.models.*; // <-- SỬA IMPORT
import com.gym.service.gymmanagementservice.repositories.AmenityRepository; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.repositories.ClubRepository; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.services.*;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set; // <-- IMPORT MỚI
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Hidden
public class AdminWebController {

    private final StaffService staffService;
    private final PackageService packageService;
    private final ProductService productService;
    private final WorkScheduleService workScheduleService;
    private final AuthenticationService authenticationService;
    private final ReportService reportService;
    private final ClubRepository clubRepository;
    private final AmenityRepository amenityRepository;
    private final ClubService clubService;
    private final AmenityService amenityService;
    private final ClassDefinitionService classDefinitionService;
    private final ScheduledClassService scheduledClassService;
    private final PtBookingService ptBookingService;

    // ... (Các hàm của User giữ nguyên, tôi sẽ ẩn đi cho gọn) ...
    @GetMapping("/users")
    public String getUsersPage(Model model) {
        List<UserResponseDTO> users = staffService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("pageTitle", "Quản lý Nhân viên");
        model.addAttribute("contentView", "admin/users");
        model.addAttribute("activePage", "adminUsers");
        return "fragments/layout";
    }
    @GetMapping("/users/edit/{userId}")
    public String showEditUserForm(@PathVariable("userId") Long userId, Model model) {
        try {
            UserResponseDTO user = staffService.getUserById(userId);
            if (user.getRole() == Role.MEMBER) { return "redirect:/admin/users"; }
            AdminUpdateUserRequestDTO userRequest = new AdminUpdateUserRequestDTO();
            userRequest.setFullName(user.getFullName());
            userRequest.setRole(user.getRole());
            userRequest.setLocked(user.isLocked());
            List<Role> staffRoles = Arrays.stream(Role.values()).filter(r -> r != Role.MEMBER).collect(Collectors.toList());
            model.addAttribute("userRequest", userRequest);
            model.addAttribute("userProfile", user);
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + user.getFullName());
            model.addAttribute("contentView", "admin/user-edit");
            model.addAttribute("activePage", "adminUsers");
            return "fragments/layout";
        } catch (Exception e) { return "redirect:/admin/users"; }
    }
    @PostMapping("/users/edit/{userId}")
    public String processEditUser(@PathVariable("userId") Long userId, @Valid @ModelAttribute("userRequest") AdminUpdateUserRequestDTO userRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            UserResponseDTO user = staffService.getUserById(userId);
            List<Role> staffRoles = Arrays.stream(Role.values()).filter(r -> r != Role.MEMBER).collect(Collectors.toList());
            model.addAttribute("userProfile", user);
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Chỉnh sửa: " + user.getFullName());
            model.addAttribute("contentView", "admin/user-edit");
            model.addAttribute("activePage", "adminUsers");
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
    @PostMapping("/users/toggle-lock/{userId}")
    public String toggleUserLock(@PathVariable("userId") Long userId, RedirectAttributes redirectAttributes) {
        try {
            staffService.toggleUserLockStatus(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái tài khoản.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
    @GetMapping("/users/create")
    public String showCreateUserForm(Model model) {
        List<Role> staffRoles = Arrays.stream(Role.values())
                .filter(r -> r != Role.MEMBER)
                .collect(Collectors.toList());
        model.addAttribute("userRequest", new com.gym.service.gymmanagementservice.dtos.AdminCreateUserRequestDTO());
        model.addAttribute("allRoles", staffRoles);
        model.addAttribute("pageTitle", "Tạo Nhân viên mới");
        model.addAttribute("contentView", "admin/user-create");
        model.addAttribute("activePage", "adminUsers");
        return "fragments/layout";
    }
    @PostMapping("/users/create")
    public String processCreateUser(@Valid @ModelAttribute("userRequest") com.gym.service.gymmanagementservice.dtos.AdminCreateUserRequestDTO userRequest,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            List<Role> staffRoles = Arrays.stream(Role.values())
                    .filter(r -> r != Role.MEMBER)
                    .collect(Collectors.toList());
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Tạo Nhân viên mới");
            model.addAttribute("contentView", "admin/user-create");
            model.addAttribute("activePage", "adminUsers");
            return "fragments/layout";
        }
        try {
            authenticationService.createStaffAccount(userRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản nhân viên thành công!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            List<Role> staffRoles = Arrays.stream(Role.values())
                    .filter(r -> r != Role.MEMBER)
                    .collect(Collectors.toList());
            model.addAttribute("allRoles", staffRoles);
            model.addAttribute("pageTitle", "Tạo Nhân viên mới");
            model.addAttribute("contentView", "admin/user-create");
            model.addAttribute("activePage", "adminUsers");
            return "fragments/layout";
        }
    }

    // --- CÁC HÀM XỬ LÝ GÓI TẬP (PACKAGES) ---

    @GetMapping("/packages")
    public String getPackagesPage(Model model) {
        List<PackageResponseDTO> packages = packageService.getAllPackages();
        model.addAttribute("packages", packages);
        model.addAttribute("pageTitle", "Quản lý Gói tập");
        model.addAttribute("contentView", "admin/packages");
        model.addAttribute("activePage", "adminPackages");
        return "fragments/layout";
    }

    // Hàm private để load dữ liệu cho form Gói tập
    private void loadPackageFormData(Model model) {
        model.addAttribute("allPackageTypes", PackageType.values());
        model.addAttribute("allAccessTypes", PackageAccessType.values()); // MỚI
        model.addAttribute("allClubs", clubRepository.findAllByIsActive(true)); // MỚI
        model.addAttribute("allAmenities", amenityRepository.findAll()); // MỚI
    }

    @GetMapping("/packages/create")
    public String showCreatePackageForm(Model model) {
        model.addAttribute("packageRequest", new PackageRequestDTO());
        loadPackageFormData(model); // Load dữ liệu
        model.addAttribute("pageTitle", "Tạo Gói tập mới");
        model.addAttribute("contentView", "admin/package-form");
        model.addAttribute("activePage", "adminPackages");
        return "fragments/layout";
    }

    @PostMapping("/packages/create")
    public String processCreatePackage(@Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            loadPackageFormData(model); // Load lại dữ liệu khi có lỗi
            model.addAttribute("pageTitle", "Tạo Gói tập mới");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
        try {
            packageService.createPackage(packageRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo gói tập thành công!");
            return "redirect:/admin/packages";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            loadPackageFormData(model); // Load lại dữ liệu khi có lỗi
            model.addAttribute("pageTitle", "Tạo Gói tập mới");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
    }

    @GetMapping("/packages/edit/{packageId}")
    public String showEditPackageForm(@PathVariable("packageId") Long packageId, Model model) {
        try {
            PackageResponseDTO pkg = packageService.getPackageById(packageId);

            // Chuyển từ DTO response sang DTO request
            PackageRequestDTO packageRequest = new PackageRequestDTO();
            packageRequest.setName(pkg.getName());
            packageRequest.setDescription(pkg.getDescription());
            packageRequest.setPrice(pkg.getPrice());
            packageRequest.setPackageType(pkg.getPackageType());
            packageRequest.setDurationDays(pkg.getDurationDays());
            packageRequest.setNumberOfSessions(pkg.getNumberOfSessions());
            packageRequest.setStartTimeLimit(pkg.getStartTimeLimit());
            packageRequest.setEndTimeLimit(pkg.getEndTimeLimit());
            // --- CÁC TRƯỜNG MỚI ---
            packageRequest.setAccessType(pkg.getAccessType());
            packageRequest.setTargetClubId(pkg.getTargetClubId()); // Lấy ID từ DTO
            packageRequest.setAmenityIds(pkg.getAmenities().stream() // Lấy Set<ID> từ Set<DTO>
                    .map(PackageResponseDTO.AmenityDTO::getId)
                    .collect(Collectors.toSet()));

            model.addAttribute("packageRequest", packageRequest);
            model.addAttribute("packageId", packageId);
            loadPackageFormData(model); // Load dữ liệu
            model.addAttribute("pageTitle", "Chỉnh sửa: " + pkg.getName());
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        } catch (Exception e) { return "redirect:/admin/packages"; }
    }

    @PostMapping("/packages/edit/{packageId}")
    public String processEditPackage(@PathVariable("packageId") Long packageId, @Valid @ModelAttribute("packageRequest") PackageRequestDTO packageRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("packageId", packageId);
            loadPackageFormData(model); // Load lại dữ liệu khi có lỗi
            model.addAttribute("pageTitle", "Chỉnh sửa Gói tập");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
        try {
            packageService.updatePackage(packageId, packageRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật gói tập thành công!");
            return "redirect:/admin/packages";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("packageId", packageId);
            loadPackageFormData(model); // Load lại dữ liệu khi có lỗi
            model.addAttribute("pageTitle", "Chỉnh sửa Gói tập");
            model.addAttribute("contentView", "admin/package-form");
            model.addAttribute("activePage", "adminPackages");
            return "fragments/layout";
        }
    }

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

    // ... (Các hàm của Product, Schedule, Report giữ nguyên, tôi sẽ ẩn đi cho gọn) ...
    // --- CÁC HÀM XỬ LÝ SẢN PHẨM (PRODUCTS) ---
    @GetMapping("/products")
    public String getProductsPage(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        model.addAttribute("pageTitle", "Quản lý Sản phẩm (POS)");
        model.addAttribute("contentView", "admin/products");
        model.addAttribute("activePage", "adminProducts");
        return "fragments/layout";
    }
    @GetMapping("/products/create")
    public String showCreateProductForm(Model model) {
        model.addAttribute("productRequest", new ProductRequestDTO());
        model.addAttribute("pageTitle", "Tạo Sản phẩm mới");
        model.addAttribute("contentView", "admin/product-form"); // Dùng file form mới
        model.addAttribute("activePage", "adminProducts");
        return "fragments/layout";
    }
    @PostMapping("/products/create")
    public String processCreateProduct(@Valid @ModelAttribute("productRequest") ProductRequestDTO productRequest,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo Sản phẩm mới");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
        try {
            productService.createProduct(productRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("pageTitle", "Tạo Sản phẩm mới");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
    }
    @GetMapping("/products/edit/{productId}")
    public String showEditProductForm(@PathVariable("productId") Long productId, Model model) {
        try {
            Product product = productService.getProductById(productId);
            ProductRequestDTO productRequest = new ProductRequestDTO();
            productRequest.setName(product.getName());
            productRequest.setPrice(product.getPrice());
            productRequest.setStockQuantity(product.getStockQuantity());
            model.addAttribute("productRequest", productRequest);
            model.addAttribute("productId", productId); // Để biết là form Sửa
            model.addAttribute("pageTitle", "Chỉnh sửa: " + product.getName());
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        } catch (Exception e) {
            return "redirect:/admin/products";
        }
    }
    @PostMapping("/products/edit/{productId}")
    public String processEditProduct(@PathVariable("productId") Long productId,
                                     @Valid @ModelAttribute("productRequest") ProductRequestDTO productRequest,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", productId);
            model.addAttribute("pageTitle", "Chỉnh sửa Sản phẩm");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
        try {
            productService.updateProduct(productId, productRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            model.addAttribute("productId", productId);
            model.addAttribute("pageTitle", "Chỉnh sửa Sản phẩm");
            model.addAttribute("contentView", "admin/product-form");
            model.addAttribute("activePage", "adminProducts");
            return "fragments/layout";
        }
    }
    @PostMapping("/products/toggle/{productId}")
    public String toggleProductStatus(@PathVariable("productId") Long productId, RedirectAttributes redirectAttributes) {
        try {
            productService.toggleProductStatus(productId);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái sản phẩm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
    // --- SCHEDULE MANAGEMENT ---
    @GetMapping("/schedules")
    public String getSchedulesPage(Model model) {
        List<com.gym.service.gymmanagementservice.dtos.WorkScheduleResponseDTO> schedules = workScheduleService.getSchedules(
                java.time.OffsetDateTime.now().minusDays(30),
                java.time.OffsetDateTime.now().plusDays(30)
        );
        model.addAttribute("schedules", schedules);
        model.addAttribute("pageTitle", "Quản lý Lịch làm việc");
        model.addAttribute("contentView", "admin/schedules");
        model.addAttribute("activePage", "adminSchedules");
        return "fragments/layout";
    }
    @GetMapping("/schedules/create")
    public String showCreateScheduleForm(Model model) {
        List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                .collect(Collectors.toList());
        model.addAttribute("scheduleRequest", new com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO());
        model.addAttribute("staffAndPt", staffAndPt);
        model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
        model.addAttribute("contentView", "admin/schedule-form");
        model.addAttribute("activePage", "adminSchedules");
        return "fragments/layout";
    }
    @PostMapping("/schedules/create")
    public String processCreateSchedule(@Valid @ModelAttribute("scheduleRequest") com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO scheduleRequest,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {
        if (bindingResult.hasErrors()) {
            List<UserResponseDTO> staffAndPt = staffService.getAllUsers().stream()
                    .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                    .collect(Collectors.toList());
            model.addAttribute("staffAndPt", staffAndPt);
            model.addAttribute("pageTitle", "Xếp lịch làm việc mới");
            model.addAttribute("contentView", "admin/schedule-form");
            model.addAttribute("activePage", "adminSchedules");
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
            model.addAttribute("activePage", "adminSchedules");
            return "fragments/layout";
        }
    }
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
    // --- CÁC HÀM XỬ LÝ BÁO CÁO (REPORTS) ---
    @GetMapping("/reports/sales")
    public String getSaleReportPage(Model model) {
        List<com.gym.service.gymmanagementservice.dtos.TransactionReportDTO> reportData = reportService.getFullTransactionReport();
        java.math.BigDecimal totalRevenue = reportData.stream()
                .filter(tx -> tx.getStatus() == com.gym.service.gymmanagementservice.models.TransactionStatus.COMPLETED)
                .map(com.gym.service.gymmanagementservice.dtos.TransactionReportDTO::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("reportData", reportData);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pageTitle", "Báo cáo Doanh thu");
        model.addAttribute("contentView", "admin/sales-report");
        model.addAttribute("activePage", "adminReports");
        return "fragments/layout";
    }

    // --- CÁC HÀM MỚI ĐỂ QUẢN LÝ CLB (CHI NHÁNH) ---

    @GetMapping("/clubs")
    public String getClubsPage(Model model) {
        model.addAttribute("clubs", clubService.getAllClubs());
        model.addAttribute("pageTitle", "Quản lý Chi nhánh (Club)");
        model.addAttribute("contentView", "admin/clubs");
        model.addAttribute("activePage", "adminClubs"); // Dùng cho layout
        return "fragments/layout";
    }

    @GetMapping("/clubs/create")
    public String showCreateClubForm(Model model) {
        model.addAttribute("club", new Club());
        model.addAttribute("pageTitle", "Tạo Chi nhánh mới");
        model.addAttribute("contentView", "admin/club-form");
        model.addAttribute("activePage", "adminClubs");
        return "fragments/layout";
    }

    @PostMapping("/clubs/create")
    public String processCreateClub(@Valid @ModelAttribute("club") Club club, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo Chi nhánh mới");
            model.addAttribute("contentView", "admin/club-form");
            model.addAttribute("activePage", "adminClubs");
            return "fragments/layout";
        }
        clubService.createClub(club);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo chi nhánh thành công!");
        return "redirect:/admin/clubs";
    }

    @GetMapping("/clubs/edit/{clubId}")
    public String showEditClubForm(@PathVariable("clubId") Long clubId, Model model) {
        model.addAttribute("club", clubService.getClubById(clubId));
        model.addAttribute("pageTitle", "Chỉnh sửa Chi nhánh");
        model.addAttribute("contentView", "admin/club-form");
        model.addAttribute("activePage", "adminClubs");
        return "fragments/layout";
    }

    @PostMapping("/clubs/edit/{clubId}")
    public String processEditClub(@PathVariable("clubId") Long clubId, @Valid @ModelAttribute("club") Club club, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Chỉnh sửa Chi nhánh");
            model.addAttribute("contentView", "admin/club-form");
            model.addAttribute("activePage", "adminClubs");
            return "fragments/layout";
        }
        clubService.updateClub(clubId, club);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật chi nhánh thành công!");
        return "redirect:/admin/clubs";
    }

    @PostMapping("/clubs/toggle/{clubId}")
    public String toggleClubStatus(@PathVariable("clubId") Long clubId, RedirectAttributes redirectAttributes) {
        clubService.toggleClubStatus(clubId);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái chi nhánh thành công.");
        return "redirect:/admin/clubs";
    }

    // --- CÁC HÀM MỚI ĐỂ QUẢN LÝ AMENITY (TIỆN ÍCH) ---

    @GetMapping("/amenities")
    public String getAmenitiesPage(Model model) {
        model.addAttribute("amenities", amenityService.getAllAmenities());
        model.addAttribute("pageTitle", "Quản lý Tiện ích (Amenity)");
        model.addAttribute("contentView", "admin/amenities");
        model.addAttribute("activePage", "adminAmenities");
        return "fragments/layout";
    }

    @GetMapping("/amenities/create")
    public String showCreateAmenityForm(Model model) {
        model.addAttribute("amenity", new Amenity());
        model.addAttribute("pageTitle", "Tạo Tiện ích mới");
        model.addAttribute("contentView", "admin/amenity-form");
        model.addAttribute("activePage", "adminAmenities");
        return "fragments/layout";
    }

    @PostMapping("/amenities/create")
    public String processCreateAmenity(@Valid @ModelAttribute("amenity") Amenity amenity, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo Tiện ích mới");
            model.addAttribute("contentView", "admin/amenity-form");
            model.addAttribute("activePage", "adminAmenities");
            return "fragments/layout";
        }
        amenityService.createAmenity(amenity);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo tiện ích thành công!");
        return "redirect:/admin/amenities";
    }

    @GetMapping("/amenities/edit/{amenityId}")
    public String showEditAmenityForm(@PathVariable("amenityId") Long amenityId, Model model) {
        model.addAttribute("amenity", amenityService.getAmenityById(amenityId));
        model.addAttribute("pageTitle", "Chỉnh sửa Tiện ích");
        model.addAttribute("contentView", "admin/amenity-form");
        model.addAttribute("activePage", "adminAmenities");
        return "fragments/layout";
    }

    @PostMapping("/amenities/edit/{amenityId}")
    public String processEditAmenity(@PathVariable("amenityId") Long amenityId, @Valid @ModelAttribute("amenity") Amenity amenity, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Chỉnh sửa Tiện ích");
            model.addAttribute("contentView", "admin/amenity-form");
            model.addAttribute("activePage", "adminAmenities");
            return "fragments/layout";
        }
        amenityService.updateAmenity(amenityId, amenity);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tiện ích thành công!");
        return "redirect:/admin/amenities";
    }

    @PostMapping("/amenities/delete/{amenityId}")
    public String deleteAmenity(@PathVariable("amenityId") Long amenityId, RedirectAttributes redirectAttributes) {
        try {
            amenityService.deleteAmenity(amenityId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tiện ích.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Không thể xóa tiện ích này (có thể đã được gán cho 1 gói tập).");
        }
        return "redirect:/admin/amenities";
    }

    // --- HÀM MỚI: QUẢN LÝ LOẠI LỚP (Class Definition) ---

    @GetMapping("/class-definitions")
    public String getClassDefinitionsPage(Model model) {
        model.addAttribute("definitions", classDefinitionService.getAll());
        model.addAttribute("pageTitle", "Quản lý Các Loại Lớp");
        model.addAttribute("contentView", "admin/class-definitions");
        model.addAttribute("activePage", "adminClassDefinitions");
        return "fragments/layout";
    }

    @GetMapping("/class-definitions/create")
    public String showCreateClassDefinitionForm(Model model) {
        model.addAttribute("classDef", new ClassDefinition());
        model.addAttribute("pageTitle", "Tạo Loại Lớp mới");
        model.addAttribute("contentView", "admin/class-definition-form");
        model.addAttribute("activePage", "adminClassDefinitions");
        return "fragments/layout";
    }

    @PostMapping("/class-definitions/create")
    public String processCreateClassDefinition(@Valid @ModelAttribute("classDef") ClassDefinition classDef, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Tạo Loại Lớp mới");
            model.addAttribute("contentView", "admin/class-definition-form");
            model.addAttribute("activePage", "adminClassDefinitions");
            return "fragments/layout";
        }
        classDefinitionService.create(classDef);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo loại lớp thành công!");
        return "redirect:/admin/class-definitions";
    }

    @GetMapping("/class-definitions/edit/{id}")
    public String showEditClassDefinitionForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("classDef", classDefinitionService.getById(id));
        model.addAttribute("pageTitle", "Chỉnh sửa Loại Lớp");
        model.addAttribute("contentView", "admin/class-definition-form");
        model.addAttribute("activePage", "adminClassDefinitions");
        return "fragments/layout";
    }

    @PostMapping("/class-definitions/edit/{id}")
    public String processEditClassDefinition(@PathVariable("id") Long id, @Valid @ModelAttribute("classDef") ClassDefinition classDef, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Chỉnh sửa Loại Lớp");
            model.addAttribute("contentView", "admin/class-definition-form");
            model.addAttribute("activePage", "adminClassDefinitions");
            return "fragments/layout";
        }
        classDefinitionService.update(id, classDef);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật loại lớp thành công!");
        return "redirect:/admin/class-definitions";
    }

    @PostMapping("/class-definitions/toggle/{id}")
    public String toggleClassDefinitionStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        classDefinitionService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái thành công.");
        return "redirect:/admin/class-definitions";
    }


    // --- HÀM MỚI: QUẢN LÝ LỊCH LỚP (Scheduled Class) ---

    private void loadScheduledClassFormData(Model model) {
        // Lấy danh sách HLV (Bao gồm PT và STAFF)
        List<UserResponseDTO> instructors = staffService.getAllUsers().stream()
                .filter(user -> user.getRole() == Role.STAFF || user.getRole() == Role.PT)
                .collect(Collectors.toList());

        model.addAttribute("allClassDefinitions", classDefinitionService.getAllActive());
        model.addAttribute("allInstructors", instructors);
        model.addAttribute("allClubs", clubService.getAllActiveClubs());
    }

    @GetMapping("/classes")
    public String getScheduledClassesPage(Model model) {
        // Lấy lịch lớp trong 30 ngày tới
        List<ScheduledClassResponseDTO> classes = scheduledClassService.getAll(
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(30)
        );
        model.addAttribute("classes", classes);
        model.addAttribute("pageTitle", "Quản lý Lịch Lớp");
        model.addAttribute("contentView", "admin/classes");
        model.addAttribute("activePage", "adminClasses");
        return "fragments/layout";
    }

    @GetMapping("/classes/create")
    public String showCreateScheduledClassForm(Model model) {
        model.addAttribute("classRequest", new ScheduledClassRequestDTO());
        loadScheduledClassFormData(model);
        model.addAttribute("pageTitle", "Tạo Lịch Lớp mới");
        model.addAttribute("contentView", "admin/class-form");
        model.addAttribute("activePage", "adminClasses");
        return "fragments/layout";
    }

    @PostMapping("/classes/create")
    public String processCreateScheduledClass(@Valid @ModelAttribute("classRequest") ScheduledClassRequestDTO classRequest, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            loadScheduledClassFormData(model);
            model.addAttribute("pageTitle", "Tạo Lịch Lớp mới");
            model.addAttribute("contentView", "admin/class-form");
            model.addAttribute("activePage", "adminClasses");
            return "fragments/layout";
        }
        try {
            scheduledClassService.create(classRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch lớp thành công!");
            return "redirect:/admin/classes";
        } catch (Exception e) {
            bindingResult.reject("globalError", e.getMessage());
            loadScheduledClassFormData(model);
            model.addAttribute("pageTitle", "Tạo Lịch Lớp mới");
            model.addAttribute("contentView", "admin/class-form");
            model.addAttribute("activePage", "adminClasses");
            return "fragments/layout";
        }
    }

    @PostMapping("/classes/delete/{id}")
    public String deleteScheduledClass(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            scheduledClassService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa lịch lớp.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/classes";
    }

    // --- CÁC HÀM MỚI ĐỂ QUẢN LÝ LỊCH PT (PT BOOKING) ---

    @GetMapping("/pt-bookings")
    public String getPtBookingsPage(Model model) {
        model.addAttribute("bookings", ptBookingService.getAllBookings());
        model.addAttribute("pageTitle", "Quản lý Lịch hẹn PT");
        model.addAttribute("contentView", "admin/pt-bookings");
        model.addAttribute("activePage", "adminPtBookings");
        return "fragments/layout";
    }

    // Xác nhận (do Admin)
    @PostMapping("/pt-bookings/confirm/{id}")
    public String confirmPtBooking(@PathVariable("id") Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            ptBookingService.confirmBooking(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận lịch hẹn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/pt-bookings";
    }

    // Hủy (do Admin)
    @PostMapping("/pt-bookings/cancel/{id}")
    public String cancelPtBooking(@PathVariable("id") Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            ptBookingService.cancelPtBooking(bookingId); // Dùng hàm cancel của PT
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy lịch hẹn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/pt-bookings";
    }

    // Hiển thị form "Hoàn thành"
    @GetMapping("/pt-bookings/complete/{id}")
    public String showCompletePtBookingForm(@PathVariable("id") Long bookingId, Model model) {
        try {
            PtBookingResponseDTO booking = ptBookingService.getBookingsByMemberId(bookingId).stream().findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy booking"));

            model.addAttribute("booking", booking);
            model.addAttribute("notesRequest", new PtLogRequestDTO()); // Dùng DTO cũ
            model.addAttribute("pageTitle", "Hoàn thành buổi tập");
            model.addAttribute("contentView", "admin/pt-booking-complete");
            model.addAttribute("activePage", "adminPtBookings");
            return "fragments/layout";
        } catch (Exception e) {
            return "redirect:/admin/pt-bookings";
        }
    }

    // Xử lý "Hoàn thành"
    @PostMapping("/pt-bookings/complete/{id}")
    public String processCompletePtBooking(@PathVariable("id") Long bookingId,
                                           @ModelAttribute("notesRequest") PtLogRequestDTO notesRequest,
                                           RedirectAttributes redirectAttributes) {
        try {
            String notes = (notesRequest != null && notesRequest.getNotes() != null) ? notesRequest.getNotes() : "Hoàn thành bởi Admin";
            ptBookingService.completeBooking(bookingId, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hoàn thành & trừ buổi tập của hội viên.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/pt-bookings";
    }
}