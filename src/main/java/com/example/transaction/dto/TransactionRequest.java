package com.example.transaction.dto;

import com.example.transaction.model.Transaction.TransactionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 20, message = "用户名长度必须在1-20之间")
    private String userName;

    @Size(min = 1, max = 20, message = "接收方用户名长度必须在1-20之间")
    private String toUserName;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    @DecimalMax(value = "999999999.99", message = "金额不能超过999,999,999.99")
    @Digits(integer = 9, fraction = 2, message = "金额整数部分不能超过9位，小数部分不能超过2位")
    private BigDecimal amount;

    @NotNull(message = "交易类型不能为空")
    private TransactionType type;

    @Size(max = 20, message = "描述长度不能超过20")
    private String description;
} 