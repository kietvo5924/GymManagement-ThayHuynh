package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.dtos.SaleRequestDTO;
import com.gym.service.gymmanagementservice.models.Product;
import com.gym.service.gymmanagementservice.services.MemberService;
import com.gym.service.gymmanagementservice.services.ProductService;
import com.gym.service.gymmanagementservice.services.SaleService;
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
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pos") // Prefix cho trang Bán hàng
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')") // Yêu cầu Admin hoặc Staff
public class SaleWebController {

    private final ProductService productService;
    private final MemberService memberService;
    private final SaleService saleService;

    /**
     * Hiển thị trang POS chính
     */
    @GetMapping
    public String getPosPage(Model model) {
        // 1. Lấy danh sách sản phẩm đang bán (isActive = true)
        List<Product> products = productService.getAllProducts().stream()
                .filter(Product::isActive)
                .collect(Collectors.toList());

        // 2. Lấy danh sách hội viên
        List<MemberResponseDTO> members = memberService.getAllMembers();

        // 3. Chuẩn bị DTO trống cho form
        if (!model.containsAttribute("saleRequest")) {
            model.addAttribute("saleRequest", new SaleRequestDTO());
        }

        model.addAttribute("products", products);
        model.addAttribute("members", members);
        model.addAttribute("pageTitle", "Bán hàng tại quầy (POS)");
        model.addAttribute("contentView", "pos"); // Dùng file pos.html

        // Dùng layout 1 cột (full-width) thay vì layout có sidebar
        // return "fragments/layout";
        // (Chúng ta sẽ sửa lại layout để hỗ trợ full-width sau, tạm dùng layout cũ)
        return "fragments/layout";
    }

    /**
     * Xử lý thanh toán tại quầy
     */
    @PostMapping
    public String processPosSale(@Valid @ModelAttribute("saleRequest") SaleRequestDTO request,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {

        // Kiểm tra DTO (đặc biệt là List<SaleItemDTO> và paymentMethod)
        if (bindingResult.hasErrors() || request.getItems() == null || request.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng trống hoặc chưa chọn hình thức thanh toán.");
            return "redirect:/pos";
        }

        try {
            // Gọi service createPosSale (đã xử lý trừ kho và tạo giao dịch)
            saleService.createPosSale(request);
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
            return "redirect:/pos";
        } catch (Exception e) {
            // Bắt lỗi (vd: Hết tồn kho)
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            // Gửi lại giỏ hàng cũ để người dùng sửa
            redirectAttributes.addFlashAttribute("saleRequest", request);
            return "redirect:/pos";
        }
    }
}