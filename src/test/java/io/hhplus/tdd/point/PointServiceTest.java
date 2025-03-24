package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트가 존재하는 유저는 해당 포인트를 조회할 수 있다")
    void getPoint_existingUser_returnsUserPoint() {
        // given
        long userId = 1L;
        UserPoint mockPoint = new UserPoint(userId, 1_000L, System.currentTimeMillis());

        // userPointTable이 selectById(userId)를 호출하면 mockPoint를 반환하도록 설정
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(1_000L, result.point());
    }

    @Test
    @DisplayName("포인트 정보가 없는 유저는 0포인트로 조회된다")
    void getPoint_nonExistingUser_returnsZeroPoint() {
        // given
        long userId = 999L;
        UserPoint emptyPoint = new UserPoint(userId, 0L, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(emptyPoint);

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(0L, result.point());
    }
}
