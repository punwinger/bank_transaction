package com.example.transaction.controller;

import com.example.transaction.dto.TransactionRequest;
import com.example.transaction.model.Transaction;
import com.example.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTransaction_ShouldReturnCreatedTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setType(Transaction.TransactionType.DEPOSIT);
        request.setDescription("测试交易");

        Transaction transaction = Transaction.builder()
                .id(1)
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();

        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.amount").value(transaction.getAmount().doubleValue()))
                .andExpect(jsonPath("$.type").value(transaction.getType().toString()));
    }

    @Test
    void updateTransaction_ShouldReturnUpdatedTransaction() throws Exception {
        long id = 1;
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("200.00"));
        request.setType(Transaction.TransactionType.WITHDRAWAL);
        request.setDescription("更新的测试交易");

        Transaction transaction = Transaction.builder()
                .id(id)
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();

        when(transactionService.updateTransaction(eq(id), any(TransactionRequest.class)))
                .thenReturn(transaction);

        mockMvc.perform(put("/api/v1/transactions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.amount").value(transaction.getAmount().doubleValue()))
                .andExpect(jsonPath("$.type").value(transaction.getType().toString()));
    }

    @Test
    void deleteTransaction_ShouldReturnNoContent() throws Exception {
        String id = "test-id";
        doNothing().when(transactionService).deleteTransaction(id);

        mockMvc.perform(delete("/api/v1/transactions/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTransaction_ShouldReturnTransaction() throws Exception {
        String id = "test-id";
        Transaction transaction = Transaction.builder()
                .id(id)
                .amount(new BigDecimal("100.00"))
                .type(Transaction.TransactionType.DEPOSIT)
                .description("测试交易")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.getTransaction(id)).thenReturn(transaction);

        mockMvc.perform(get("/api/v1/transactions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.amount").value(transaction.getAmount().doubleValue()))
                .andExpect(jsonPath("$.type").value(transaction.getType().toString()));
    }

    @Test
    void getAllTransactions_ShouldReturnPagedTransactions() throws Exception {
        Transaction transaction = Transaction.builder()
                .id("test-id")
                .amount(new BigDecimal("100.00"))
                .type(Transaction.TransactionType.DEPOSIT)
                .description("测试交易")
                .timestamp(LocalDateTime.now())
                .build();

        PageImpl<Transaction> page = new PageImpl<>(
                Collections.singletonList(transaction),
                PageRequest.of(0, 10),
                1
        );

        when(transactionService.getAllTransactions(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(transaction.getId()))
                .andExpect(jsonPath("$.content[0].amount").value(transaction.getAmount().doubleValue()))
                .andExpect(jsonPath("$.content[0].type").value(transaction.getType().toString()));
    }
} 