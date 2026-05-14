package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.dto.UmbrellaForm;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/umbrellas")
public class AdminUmbrellaController {

    private final UmbrellaService umbrellaService;

    public AdminUmbrellaController(UmbrellaService umbrellaService) {
        this.umbrellaService = umbrellaService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("umbrellas", umbrellaService.findAll());
        model.addAttribute("umbrellaForm", new UmbrellaForm());
        return "admin/umbrellas";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute UmbrellaForm umbrellaForm,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("umbrellas", umbrellaService.findAll());
            return "admin/umbrellas";
        }
        try {
            umbrellaService.register(umbrellaForm.getNumber());
            redirectAttributes.addFlashAttribute("successMessage", umbrellaForm.getNumber() + "번 우산이 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/umbrellas";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            umbrellaService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "우산이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/umbrellas";
    }
}
