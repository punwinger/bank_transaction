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
        request.setUserName("abc");

        Transaction transaction = Transaction.builder()
                .id(1)
                .createTimestamp(System.currentTimeMillis())
                .updateTimestamp(System.currentTimeMillis())
                .userName(request.getUserName())
                .toUserName(request.getToUserName())
                .amount(request.getAmount())
                .type(request.getType())
                .description(request.getDescription())
                .build();

        when(transactionService.createTransaction(any(TransactionRequest.class)))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/v1/users/{userName}/transactions", request.getUserName())
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
        request.setUserName("abc");

        Transaction transaction = Transaction.builder()
                .id(id)
                .amount(request.getAmount())
                .type(request.getType())
                .createTimestamp(System.currentTimeMillis())
                .updateTimestamp(System.currentTimeMillis())
                .userName(request.getUserName())
                .toUserName(request.getToUserName())
                .description(request.getDescription())
                .build();

        when(transactionService.updateTransaction(eq(request.getUserName()), eq(String.valueOf(id)), any(TransactionRequest.class)))
                .thenReturn(transaction);

        mockMvc.perform(put("/api/v1/users/{userName}/transactions/{id}", request.getUserName(), id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.amount").value(transaction.getAmount().doubleValue()))
                .andExpect(jsonPath("$.type").value(transaction.getType().toString()))
                .andExpect(jsonPath("$.description").value(transaction.getDescription()));
    }

    @Test
    void deleteTransaction_ShouldReturnNoContent() throws Exception {
        String id = "3";
        String userName = "abc";
        doNothing().when(transactionService).deleteTransaction(userName, id);

        mockMvc.perform(delete("/api/v1/users/{userName}/transactions/{id}", userName, id))
                .andExpect(status().isNoContent());
    }

    @Test
    void getTransaction_ShouldReturnTransaction() throws Exception {
        long id = 1;
        String userName = "abc";
        Transaction transaction = Transaction.builder()
                .id(id)
                .userName(userName)
                .amount(new BigDecimal("100.00"))
                .type(Transaction.TransactionType.DEPOSIT)
                .description("测试交易")
                .build();

        when(transactionService.getTransaction(transaction.getUserName(), String.valueOf(transaction.getId()))).thenReturn(transaction);

        mockMvc.perform(get("/api/v1/users/{userName}/transactions/{id}", userName, id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transaction.getId()))
                .andExpect(jsonPath("$.amount").value(transaction.getAmount().doubleValue()))
                .andExpect(jsonPath("$.type").value(transaction.getType().toString()));
    }

    @Test
    void getAllTransactions_ShouldReturnPagedTransactions() throws Exception {
        String userName = "abc";
        Transaction transaction = Transaction.builder()
                .id(1)
                .userName(userName)
                .amount(new BigDecimal("100.00"))
                .type(Transaction.TransactionType.DEPOSIT)
                .description("测试交易")
                .build();

        PageImpl<Transaction> page = new PageImpl<>(
                Collections.singletonList(transaction),
                PageRequest.of(0, 10),
                1
        );

        when(transactionService.getAllTransactions(eq(transaction.getUserName()), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users/{userName}/transactions", userName)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(transaction.getId()))
                .andExpect(jsonPath("$.content[0].amount").value(transaction.getAmount().doubleValue()))
                .andExpect(jsonPath("$.content[0].type").value(transaction.getType().toString()));
    }
} 