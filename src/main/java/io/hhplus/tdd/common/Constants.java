package io.hhplus.tdd.common;

public class Constants {

    public static final long MAX_POINT = 1_000_000L;

    // 인스턴스화 방지 - new Constants() 못하게 막음 (유틸 클래스 패턴)
    private Constants() {}
}
