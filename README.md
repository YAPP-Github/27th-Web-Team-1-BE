# Lokit

> 우리만의 이야기를, 지도에 Lokit
>
> 함께 기록하고, 함께 쌓아가는 커플 아카이빙 서비스

Lokit은 촬영 위치를 기반으로 지도 위에서 추억을 한눈에 돌아볼 수 있는 서비스입니다.

<br>

## Highlights

### Tech

1. **지도 대용량 데이터 성능 최적화**
    - PostGIS 공간 쿼리 + POI 클러스터링 + multi level 캐시 + 방향 기반 prefetch로 지도 이동 시 지연 최소화
2. **PostgreSQL 인덱스 튜닝**
    - GiST(공간), BRIN(시계열), 복합 인덱스, DESC/NULLS LAST 정렬 인덱스 운영
3. **캐시 일관성과 무효화 복잡도**
    - dataVersion 기반 증분 캐시를 설계해 불필요한 재조회/재전송을 줄이고 응답 지연을 개선
4. **안정성을 고려한 비동기 병렬 처리로 성능 최적화**
    - Virtual Threads + Structured Concurrency로 로직/쿼리 병렬화, DB 세마포어로 과부하 제어.
5. **Soft delete 환경 최적화**
    - WHERE is_deleted = false 기반 partial index로 활성 데이터 조회 성능 최적화
6. **무결성 제약 설계**
    - 부분 유니크 인덱스로 비즈니스 룰 강제
7. **외부 스토리지 정합성 유지**
    - S3 비동기 삭제 실패를 허용하고(이벤트 핸들러), 스케줄러로 orphan object를 정리해 최종 정합성 확보.
8. **데이터 수명주기/파기 자동화**
    - 유예기간 만료 처리, soft delete, 탈퇴 계정 비가역 익명화까지 스케줄러 기반 자동 운영.
9. **모바일 wifi ↔ cellular 네트워크 전환 지원**
    - caddy 기반 http 2/3 활용

### Code Architecture

1. **이벤트 기반 후속 처리 분리**
    - 캐시 무효화, 스토리지 정리 같은 후속 작업을 도메인 이벤트로 분리해 결합도를 낮추고 운영 안정성을 높임.
2. **Kotlin 친화적이고 안전한 코드베이스**
    - data class/copy, null-safety, 확장 함수, 명확한 타입 모델로 변경 비용을 줄이고 읽기 쉬운 코드 지향.
3. **도메인 중심 설계**
    - 복잡한 비즈니스 규칙을 도메인 모델에 명확히 캡슐화.
4. **헥사고날 아키텍처**
    - Controller → UseCase(In Port) → Service → Out Port → Adapter 구조를 유지.
    - 외부 시스템(S3/Kakao/DB) 변경이 도메인 로직에 전파되지 않도록 설계.

<br>

## Architecture

### System Architecture

```
┌─────────────┐     HTTPS      ┌──────────────────┐        ┌──────────────────┐
│   Client    │ ──────────────→│      Caddy       │───────→│  Spring Boot 4.0 │
│  (Mobile)   │  HTTP/2, H3    │  Reverse Proxy   │  :8080 │  Java 24 Virtual │
└─────────────┘  zstd / gzip   │  Auto TLS (ACME) │        │    Threads       │
                               └──────────────────┘        └────────┬─────────┘
                                                                    │
                              ┌─────────────────────────────────────┼──────────────────┐
                              │                                     │                  │
                              ▼                                     ▼                  ▼
                   ┌─────────────────┐                  ┌──────────────────┐  ┌────────────────┐
                   │   PostgreSQL    │                  │     AWS S3       │  │  Kakao API     │
                   │  + PostGIS 3.4  │                  │  Presigned URL   │  │  Geocoding     │
                   │   (AWS RDS)     │                  │  이미지 스토리지     │  │  장소 검색       │
                   └─────────────────┘                  └──────────────────┘  └────────────────┘
```

- **Caddy**: TLS 자동 발급(ACME), HTTP/2 & HTTP/3, zstd/gzip 압축, 리버스 프록시
- **Spring Boot 4.0**: Virtual Threads 기반 요청 처리, Structured Concurrency로 병렬 I/O
- **PostgreSQL + PostGIS**: 공간 인덱스(GiST)를 활용한 Bounding Box 쿼리, `ST_SnapToGrid` 클러스터링
- **AWS S3**: Presigned URL로 클라이언트 직접 업로드, 서버 대역폭 사용 없음
- **CI/CD**: GitHub Actions → Docker Build → AWS ECR Push → EC2 배포, SSM Parameter Store로 환경변수 관리

