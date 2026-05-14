package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.config.SecurityConfig;
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

@WebMvcTest(AdminUmbrellaController.class)
@Import(SecurityConfig.class)
class AdminUmbrellaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UmbrellaService umbrellaService;

    @Test
    void 인증없이_접근_시_로그인_리다이렉트() throws Exception {
        mockMvc.perform(get("/admin/umbrellas"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void 인증_후_우산_목록_접근() throws Exception {
        given(umbrellaService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/umbrellas").with(user("chuseok22").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/umbrellas"))
            .andExpect(model().attributeExists("umbrellas", "umbrellaForm"));
    }

    @Test
    void 우산_등록_성공_시_리다이렉트() throws Exception {
        willDoNothing().given(umbrellaService).register("001");

        mockMvc.perform(post("/admin/umbrellas")
                .param("number", "001")
                .with(user("chuseok22").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/umbrellas"));
    }

    @Test
    void 우산_등록_폼_검증_실패_시_뷰_재렌더링() throws Exception {
        given(umbrellaService.findAll()).willReturn(List.of());

        mockMvc.perform(post("/admin/umbrellas")
                .param("number", "")
                .with(user("chuseok22").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/umbrellas"));
    }

    @Test
    void 우산_삭제_성공_시_리다이렉트() throws Exception {
        willDoNothing().given(umbrellaService).delete(1L);

        mockMvc.perform(post("/admin/umbrellas/1/delete")
                .with(user("chuseok22").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/umbrellas"));
    }
}
