package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.exception.CustomException;
import com.chuseok22.umbrellareturn.service.RentalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rentals")
public class AdminRentalController {

    private static final Logger log = LoggerFactory.getLogger(AdminRentalController.class);

    private final RentalService rentalService;

    public AdminRentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rentals", rentalService.findActiveRentals());
        return "admin/rentals";
    }

    @PostMapping("/{id}/return")
    public String adminReturn(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rentalService.adminReturn(id);
            redirectAttributes.addFlashAttribute("successMessage", "반납 처리되었습니다.");
        } catch (CustomException e) {
            log.warn("관리자 반납 처리 실패: rentalId={}, code={}", id, e.getErrorCode());
            redirectAttributes.addFlashAttribute("errorMessage", e.getErrorCode().getMessage());
        }
        return "redirect:/admin/rentals";
    }
}
