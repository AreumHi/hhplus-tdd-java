package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Test
    @DisplayName("특정 유저의 포인트를 조회한다")
    void getUserPoint_success() throws Exception {
        // given
        long userId = 1L;
        long point = 1000L;
        long updateMillis = System.currentTimeMillis();
        UserPoint mockUserPoint = new UserPoint(userId, point, updateMillis);

        // pointService.getPoint(userId) 호출 시 mockUserPoint 리턴하도록 설정
        when(pointService.getPoint(userId)).thenReturn(mockUserPoint);

        // when & then
        mockMvc.perform(get("/point/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 200(OK) 인지 확인
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(point))
                .andDo(print());
    }

    @Test
    @DisplayName("유저의 포인트 이용(충전/사용) 내역을 조회한다")
    void getUserHistories_success() throws Exception {
        // given
        long userId = 1L;

        List<PointHistory> mockPointHistoryList = List.of(
                new PointHistory(1L, userId, 10_000L, TransactionType.CHARGE, 100_000L),
                new PointHistory(2L, userId, 5_000L, TransactionType.USE, 101_000L),
                new PointHistory(3L, userId, 3_000L, TransactionType.USE, 102_000L)
        );

        when(pointService.getHistories(userId)).thenReturn(mockPointHistoryList);

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(10_000L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].amount").value(5_000L))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andDo(print());
    }

    @Test
    @DisplayName("유저의 포인트를 충전한다")
    void chargePoint_success() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        long updateMillis = System.currentTimeMillis();
        UserPoint charged = new UserPoint(userId, amount, updateMillis);

        when(pointService.chargePoint(userId, amount)).thenReturn(charged);

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(amount))
                .andDo(print());
    }

    // 포인트 충전 실패
    @Test
    @DisplayName("최대 포인트 한도 초과로 포인트 충전 실패한다.")
    void chargePoint_maxLimit_fail() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        long updateMillis = System.currentTimeMillis();
        UserPoint charged = new UserPoint(userId, amount, updateMillis);

        when(pointService.chargePoint(userId, amount))
                .thenThrow(new IllegalArgumentException("최대 포인트 한도를 초과할 수 없습니다."));

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("최대 포인트 한도를 초과할 수 없습니다."))
                .andDo(print());
    }

    @DisplayName("유저의 포인트를 사용한다")
    @Test
    void usePoint_success() throws Exception {
        // given
        long userId = 1L;
        long amount = 500L;
        long remaining = 500L;
        long updateMillis = System.currentTimeMillis();
        UserPoint used = new UserPoint(userId, remaining, updateMillis);

        when(pointService.usePoint(userId, amount)).thenReturn(used);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(remaining))
                .andDo(print());
    }

    // 포인트 사용 실패
    @DisplayName("유저의 포인트 잔액 부족으로 사용 실패")
    @Test
    void usePoint_insufficientBalance_fail() throws Exception {
        // given
        long userId = 1L;
        long amount = 10_000L;

        when(pointService.usePoint(userId, amount))
                .thenThrow(new IllegalArgumentException("포인트가 부족합니다."));

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("포인트가 부족합니다."))
                .andDo(print());
    }
}