### Code Architecture

각 도메인은 다음 계층으로 구성됩니다:

```
domain/{bounded-context}/
├── presentation/                    # Primary Adapter (In)
│   ├── *Api.kt                      #   Swagger 인터페이스
│   └── *Controller.kt               #   REST Controller
├── application/
│   ├── port/in/                     # Input Port
│   │   └── *UseCase.kt              #   유스케이스 인터페이스
│   ├── port/                        # Output Port
│   │   └── *RepositoryPort.kt       #   영속화 인터페이스
│   └── *Service.kt                  #   유스케이스 구현
├── domain/                          # Domain Model
│   └── *.kt                         #   순수 도메인 객체
├── infrastructure/                  # Secondary Adapter (Out)
│   ├── *Entity.kt                   #   JPA Entity
│   └── *Repository.kt              #   Output Port 구현체
├── dto/                             # Request/Response DTO
├── presentation/mapping/            # Controller DTO <-> Domain(ReadModel/Command) 변환
├── application/mapping/             # Application ReadModel 변환
└── infrastructure/mapping/          # Domain <-> Entity 변환
```

<br>

## Performance Optimization

### Virtual Threads & Structured Concurrency

Java 24 Virtual Threads 기반으로 모든 요청이 경량 스레드에서 처리됩니다. `StructuredTaskScope`를 활용한 Structured Concurrency로 하나의 API 요청 내에서
독립적인 작업(역지오코딩, 앨범 조회, 사진 조회 등)을 병렬 실행합니다.

```kotlin
// 지도 홈 조회 시 3개의 독립 쿼리를 동시 실행
val (locationFuture, albumsFuture, photosFuture) =
    StructuredConcurrency.run { scope ->
        Triple(
            scope.fork { mapClientPort.reverseGeocode(longitude, latitude) },
            scope.fork { albumRepository.findAllByCoupleId(coupleId) },
            scope.fork { getPhotos(zoom, bbox, userId, albumId) },
        )
    }
```

### Multi-level Caching

**Application Cache (Caffeine)**

11개의 용도별 캐시를 Caffeine 인메모리 캐시로 관리합니다.

| Cache            | TTL | Max Size | 용도                    |
|------------------|-----|----------|-----------------------|
| `mapCells`       | 10분 | 400      | 그리드 셀 단위 클러스터 캐시      |
| `mapPhotos`      | 10분 | 400      | Bounding Box 기반 사진 캐시 |
| `coupleAlbums`   | 3분  | 200      | 커플 앨범 목록 캐시           |
| `reverseGeocode` | 3분  | 100      | 역지오코딩 결과              |
| `searchPlaces`   | 3분  | 50       | 장소 검색 결과              |
| `presignedUrl`   | 3분  | 100      | S3 Presigned URL      |
| `userCouple`     | 10분 | 200      | 사용자-커플 매핑             |

**Grid Cell Caching**

지도 클러스터 조회 시 Bounding Box 내 모든 그리드 셀을 계산하고, 캐시된 셀은 건너뛰어 미캐싱 셀만 DB에서 조회합니다. 지도 이동 시 겹치는 영역의 재조회를 방지합니다.

**dataVersion (증분 캐시)**

`/map/me` API에서 사진 데이터 버전(`dataVersion`)을 관리합니다. `dataVersion`은 **커플/앨범 기준 전역 버전**이며, 위치와 무관하게 변경 이력을 식별합니다.

- `lastDataVersion`은 클라이언트 캐시 동기화 판단에 사용됩니다.
- 서버는 요청 위치의 `clusters`/`photos`를 항상 계산하며, 셀/영역 캐시를 사용해 미캐싱 영역만 DB에서 조회합니다.
- 현재 증분 버전 파라미터(`lastDataVersion`)는 `/map/me`에만 제공됩니다.

> 참고: 기본 앨범(`isDefault=true`) 요청은 `albumId`를 `null`로 정규화하여 처리합니다.
> `dataVersion` 계산과 실제 사진 조회 모두 동일한 정규화 기준을 사용합니다.

**Technical Decisions & Trade-offs**

