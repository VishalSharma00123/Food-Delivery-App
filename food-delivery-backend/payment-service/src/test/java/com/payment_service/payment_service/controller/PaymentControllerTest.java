package com.payment_service.payment_service.controller;

import com.payment_service.payment_service.dto.PaymentDto;
import com.payment_service.payment_service.entity.enums.PaymentMethod;
import com.payment_service.payment_service.entity.enums.PaymentStatus;
import com.payment_service.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getPaymentById_returnsOk() throws Exception {
        PaymentDto dto = PaymentDto.builder()
                .id(1L)
                .orderId(2L)
                .userId(3L)
                .amount(new BigDecimal("10.00"))
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentService.getPaymentById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/payments/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(2))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getPaymentByOrderId_returnsOk() throws Exception {
        PaymentDto dto = PaymentDto.builder()
                .id(5L)
                .orderId(9L)
                .userId(1L)
                .amount(new BigDecimal("5.00"))
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentService.getPaymentByOrderId(9L)).thenReturn(dto);

        mockMvc.perform(get("/api/payments/order/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(9));
    }

    @Test
    void getPaymentsByUserId_returnsList() throws Exception {
        when(paymentService.getPaymentsByUserId(7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/user/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void refundPayment_returnsOk() throws Exception {
        PaymentDto dto = PaymentDto.builder()
                .id(1L)
                .orderId(2L)
                .userId(3L)
                .amount(new BigDecimal("10.00"))
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.REFUNDED)
                .build();
        when(paymentService.refundPayment(anyLong())).thenReturn(dto);

        mockMvc.perform(post("/api/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
