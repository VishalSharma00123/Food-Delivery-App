package com.payment_service.payment_service.service;

import com.payment_service.payment_service.dto.PaymentDto;
import com.payment_service.payment_service.dto.RazorpayVerifyRequest;
import com.payment_service.payment_service.dto.event.OrderPlacedEvent;
import com.payment_service.payment_service.dto.event.PaymentConfirmedEvent;
import com.payment_service.payment_service.dto.event.PaymentFailedEvent;
import com.payment_service.payment_service.entity.Payment;
import com.payment_service.payment_service.entity.enums.PaymentMethod;
import com.payment_service.payment_service.entity.enums.PaymentStatus;
import com.payment_service.payment_service.kafka.PaymentEventProducer;
import com.payment_service.payment_service.repository.PaymentRepository;
import com.payment_service.payment_service.strategy.PaymentProcessingException;
import com.payment_service.payment_service.strategy.PaymentStrategy;
import com.payment_service.payment_service.strategy.PaymentStrategyFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStrategyFactory strategyFactory;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private RazorpayGatewayService razorpayGatewayService;

    @Mock
    private PaymentInsertService paymentInsertService;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private OrderPlacedEvent orderPlacedEvent;
    private Payment payment;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("admin@test.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(101L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        orderPlacedEvent = OrderPlacedEvent.builder()
                .orderId(1L)
                .userId(101L)
                .totalAmount(new BigDecimal("500.00"))
                .paymentMethod("UPI")
                .timestamp(LocalDateTime.now())
                .build();

        payment = Payment.builder()
                .id(1L)
                .orderId(1L)
                .userId(101L)
                .amount(new BigDecimal("500.00"))
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void processPayment_ShouldSucceed_WhenPaymentMethodNull_defaultsToCod() {
        OrderPlacedEvent codEvent = OrderPlacedEvent.builder()
                .orderId(2L)
                .userId(102L)
                .totalAmount(new BigDecimal("10.00"))
                .paymentMethod(null)
                .timestamp(LocalDateTime.now())
                .build();
        Payment codPayment = Payment.builder()
                .id(2L)
                .orderId(2L)
                .userId(102L)
                .amount(new BigDecimal("10.00"))
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.empty());
        when(paymentInsertService.insertPending(anyLong(), anyLong(), any(), any())).thenReturn(codPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(codPayment);
        when(strategyFactory.getStrategy("COD")).thenReturn(paymentStrategy);
        when(paymentStrategy.pay(any(BigDecimal.class))).thenReturn("TXN-COD");

        PaymentDto result = paymentService.processPayment(codEvent);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        verify(strategyFactory, times(1)).getStrategy("COD");
        verify(paymentEventProducer, times(1)).publishPaymentConfirmed(any(PaymentConfirmedEvent.class));
        verify(razorpayGatewayService, never()).createOrder(anyLong(), any());
    }

    @Test
    void processPayment_onlineMethod_staysPending_andCreatesRazorpayOrder() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(paymentInsertService.insertPending(anyLong(), anyLong(), any(), any())).thenAnswer(invocation -> {
            payment.setId(1L);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setRazorpayOrderId(null);
            return payment;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayGatewayService.createOrder(1L, new BigDecimal("500.00"))).thenReturn("order_test123");

        PaymentDto result = paymentService.processPayment(orderPlacedEvent);

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals("order_test123", result.getRazorpayOrderId());
        verify(strategyFactory, never()).getStrategy(anyString());
        verify(paymentEventProducer, never()).publishPaymentConfirmed(any());
        verify(razorpayGatewayService).createOrder(1L, new BigDecimal("500.00"));
    }

    @Test
    void processPayment_ShouldReturnExisting_WhenDuplicateOrderIdReceived() {
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRazorpayOrderId("order_existing");
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        PaymentDto result = paymentService.processPayment(orderPlacedEvent);

        assertNotNull(result);
        assertEquals("order_existing", result.getRazorpayOrderId());
        verify(paymentInsertService, never()).insertPending(anyLong(), anyLong(), any(), any());
        verify(razorpayGatewayService, never()).createOrder(anyLong(), any());
        verify(strategyFactory, never()).getStrategy(anyString());
    }

    @Test
    void processPayment_onlineMethod_marksFailed_WhenRazorpayCreateFails() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(paymentInsertService.insertPending(anyLong(), anyLong(), any(), any())).thenAnswer(invocation -> {
            payment.setId(1L);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setRazorpayOrderId(null);
            return payment;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayGatewayService.createOrder(1L, new BigDecimal("500.00")))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "rzp down"));

        PaymentDto result = paymentService.processPayment(orderPlacedEvent);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(paymentEventProducer).publishPaymentFailed(any(PaymentFailedEvent.class));
    }

    @Test
    void processPayment_cod_ShouldHandleFailure_WhenStrategyThrowsPaymentProcessingException() {
        OrderPlacedEvent codEvent = OrderPlacedEvent.builder()
                .orderId(1L)
                .userId(101L)
                .totalAmount(new BigDecimal("500.00"))
                .paymentMethod("COD")
                .timestamp(LocalDateTime.now())
                .build();
        Payment codPayment = Payment.builder()
                .id(1L)
                .orderId(1L)
                .userId(101L)
                .amount(new BigDecimal("500.00"))
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(paymentInsertService.insertPending(anyLong(), anyLong(), any(), any())).thenReturn(codPayment);
        when(paymentRepository.save(any(Payment.class))).thenReturn(codPayment);
        when(strategyFactory.getStrategy("COD")).thenReturn(paymentStrategy);
        when(paymentStrategy.pay(any(BigDecimal.class))).thenThrow(new PaymentProcessingException("Gateway timeout"));

        PaymentDto result = paymentService.processPayment(codEvent);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        verify(paymentEventProducer).publishPaymentFailed(any(PaymentFailedEvent.class));
    }

    @Test
    void verifyRazorpayPayment_succeeds_whenSignatureValid() {
        payment.setRazorpayOrderId("order_abc");
        payment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(razorpayGatewayService.verifySignature("order_abc", "pay_xyz", "sig")).thenReturn(true);

        RazorpayVerifyRequest request = RazorpayVerifyRequest.builder()
                .orderId(1L)
                .razorpayOrderId("order_abc")
                .razorpayPaymentId("pay_xyz")
                .razorpaySignature("sig")
                .build();

        PaymentDto result = paymentService.verifyRazorpayPayment(request);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("pay_xyz", result.getTransactionId());
        verify(paymentEventProducer).publishPaymentConfirmed(any(PaymentConfirmedEvent.class));
    }

    @Test
    void refundPayment_ShouldSucceed_WhenPaymentIsSuccessful() {
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentDto result = paymentService.refundPayment(1L);

        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
    }

    @Test
    void refundPayment_ShouldThrowException_WhenPaymentIsNotSuccessful() {
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(ResponseStatusException.class, () -> paymentService.refundPayment(1L));
    }

    @Test
    void getPaymentById_ShouldThrow_WhenMissing() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> paymentService.getPaymentById(99L));
    }

    @Test
    void getPaymentByOrderId_ShouldThrow_WhenMissing() {
        when(paymentRepository.findByOrderId(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> paymentService.getPaymentByOrderId(99L));
    }

    @Test
    void getPaymentsByUserId_mapsResults() {
        Payment second = Payment.builder()
                .id(2L)
                .orderId(3L)
                .userId(101L)
                .amount(new BigDecimal("1.00"))
                .paymentMethod(PaymentMethod.COD)
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByUserId(101L)).thenReturn(List.of(payment, second));

        List<PaymentDto> result = paymentService.getPaymentsByUserId(101L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void refundPayment_ShouldThrow_WhenPaymentIdUnknown() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> paymentService.refundPayment(404L));
    }
}
