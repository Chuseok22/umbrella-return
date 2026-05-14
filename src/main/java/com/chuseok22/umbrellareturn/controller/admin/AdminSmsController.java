package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.service.SmsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/sms")
public class AdminSmsController {

    private final SmsService smsService;

    public AdminSmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("smsLogs", smsService.findAllLogs());
        return "admin/sms";
    }

    @PostMapping("/send")
    public String sendManual(RedirectAttributes redirectAttributes) {
        try {
            int count = smsService.sendReminderToAll();
            redirectAttributes.addFlashAttribute("successMessage",
                count + "명에게 SMS가 발송되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "SMS 발송 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/sms";
    }
}
