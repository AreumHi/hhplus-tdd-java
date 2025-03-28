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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    UserPointTable userPointTable;

    private long userId;

    @BeforeEach
    void setup() {
        userId = generateUniqueUserId();
        userPointTable.insertOrUpdate(userId, 0L);
    }

    private long generateUniqueUserId() {
        return System.currentTimeMillis();
    }

    @Test
    @DisplayName("포인트 조회 성공")
    void getPoint_success() throws Exception {

        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) userId))
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("포인트 충전 성공")
    void chargePoint_success() throws Exception {
        long chargeAmount = 1_000L;

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) userId))
                .andExpect(jsonPath("$.point").value((int) chargeAmount));
    }

    @Test
    @DisplayName("포인트 충전 후 사용 성공")
    void usePoint_success() throws Exception {
        long chargeAmount = 1_000L;
        long useAmount = 500L;

        // 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value((int) (chargeAmount - useAmount)));
    }

    @Test
    @DisplayName("포인트 충전/사용 후 이용 내역 조회 성공")
    void getPointHistories_success() throws Exception {
        long chargeAmount = 1_000L;
        long useAmount = 1_000L;

        // 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk());

        // 내역 조회
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value((int) userId))
                .andExpect(jsonPath("$[1].userId").value((int) userId))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].type").value("USE"));
    }
}