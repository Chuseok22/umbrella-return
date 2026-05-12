package com.chuseok22.umbrellareturn.dto;

import jakarta.validation.constraints.NotBlank;

public class UmbrellaForm {

    @NotBlank
    private String number;

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
}
