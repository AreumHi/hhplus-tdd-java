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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    void chargePoint_concurrent_two_requests() throws Exception {
        int threadCount = 2;
        long chargeAmount = 1_000L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/charge", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(String.valueOf(chargeAmount)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value((int)(chargeAmount * 2)));
    }

    @Test
    @DisplayName("포인트 사용 요청 2건 동시 처리")
    void usePoint_concurrent_two_requests() throws Exception {
        int threadCount = 2;
        long initialAmount = 2_000L;
        long useAmount = 1_000L;

        // 선충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(initialAmount)))
                .andExpect(status().isOk());

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/use", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(String.valueOf(useAmount)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("포인트 충전/사용 요청 2건 동시 처리")
    void charge_and_usePoint_two_concurrent_requests() throws Exception {
        int threadCount = 2;
        long initamount = 500L;
        long chargeAmount = 1_000L;
        long useAmount = 500L;

        // 선충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(initamount)))
                .andExpect(status().isOk());

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 충전
        executorService.submit(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/charge", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(chargeAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        // 사용
        executorService.submit(() -> {
            try {
                mockMvc.perform(patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(useAmount)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executorService.shutdown();

        // 선충전 500 + 충전 1_000 - 사용 500 = 기대값 1_000
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(1_000));
    }
}