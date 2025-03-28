## 🔒 Java에서의 동시성 제어 방식 및 적용 비교

이 프로젝트에서는 단일 서버 환경에서 **사용자 포인트 충전/사용 시의 동시성 문제**를 해결하기 위해  
**Java의 동시성 제어 방식**을 학습하고 적용했습니다.

---

### ❗ 동시성 문제는 어떻게 발생할까?
#### 공유 자원에 대해 분리된 연산을 수행하는 과정에서 발생

#### ✅ 동시성 이슈 예시

- 동일 유저에게 동시에 포인트 충전/사용 요청이 들어올 경우, **레이스 컨디션(Race Condition)** 발생 가능  
  → 잘못된 포인트 합산, 음수 포인트 등 **데이터 정합성 깨짐**

---


### ⚙️ Java에서 사용할 수 있는 동시성 제어 방식

#### 본 과제에서는 분산환경을 고려하지 않으며, Map을 활용한 인메모리 DB 방식을 사용합니다. 따라서 애플리케이션 레벨에서 동시성 제어를 하는 방법을 학습합니다.

#### 1. synchronized
- 메서드나 블록에 락을 걸어 한 번에 하나의 스레드만 접근하도록 보장
- 자바의 모든 객체는 모니터 락을 가지고 있고, synchronized 키워드를 통해 암묵적으로 사용
- 특정 객체의 모니터락을 획득한 스레드만 synchronized 메서드나 블록을 실행 가능
- 다른 스레드는 모니터 락이 해제될 때까지 대기
- 스레드의 순서를 보장할 수 없음

* **장점**: 간단하게 동시성 제어 가능, 빠른 구현 가능
* **단점**:
1)메서드 전체가 락에 걸리므로 성능 저하 가능성 있음, 느려질 수 있음  2)세부 제어 불가능 (타임아웃, 락 공정성 등)

#### 2. ReentrantLock
- lock(), unlock()을 명시적으로 호출하며 락을 획득/해제하며 동시성 제어를 하기 때문에 휴먼에러 발생 가능
- 기본적으로 스레드가 동시 요청했을 때 순서를 보장되지 않지만, 공정락(fairness) 모드로 설정하면 대기 중인 스레드에게 순차적으로 락이 할당 됨
- 공정 모드는 락 내부에서 큐를 사용하여 대기 중인 스레드의 순서를 보장

* **장점**: 1) 타임아웃, 공정성, 인터럽트 처리 가능 2) 필요한 범위만 락 가능 (메서드 전체 X)
* **단점**: lock()/unlock() 누락 위험, 코드 복잡도 ↑

#### 3. AtomicInteger, AtomicLong 등 CAS 기반 원자 연산
- 연산에서 기대하는 값과 저장된 값을 비교해서 두개가 일치할 때만 값을 수정
- 락을 사용하지 않아 락 경합이 일어나지 않음

* **장점**: 성능 뛰어남, 경량 락 대체 가능
* **단점**: 복잡한 연산은 처리 불가

---

### 🧪 본 과제에서 적용한 방식

#### ✅ `synchronized`
#### 동시성을 허용하면서 동시성을 제어했습니다. 예를들어 A의 충전과 사용이 동시 요청 되었을 때 각 요청이 순차적으로 실행됩니다.

```java
// 포인트 사용
public synchronized UserPoint chargePoint(long userId, long chargeAmount) {
        if (chargeAmount <= 0) throw new IllegalArgumentException("충전 금액은 1원 이상이어야 합니다.");
        
        UserPoint currentPoint = userPointTable.selectById(userId);
        long updatePoint = currentPoint.point() + chargeAmount;

        if (MAX_POINT < updatePoint) throw new IllegalArgumentException(String.format("최대 포인트 한도(%d)를 초과할 수 없습니다.", MAX_POINT));
        
        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(userId, updatePoint);
    }

// 포인트 충전
public synchronized UserPoint usePoint(long userId, long useRequestAmount) {
        if (useRequestAmount <= 0) throw new IllegalArgumentException("사용 금액은 1원 이상이어야 합니다.");
        
        UserPoint currentPoint = userPointTable.selectById(userId);
        long updatePoint = currentPoint.point() - useRequestAmount;

        if (updatePoint < 0) throw new IllegalArgumentException(String.format("포인트가 부족합니다. 현재 포인트: %d, 사용 요청 금액: %d", currentPoint.point(), useRequestAmount));
        
        pointHistoryTable.insert(userId, useRequestAmount, TransactionType.USE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(userId, updatePoint);
    }
```

