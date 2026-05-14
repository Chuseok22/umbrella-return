package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UmbrellaService umbrellaService;
    private final RentalService rentalService;

    public AdminDashboardController(UmbrellaService umbrellaService, RentalService rentalService) {
        this.umbrellaService = umbrellaService;
        this.rentalService = rentalService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUmbrellas", umbrellaService.countTotal());
        model.addAttribute("availableUmbrellas", umbrellaService.countAvailable());
        model.addAttribute("activeRentals", rentalService.countActive());
        model.addAttribute("recentRentals", rentalService.findActiveRentals());
        return "admin/dashboard";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("rentals", rentalService.findAllRentals());
        return "admin/history";
    }
}
