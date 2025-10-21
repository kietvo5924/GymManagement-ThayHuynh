package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.SaleItemDTO;
import com.gym.service.gymmanagementservice.dtos.SaleRequestDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SaleService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final AuthenticationService authenticationService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Sale createSale(SaleRequestDTO request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member member = null;
        if (request.getMemberId() != null) {
            member = memberRepository.findById(request.getMemberId()).orElse(null);
        }

        Sale sale = Sale.builder()
                .user(currentUser)
                .member(member)
                .saleDate(OffsetDateTime.now())
                .saleDetails(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SaleItemDTO item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Sản phẩm không tồn tại: " + item.getProductId()));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Không đủ tồn kho cho sản phẩm: " + product.getName());
            }

            // Trừ tồn kho
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);

            SaleDetail detail = SaleDetail.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(item.getQuantity())
                    .priceAtSale(product.getPrice())
                    .build();

            sale.getSaleDetails().add(detail);
            totalAmount = totalAmount.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        sale.setTotalAmount(totalAmount);
        Sale savedSale = saleRepository.save(sale);

        Transaction transaction = Transaction.builder()
                .amount(savedSale.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .sale(savedSale)
                .build();
        transactionRepository.save(transaction);

        return savedSale;
    }
}