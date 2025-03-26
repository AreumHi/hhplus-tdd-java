package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.hhplus.tdd.common.Constants.MAX_POINT;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    // TODO: 시간되면 @Nested 로 테스트 케이스 그룹화 해보기
    // TODO: BDDMockito 방식으로 바꿔보기: given().willReturn() ( <-> 현재는 기본 Mockito: when().thenReturn())

    // 조회 관련
    @Test
    @DisplayName("특정 유저의 포인트를 조회한다")
    void getPoint_existingUser() {
        // given
        long userId = 1L;
        long currentPoint = 1_000L;

        UserPoint mockPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);  // selectById(userId) 호출 -> mockPoint를 반환하도록 설정

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(currentPoint, result.point());
    }

    @Test
    @DisplayName("존재하지 않는 유저는 0포인트로 조회된다")
    void getPoint_nonExistingUser_returnsZeroPoint() {
        // given
        long userId = 999L;
        long emptyUserPoint = 0L;

        UserPoint emptyPoint = new UserPoint(userId, emptyUserPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(emptyPoint);

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(emptyUserPoint, result.point());
    }

    // 충전 성공
    @Test
    @DisplayName("유저에게 포인트를 충전한다 - 충전 성공")
    void chargePoint_existingUser() {
        // given
        long userId = 1L;
        long currentPoint = 1_000L;
        long chargeAmount = 9_000L;
        long expectedTotalPoint = currentPoint + chargeAmount;

        UserPoint mockPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        UserPoint chargedTotalPoint = new UserPoint(userId, expectedTotalPoint, System.currentTimeMillis());
        when(userPointTable.insertOrUpdate(userId, expectedTotalPoint)).thenReturn(chargedTotalPoint);

        // when - 9_000포인트 충전
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then - 총 10_000포인트
        assertNotNull(result);
        assertEquals(expectedTotalPoint, result.point());
        assertEquals(userId, result.id());
    }

    @Test
    @DisplayName("포인트 최대 한도에 딱 맞는 값을 충전한다 - 경계값 테스트")
    void chargePoint_MAX_POINT_souldSucceed() {
        // given
        long userId = 1L;
        long currentPoint = 950_000L;
        long chargeAmount = 50_000L;
        long expectedTotalPoint = currentPoint + chargeAmount;

        UserPoint mockPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        UserPoint chargedTotalPoint = new UserPoint(userId, expectedTotalPoint, System.currentTimeMillis());
        when(userPointTable.insertOrUpdate(userId, expectedTotalPoint)).thenReturn(chargedTotalPoint);

        // when
        UserPoint result = pointService.chargePoint(userId, chargeAmount);

        // then
        assertNotNull(result);
        assertEquals(expectedTotalPoint, result.point());
        assertEquals(userId, result.id());
    }

    // 충전 예외
    @Test
    @DisplayName("충전금액이 0원일 경우 예외 발생")
    void chargePoint_zeroAmount_throwException() {
        // given
        long userId = 1L;
        long chargeAmount = 0L;

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, chargeAmount);
        });
        assertEquals("충전 금액은 1원 이상이어야 합니다.", exception.getMessage());

    }

    @Test
    @DisplayName("음수 충전 할 경우 예외 발생")
    void chargePoint_negativeAmount_throwException() {
        // given
        long userId = 1L;
        long chargeAmount = -1_000L;

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
           pointService.chargePoint(userId, chargeAmount);
        });
        assertEquals("충전 금액은 1원 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("최대 포인트 한도 초과 할 경우 예외 발생")
    void chargePoint_maxLimit_throwException() {
        // given
        long userId = 1L;
        long currentPoint = 950_000L;
        long chargeAmount = 100_000L;

        UserPoint mockPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargePoint(userId, chargeAmount);
        });
        assertEquals(String.format("최대 포인트 한도(%d)를 초과할 수 없습니다.", MAX_POINT), exception.getMessage());
    }

    // 사용 성공
    @Test
    @DisplayName("충분한 포인트가 있는 유저의 포인트를 사용한다")
    void usePoint_enoughBalance() {
        // given
        long userId = 1L;
        long currentPoint = 10_000L;
        long useRequestAmount = 1_000L;
        long expectedNewPoint = currentPoint - useRequestAmount;

        UserPoint mockPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        UserPoint newPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());
        when(userPointTable.insertOrUpdate(userId, expectedNewPoint)).thenReturn(newPoint);

        // when
        UserPoint result = pointService.usePoint(userId, useRequestAmount);

        // then
        assertNotNull(result);
        assertEquals(expectedNewPoint, result.point());
        assertEquals(userId, result.id());
    }

    // 사용 예외
    @Test
    @DisplayName("포인트 잔액이 부족 할 경우 예외 발생")
    void usePoint_insufficientBalance_throwException() {
        // given
        long userId = 1L;
        long currentPoint = 5_000L;
        long useRequestAmount = 10_000L;

        UserPoint mockPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
           pointService.usePoint(userId, useRequestAmount);
        });
        assertEquals(String.format("포인트가 부족합니다. 현재 포인트: %d, 사용 요청 금액: %d", currentPoint, useRequestAmount), exception.getMessage());
    }

    @Test
    @DisplayName("사용금액이 0원일 경우 예외 발생")
    void usePoint_zeroAmount_throwException() {
        // given
        long userId = 1L;
        long useRequestAmount = 0L;

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, useRequestAmount);
        });
        assertEquals("사용 금액은 1원 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("음수 사용 할 경우 예외 발생")
    void usePoint_negativeAmount_throwException() {
        // given
        long userId = 1L;
        long useRequestAmount = -5_000L;

        // when & then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.usePoint(userId, useRequestAmount);
        });
        assertEquals("사용 금액은 1원 이상이어야 합니다.", exception.getMessage());
    }

    // 이용 내역 조회
    @Test
    @DisplayName("특정 유저의 포인트 이용(충전/사용) 내역을 조회한다")
    void getHistory_existingUser() {
        // given
        long userId = 1L;
        long now = System.currentTimeMillis();

        List<PointHistory> mockPointHistoryList = List.of(
                new PointHistory(1L, userId, 10_000L, TransactionType.CHARGE, now - 10_000),
                new PointHistory(2L, userId, 5_000L, TransactionType.USE, now - 5_000),
                new PointHistory(3L, userId, 3_000L, TransactionType.USE, now - 3_000)
        );

        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(mockPointHistoryList);

        // when
        List<PointHistory> result = pointService.getHistories(userId);

        // then
        assertNotNull(result);
        assertEquals(mockPointHistoryList.size(), result.size());
        assertIterableEquals(mockPointHistoryList, result);
    }

    @Test
    @DisplayName("포인트 이용 내역이 없는 유저는 빈 리스트를 반환한다")
    void getHistory_noHistory_returnEmptyList() {
        // given
        long userId = 999L;

        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(Collections.emptyList());

        // when
        List<PointHistory> result = pointService.getHistories(userId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("selectAllByUserId 가 null 을 반환하면 빈 리스트로 처리한다")
    void getHistory_nullUser_returnEmptyList() {
        // given
        long userId = 1L;
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(null);

        // when
        List<PointHistory> result = pointService.getHistories(userId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}