package com.example.demo.domain.product.form;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductForm {
    @NotEmpty
    private String subject;
    @NotNull
    private int price;
    @NotNull
    private long songId;
}
