package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.hhplus.tdd.common.Constants.MAX_POINT;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

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
    @DisplayName("포인트 최대 한도에 딱 맞는 값 - 경계값 테스트")
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
    @DisplayName("0원 충전 시 예외 발생")
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
    @DisplayName("음수 충전 시 예외 발생")
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
    @DisplayName("포인트 최대 한도 초과 시 예외 발생")
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

    // TODO: 포인트 "사용" 테스트 케이스 구현
    // 잔고가 부족할 경우, 포인트 사용은 실패 하여야 한다.

    // TODO: 포인트 "내역 조회" 테스트 케이스 구현
}