package com.chuseok22.umbrellareturn.dto;

import jakarta.validation.constraints.NotBlank;

public class ReturnForm {

    @NotBlank
    private String borrowerName;

    @NotBlank
    private String borrowerPhone;

    @NotBlank
    private String umbrellaNumber;

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getBorrowerPhone() { return borrowerPhone; }
    public void setBorrowerPhone(String borrowerPhone) { this.borrowerPhone = borrowerPhone; }
    public String getUmbrellaNumber() { return umbrellaNumber; }
    public void setUmbrellaNumber(String umbrellaNumber) { this.umbrellaNumber = umbrellaNumber; }
}
