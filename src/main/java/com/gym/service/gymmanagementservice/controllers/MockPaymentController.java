package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.models.Transaction;
import com.gym.service.gymmanagementservice.models.TransactionStatus;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-payment")
public class MockPaymentController {

    private final TransactionRepository transactionRepository;

    // Endpoint này tạo ra trang thanh toán giả
    @GetMapping("/pay/{transactionId}")
    public ResponseEntity<String> showPaymentPage(@PathVariable Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giao dịch"));

        String successUrl = "http://localhost:8080/api/mock-payment/webhook?transactionId=" + transactionId + "&status=SUCCESS";
        String failUrl = "http://localhost:8080/api/mock-payment/webhook?transactionId=" + transactionId + "&status=FAILURE";

        // Trả về một trang HTML đơn giản có 2 nút
        String htmlPage = String.format("""
            <html>
                <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
                    <h1>Cổng Thanh toán Demo</h1>
                    <h3>Mã giao dịch: %d</h3>
                    <h3>Số tiền: %,.0f VND</h3>
                    <p>Vui lòng chọn kết quả thanh toán để demo:</p>
                    <a href="%s" style="display: inline-block; padding: 15px 30px; font-size: 16px; color: white; background-color: #28a745; text-decoration: none; border-radius: 5px;">
                        Thanh toán Thành công
                    </a>
                    <a href="%s" style="display: inline-block; padding: 15px 30px; font-size: 16px; color: white; background-color: #dc3545; text-decoration: none; border-radius: 5px; margin-left: 20px;">
                        Thanh toán Thất bại
                    </a>
                </body>
            </html>
            """,
                transaction.getId(),
                transaction.getAmount(),
                successUrl,
                failUrl
        );

        return ResponseEntity.ok(htmlPage);
    }

    // Endpoint này đóng vai trò là Webhook
    @GetMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(@RequestParam Long transactionId, @RequestParam String status) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy giao dịch"));

        if ("SUCCESS".equalsIgnoreCase(status)) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
            return ResponseEntity.ok("<h1>Thanh toán THÀNH CÔNG!</h1><p>Bạn có thể đóng trang này.</p>");
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            return ResponseEntity.ok("<h1>Thanh toán THẤT BẠI!</h1><p>Bạn có thể đóng trang này.</p>");
        }
    }
}