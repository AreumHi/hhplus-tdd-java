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

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    // TODO: 요구사항-동일한 사용자에 대한 동시 요청이 정상적으로 처리될 수 있도록 개선

    // TODO: 선택한 언어에 대한 동시성 제어 방식 및 각 적용의 장/단점을 기술한 보고서 작성 ( README.md )
    // 동시성 문제에 대한 학습 및 각 동시성 문제 해결 방식 간 비교/분석

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
    public synchronized UserPoint chargePoint(long userId, long chargeAmount) {

        if (chargeAmount <= 0) {
            throw new IllegalArgumentException("충전 금액은 1원 이상이어야 합니다.");
        }

        UserPoint currentPoint = userPointTable.selectById(userId);

        // 충전 후 업데이트 되어야 할 포인트
        long updatePoint = currentPoint.point() + chargeAmount;

        if (MAX_POINT < updatePoint) {
            throw new IllegalArgumentException(String.format("최대 포인트 한도(%d)를 초과할 수 없습니다.", MAX_POINT));
        }

        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(userId, updatePoint);
    }

    /**
     * 특정 유저의 포인트를 사용한다.
     * - 포인트 잔액이 부족하면 안된다.
     * - 사용 금액은 1원 이상이어야 한다.
     */
    public synchronized UserPoint usePoint(long userId, long useRequestAmount) {

        if (useRequestAmount <= 0) {
            throw new IllegalArgumentException("사용 금액은 1원 이상이어야 합니다.");
        }

        UserPoint currentPoint = userPointTable.selectById(userId);

        // 사용 후 업데이트 되어야 할 포인트
        long updatePoint = currentPoint.point() - useRequestAmount;

        if (updatePoint < 0) {
            throw new IllegalArgumentException(String.format("포인트가 부족합니다. 현재 포인트: %d, 사용 요청 금액: %d", currentPoint.point(), useRequestAmount));
        }

        pointHistoryTable.insert(userId, useRequestAmount, TransactionType.USE, System.currentTimeMillis());

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
