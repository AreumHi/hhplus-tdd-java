package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

@Service
public class PointService {
    private final UserPointTable userPointTable;    // 생성자 주입으로 받아서 사용

    public PointService(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    /**
     * 특정 유저의 포인트를 조회합니다.
     * - 존재하지 않으면 0포인트 반환
     */
    public UserPoint getPoint(long userId) {
        return userPointTable.selectById(userId);
    }
}