- **PointService** 클래스의 `chargePoint()` 및 `usePoint()` 메서드에 `synchronized` 키워드를 적용
- **한 유저의 포인트 접근이 한 번에 하나의 스레드만 가능하도록 제어**



**🚨 문제점**
* 메서드 전체가 락에 걸리므로 다른 유저의 요청에도 동시성에 제어되는 등 불필요한 대기 발생 (성능저하)
* 작업의 순서 보장X

---

### 🛠️ 향후 개선 고려: `ReentrantLock`

#### 본 과제에서는 시간상의 이유로 `ReentrantLock` 방식으로 구현은 진행하지 못했습니다. 추후 개선 시 해당 방식으로 리팩토링 할 예정입니다.

- 사용자별로 별도의 락을 걸 수 있도록 `ConcurrentHashMap<Long, ReentrantLock>` 사용
- 충전/사용 요청에 대해 더 세밀한 락 제어 가능


---

### 💡 동시성 테스트 전략

- 본 과제에서는  `ExecutorService + CountDownLatch` 조합을 사용하여 **2개의 요청을 동시에 발생시키고 처리 결과를 확인하는 방식**으로 테스트를 구현했습니다.
-  테스트 케이스: 1) 동일 유저의 2개의 동시 충전 요청 2) 동일 유저의 2개의 동시 사용 요청 3) 동일 유저의 충전 + 사용 동시 요청

### 📌 비교) `CompletableFuture` vs `ExecutorService + CountDownLatch`

#### Java에서 비동기 작업을 처리하거나 동시성 테스트/제어를 구현할 때 많이 사용되는 두 가지 방식인 `CompletableFuture`와 `ExecutorService + CountDownLatch`를 비교합니다.

| 항목 | `CompletableFuture` | `ExecutorService + CountDownLatch` |
|------|----------------------|-------------------------------------|
| 사용 목적 | 비동기 작업 처리, 콜백 연결 | 병렬 작업 실행 후 동기화 (대기) |
| 코드 스타일 | 함수형, 선언형 스타일 (체이닝) | 명령형 스타일, 직접 스레드 제어 |
| 예외 처리 | `handle()`, `exceptionally()` 등 체이닝으로 처리 가능 | `try-catch` 사용 |
| 스레드 풀 | 내부적으로 `ForkJoinPool` 사용 (`supplyAsync()` 등) | 명시적으로 `ExecutorService` 생성 필요 |
| 동시 작업 대기 | `allOf()`, `join()` 등을 활용한 자연스러운 대기 | `CountDownLatch.await()` 명시적 대기 |
| 가독성 | 간결하고 체이닝으로 깔끔 | 익숙하지만 구조가 길어질 수 있음 |
| 테스트 용이성 | 테스트 코드에선 흐름 파악 어려울 수 있음 | 테스트 흐름을 명시적으로 제어 가능 |
| 적합한 상황 | 비동기 콜백, 체이닝된 작업 처리 | 테스트 코드, 단순 병렬 작업 실행 후 확인 |

---

### ✨ 결론

- **단일 서버 기반**인 이번 과제에서는 `synchronized` 만으로도 충분히 안정적인 동시성 제어가 가능
- **그러나 `ReentrantLock`** 등 더 정밀한 제어 방식으로 개선 필요
