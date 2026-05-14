package com.chuseok22.umbrellareturn.controller;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.exception.CustomException;
import com.chuseok22.umbrellareturn.exception.ErrorCode;
import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RentalController.class)
@AutoConfigureMockMvc(addFilters = false)
class RentalControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RentalService rentalService;
    @MockitoBean UmbrellaService umbrellaService;

    @Test
    void 메인_페이지_접근() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"));
    }

    @Test
    void 대여_폼_접근() throws Exception {
        given(umbrellaService.findAvailable()).willReturn(List.of());

        mockMvc.perform(get("/rent"))
            .andExpect(status().isOk())
            .andExpect(view().name("rent"))
            .andExpect(model().attributeExists("rentForm", "availableUmbrellas"));
    }

    @Test
    void 반납_폼_접근() throws Exception {
        given(umbrellaService.findRented()).willReturn(List.of());

        mockMvc.perform(get("/return"))
            .andExpect(status().isOk())
            .andExpect(view().name("return"))
            .andExpect(model().attributeExists("returnForm", "rentedUmbrellas"));
    }

    @Test
    void 대여_성공_시_complete로_리다이렉트() throws Exception {
        willDoNothing().given(rentalService).rent(any(RentForm.class));

        mockMvc.perform(post("/rent")
                .param("borrowerName", "홍길동")
                .param("borrowerPhone", "01012345678")
                .param("borrowerStudentId", "20230001")
                .param("umbrellaNumber", "001"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/complete"));
    }

    @Test
    void 대여_폼_검증_실패_시_뷰_재렌더링() throws Exception {
        given(umbrellaService.findAvailable()).willReturn(List.of());

        mockMvc.perform(post("/rent")
                .param("borrowerName", "")
                .param("borrowerPhone", "")
                .param("borrowerStudentId", "")
                .param("umbrellaNumber", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("rent"))
            .andExpect(model().attributeExists("availableUmbrellas"));
    }

    @Test
    void 대여_서비스_예외_시_에러메시지_표시() throws Exception {
        given(umbrellaService.findAvailable()).willReturn(List.of());
        willThrow(new CustomException(ErrorCode.UMBRELLA_NOT_AVAILABLE))
            .given(rentalService).rent(any(RentForm.class));

        mockMvc.perform(post("/rent")
                .param("borrowerName", "홍길동")
                .param("borrowerPhone", "01012345678")
                .param("borrowerStudentId", "20230001")
                .param("umbrellaNumber", "001"))
            .andExpect(status().isOk())
            .andExpect(view().name("rent"))
            .andExpect(model().attribute("errorMessage", ErrorCode.UMBRELLA_NOT_AVAILABLE.getMessage()));
    }

    @Test
    void 반납_성공_시_complete로_리다이렉트() throws Exception {
        willDoNothing().given(rentalService).returnUmbrella(any());

        mockMvc.perform(post("/return")
                .param("borrowerName", "홍길동")
                .param("borrowerPhone", "01012345678")
                .param("umbrellaNumber", "001"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/complete"));
    }

    @Test
    void 완료_페이지_직접_접근_시_메인으로_리다이렉트() throws Exception {
        mockMvc.perform(get("/complete"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }
}
