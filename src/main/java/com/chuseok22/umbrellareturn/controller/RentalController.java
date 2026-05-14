package com.chuseok22.umbrellareturn.controller;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.dto.ReturnForm;
import com.chuseok22.umbrellareturn.exception.CustomException;
import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RentalController {

    private static final Logger log = LoggerFactory.getLogger(RentalController.class);

    private final RentalService rentalService;
    private final UmbrellaService umbrellaService;

    public RentalController(RentalService rentalService, UmbrellaService umbrellaService) {
        this.rentalService = rentalService;
        this.umbrellaService = umbrellaService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/rent")
    public String rentForm(Model model) {
        model.addAttribute("rentForm", new RentForm());
        model.addAttribute("availableUmbrellas", umbrellaService.findAvailable());
        return "rent";
    }

    @PostMapping("/rent")
    public String rent(@Valid @ModelAttribute RentForm rentForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("availableUmbrellas", umbrellaService.findAvailable());
            return "rent";
        }
        try {
            rentalService.rent(rentForm);
            redirectAttributes.addFlashAttribute("type", "rent");
            redirectAttributes.addFlashAttribute("name", rentForm.getBorrowerName());
            redirectAttributes.addFlashAttribute("umbrellaNumber", rentForm.getUmbrellaNumber());
            return "redirect:/complete";
        } catch (CustomException e) {
            log.warn("대여 처리 실패: umbrella={}, code={}", rentForm.getUmbrellaNumber(), e.getErrorCode());
            model.addAttribute("errorMessage", e.getErrorCode().getMessage());
            model.addAttribute("availableUmbrellas", umbrellaService.findAvailable());
            return "rent";
        }
    }

    @GetMapping("/return")
    public String returnForm(Model model) {
        model.addAttribute("returnForm", new ReturnForm());
        model.addAttribute("rentedUmbrellas", umbrellaService.findRented());
        return "return";
    }

    @PostMapping("/return")
    public String returnUmbrella(@Valid @ModelAttribute ReturnForm returnForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rentedUmbrellas", umbrellaService.findRented());
            return "return";
        }
        try {
            rentalService.returnUmbrella(returnForm);
            redirectAttributes.addFlashAttribute("type", "return");
            redirectAttributes.addFlashAttribute("name", returnForm.getBorrowerName());
            redirectAttributes.addFlashAttribute("umbrellaNumber", returnForm.getUmbrellaNumber());
            return "redirect:/complete";
        } catch (CustomException e) {
            log.warn("반납 처리 실패: umbrella={}, code={}", returnForm.getUmbrellaNumber(), e.getErrorCode());
            model.addAttribute("errorMessage", e.getErrorCode().getMessage());
            model.addAttribute("rentedUmbrellas", umbrellaService.findRented());
            return "return";
        }
    }

    @GetMapping("/complete")
    public String complete(Model model) {
        // Flash Attribute 없이 직접 접근한 경우 메인으로 리다이렉트
        if (!model.containsAttribute("type")) {
            return "redirect:/";
        }
        return "complete";
    }
}
