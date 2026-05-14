package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.config.SecurityConfig;
import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminDashboardController.class, AdminRentalController.class})
@Import(SecurityConfig.class)
class AdminRentalControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RentalService rentalService;
    @MockitoBean UmbrellaService umbrellaService;

    @Test
    void 대시보드_접근() throws Exception {
        given(umbrellaService.countTotal()).willReturn(5L);
        given(umbrellaService.countAvailable()).willReturn(3L);
        given(rentalService.countActive()).willReturn(2L);
        given(rentalService.findActiveRentals()).willReturn(List.of());

        mockMvc.perform(get("/admin").with(user("chuseok22").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard"));
    }

    @Test
    void 대여_현황_접근() throws Exception {
        given(rentalService.findActiveRentals()).willReturn(List.of());

        mockMvc.perform(get("/admin/rentals").with(user("chuseok22").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/rentals"));
    }

    @Test
    void 이력_접근() throws Exception {
        given(rentalService.findAllRentals()).willReturn(List.of());

        mockMvc.perform(get("/admin/history").with(user("chuseok22").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/history"));
    }

    @Test
    void 반납_처리_성공_시_리다이렉트() throws Exception {
        willDoNothing().given(rentalService).adminReturn(1L);

        mockMvc.perform(post("/admin/rentals/1/return")
                .with(user("chuseok22").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/rentals"));
    }
}
