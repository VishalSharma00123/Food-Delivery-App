package com.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order_service.dto.OrderDto;
import com.order_service.dto.OrderRequestDto;
import com.order_service.dto.OrderItemRequestDto;
import com.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_returnsOk() throws Exception {
        OrderRequestDto body = OrderRequestDto.builder()
                .userId(1L)
                .restaurantId(2L)
                .items(List.of(OrderItemRequestDto.builder().menuItemId(10L).quantity(1).build()))
                .build();
        OrderDto response = OrderDto.builder()
                .id(99L)
                .userId(1L)
                .restaurantId(2L)
                .totalAmount(new BigDecimal("12.50"))
                .status("PENDING")
                .items(List.of())
                .build();
        when(orderService.createOrder(any(OrderRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).createOrder(any(OrderRequestDto.class));
    }

    @Test
    void getOrderById_returnsOk() throws Exception {
        OrderDto dto = OrderDto.builder()
                .id(5L)
                .userId(1L)
                .restaurantId(2L)
                .totalAmount(BigDecimal.ONE)
                .status("CONFIRMED")
                .items(List.of())
                .build();
        when(orderService.getOrderById(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(orderService).getOrderById(5L);
    }

    @Test
    void getOrdersByUser_returnsList() throws Exception {
        when(orderService.getOrdersByUserId(7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/user/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(orderService).getOrdersByUserId(7L);
    }

    @Test
    void getOrdersByRestaurant_returnsList() throws Exception {
        when(orderService.getOrdersByRestaurantId(3L)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/restaurant/3"))
                .andExpect(status().isOk());

        verify(orderService).getOrdersByRestaurantId(3L);
    }

    @Test
    void updateOrderStatus_returnsOk() throws Exception {
        OrderDto dto = OrderDto.builder()
                .id(1L)
                .userId(1L)
                .restaurantId(2L)
                .totalAmount(BigDecimal.TEN)
                .status("PREPARING")
                .items(List.of())
                .build();
        when(orderService.updateOrderStatus(eq(1L), eq("PREPARING"))).thenReturn(dto);

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "PREPARING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));

        verify(orderService).updateOrderStatus(1L, "PREPARING");
    }

    @Test
    void cancelOrder_returnsOk() throws Exception {
        mockMvc.perform(post("/api/orders/8/cancel"))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(8L);
    }
}
