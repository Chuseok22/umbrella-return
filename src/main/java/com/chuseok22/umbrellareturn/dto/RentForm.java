package com.chuseok22.umbrellareturn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RentForm {

    @NotBlank
    private String borrowerName;

    @NotBlank
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 전화번호 형식이 아닙니다.")
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
