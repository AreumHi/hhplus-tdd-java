package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.hhplus.tdd.common.Constants.MAX_POINT;

@Service
public class PointService {

    private final UserPointTable userPointTable;    // 생성자 주입으로 받아서 사용 ( = @RequiredArgsConstructor)

    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * 특정 유저의 포인트를 조회한다.
     * - 유저가 존재하지 않으면 0포인트를 반환한다.
     */
    public UserPoint getPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * 특정 유저의 포인트를 충전한다.
     * - 충전 금액은 1원 이상이어야 한다.
     * - 포인트 최대 한도를 초과해서는 안된다.
     */
    public UserPoint chargePoint(long userId, long chargeAmount) {
        // 금액 유효성 체크 (1원 이상인지)
        if (chargeAmount <= 0) {
            throw new IllegalArgumentException("충전 금액은 1원 이상이어야 합니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);

        // 충전 후 업데이트 되어야 할 포인트
        long updatePoint = currentPoint.point() + chargeAmount;

        // 최대 한도초과 여부 체크
        if (MAX_POINT < updatePoint) {
            throw new IllegalArgumentException(String.format("최대 포인트 한도(%d)를 초과할 수 없습니다.", MAX_POINT));
        }

        // 포인트 반영
        return userPointTable.insertOrUpdate(userId, updatePoint);
    }

    /**
     * 특정 유저의 포인트를 사용한다.
     * - 포인트 잔액이 부족하면 안된다.
     * - 사용 금액은 1원 이상이어야 한다.
     */
    public UserPoint usePoint(long userId, long useRequestAmount) {
        // 금액 유효성 체크 (1원 이상인지)
        if (useRequestAmount <= 0) {
            throw new IllegalArgumentException("사용 금액은 1원 이상이어야 합니다.");
        }

        // 현재 포인트 조회
        UserPoint currentPoint = userPointTable.selectById(userId);

        // 사용 후 업데이트 되어야 할 포인트
        long updatePoint = currentPoint.point() - useRequestAmount;

        // 포인트 잔액 부족한지 여부 체크
        if (updatePoint < 0) {
            throw new IllegalArgumentException(String.format("포인트가 부족합니다. 현재 포인트: %d, 사용 요청 금액: %d", currentPoint.point(), useRequestAmount));
        }

        // 포인트 반영
        return userPointTable.insertOrUpdate(userId, updatePoint);
    }

    /**
     * 특정 유저의 포인트 이용(충전/사용) 내역을 조회한다.
     * - 포인트 이용 내역이 없는 유저는 빈 리스트를 반환한다.
     * - selectAllByUserId 가 null 을 반환하면 빈 리스트로 처리한다
     */
    public List<PointHistory> getHistories(long userId) {

        List<PointHistory> userHistoryList = pointHistoryTable.selectAllByUserId(userId);

        return Optional.ofNullable(pointHistoryTable.selectAllByUserId(userId))
                .orElse(Collections.emptyList());
    }
}
