package com.notification_service.notification_service.controller;

import com.notification_service.notification_service.dto.NotificationDto;
import com.notification_service.notification_service.entity.NotificationType;
import com.notification_service.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void listForUser_returnsNotifications() throws Exception {
        NotificationDto dto = NotificationDto.builder()
                .id(1L)
                .userId(100L)
                .type(NotificationType.ORDER_PLACED)
                .title("Order placed")
                .body("Your order is confirmed")
                .orderId(10L)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationService.listForUser(eq(100L), eq(true))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/notifications/user/100").param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("ORDER_PLACED"));

        verify(notificationService).listForUser(100L, true);
    }

    @Test
    void listForUser_omittingUnreadOnly_callsServiceWithNullBoolean() throws Exception {
        when(notificationService.listForUser(eq(5L), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications/user/5"))
                .andExpect(status().isOk());

        verify(notificationService).listForUser(eq(5L), isNull());
    }

    @Test
    void markAsRead_returns204_whenUpdated() throws Exception {
        when(notificationService.markAsRead(100L, 1L)).thenReturn(true);

        mockMvc.perform(patch("/api/notifications/user/100/1/read"))
                .andExpect(status().isNoContent());
    }

    @Test
    void markAsRead_returns404_whenNotUpdated() throws Exception {
        when(notificationService.markAsRead(100L, 1L)).thenReturn(false);

        mockMvc.perform(patch("/api/notifications/user/100/1/read")) // =
                .andExpect(status().isNotFound());
    }
}
