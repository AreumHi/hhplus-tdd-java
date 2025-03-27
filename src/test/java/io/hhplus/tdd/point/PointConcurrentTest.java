package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class PointConcurrentTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserPointTable userPointTable;

    long userId;

    @BeforeEach
    void setup() {
        userId = System.currentTimeMillis();
        userPointTable.insertOrUpdate(userId, 0L);
    }

    @Test
    @DisplayName("포인트 충전 요청 2건 동시 처리")
    void  chargePoint_concurrent_two_requests() throws Exception {
        long chargeAmount = 1_000L;

        Thread t1 = new Thread(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/charge", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(chargeAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/charge", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(chargeAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 두 스레드 시작
        t1.start();
        t2.start();

        // 두 스레드가 끝날 때까지 기다림
        t1.join();
        t2.join();

        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value((int)(chargeAmount * 2)));
    }

    @Test
    @DisplayName("포인트 사용 요청 2건 동시 처리")
    void usePoint_concurrent_two_requests() throws Exception {
        long initialAmount = 2_000L;
        long useAmount = 1_000L;

        // 먼저 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(initialAmount)))
                .andExpect(status().isOk());

        Thread t1 = new Thread(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(useAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(useAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // 최종 포인트가 0원이 되어야 함
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("포인트 충전/사용 요청 2건 동시 처리")
    void charge_and_usePoint_two_concurrent_requests() throws Exception {
        long chargeAmount = 500L;
        long useAmount = 500L;

        // 충전
        Thread t1 = new Thread(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/charge", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(chargeAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 사용
        Thread t2 = new Thread(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(useAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // 최종 포인트: 500 - 500 = 0
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(0));
    }

}
