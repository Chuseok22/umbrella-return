package com.chuseok22.umbrellareturn.dto;

import jakarta.validation.constraints.NotBlank;

public class RentForm {

    @NotBlank
    private String borrowerName;

    @NotBlank
    private String borrowerPhone;

    @NotBlank
    private String borrowerStudentId;

    @NotBlank
    private String umbrellaNumber;

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getBorrowerPhone() { return borrowerPhone; }
    public void setBorrowerPhone(String borrowerPhone) { this.borrowerPhone = borrowerPhone; }
    public String getBorrowerStudentId() { return borrowerStudentId; }
    public void setBorrowerStudentId(String borrowerStudentId) { this.borrowerStudentId = borrowerStudentId; }
    public String getUmbrellaNumber() { return umbrellaNumber; }
    public void setUmbrellaNumber(String umbrellaNumber) { this.umbrellaNumber = umbrellaNumber; }
}