| 주제                   | 선택                           | 이유                        | 트레이드오프                  |
|----------------------|------------------------------|---------------------------|-------------------------|
| `lastDataVersion` 범위 | `/map/me`에만 적용               | 지도 핵심 데이터 통합 응답이라 효과가 큼   | 다른 API는 HTTP 캐시에 의존     |
| 버전 계산 단위             | 커플/앨범 전역 버전                  | 위치 이동과 무관한 버전 일관성 확보      | 데이터 전송 생략은 클라이언트 정책에 위임 |
| 클러스터 prefetch        | 방향/속도 기반 선행 적재               | 체감 지연 감소, 불필요 prefetch 억제 | 로직 복잡도 증가               |
| 캐시 무효화               | 포인트 기반 + 필요 시 커플 단위          | 캐시 적중률 유지                 | 무효화 조건 관리 비용 증가         |
| DB 병렬도               | 세마포어로 상한 제어                  | 커넥션 풀 고갈 방지, 안정성 확보       | 피크 순간 처리량 일부 희생         |
| 공간 쿼리 범위             | grid margin 확장 조회            | 경계 셀 누락 완화                | 조회 범위 증가로 단건 비용 상승      |
| 캐시 키 전략              | bbox를 격자 단위로 정렬              | 미세 pan에도 키 안정화, 재사용률 향상   | 키 계산/좌표 변환 로직 필요        |
| 기본 앨범 모델             | 조회 시점 병합 집계                  | UX 단순화(전체 사진 보기)          | 조회/캐시 무효화 경계 복잡         |
| Presigned URL 멱등성    | `X-Idempotency-Key` + 3분 TTL | 중복 발급 억제, 운영 단순화          | 영구 멱등성은 아님              |

**Locking & Consistency Decisions**

| 주제 | 선택 | 이유 | 트레이드오프 |
|---|---|---|---|
| JVM 동시성 제어 | `LockManager.withLock` + `tryLock(timeout)` | 동일 프로세스 내 이메일 단위 경합 직렬화 | 멀티 인스턴스 환경에서는 DB/분산락 보완 필요 |
| DB 쓰기 경합 제어 | 핵심 조회에 `PESSIMISTIC_WRITE` + lock timeout 힌트 | 초대코드/커플 연결처럼 선점이 중요한 시나리오 보호 | 대기/타임아웃 튜닝 필요 |
| 엔티티 충돌 감지 | `BaseEntity.@Version` + `@OptimisticRetry`(핵심 커맨드 서비스) | 업데이트 손실 방지 + 일시 충돌 자동 재시도 | 충돌 잦으면 재시도 비용 증가 |
| 실패 처리 방식 | 예외를 가드 절에서 먼저 던지고 본 로직은 직선 흐름 유지 | 코드 가독성/검증 포인트 명확화 | 분기별 예외 타입 설계 비용 증가 |

### Transport Optimization

**HTTP/2 & HTTP/3**

Caddy 리버스 프록시를 통해 HTTP/2 멀티플렉싱과 HTTP/3(QUIC)을 지원합니다. 단일 연결에서 다중 API 요청을 병렬 처리하며, HTTP/3에서는 UDP 기반으로 HOL 블로킹을 해소하고
WiFi-셀룰러 간 연결 마이그레이션을 지원합니다.

**zstd & gzip 압축**

Caddy에서 `Accept-Encoding` 헤더에 따라 zstd(우선) 또는 gzip으로 응답을 자동 압축합니다. zstd는 gzip 대비 약 30% 더 작은 압축 결과를 제공합니다.

<br>

## API Documentation

개발 서버 실행 후 Swagger UI에서 API 명세를 확인할 수 있습니다.

| Endpoint       | Description  |
|----------------|--------------|
| `/api/swagger` | Swagger UI   |
| `/api/docs`    | OpenAPI JSON |

<br>

## Getting Started

### Prerequisites

- Java 24+
- Docker & Docker Compose

### Environment Variables

환경변수는 [`infra/.env.template`](infra/.env.template)을 참고하세요.

<br>

## Contributors

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/ohksj77">
        <img src="https://avatars.githubusercontent.com/u/89020004?v=4" width="160" alt="ohksj77" /><br />
        <sub><b>@ohksj77</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/JihwanByun">
        <img src="https://avatars.githubusercontent.com/u/156163390?v=4" width="160" alt="JihwanByun" /><br />
        <sub><b>@JihwanByun</b></sub>
      </a>
    </td>
  </tr>
</table>

<br>
