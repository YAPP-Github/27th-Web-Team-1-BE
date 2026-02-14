# Lokit

> 우리만의 이야기를, 지도에 Lokit
> 함께 기록하고, 함께 쌓아가는 커플 아카이빙 서비스

Lokit은 사진을 앨범 단위로 관리하고, 촬영 위치를 기반으로 지도 위에서 추억을 한눈에 돌아볼 수 있는 모바일 중심 서비스입니다.

<br>

## Highlights

- **이미지 처리**: AWS S3 Presigned URL 기반 대용량 이미지 업로드 및 관리
- **공간 데이터 처리**: PostGIS + Hibernate Spatial을 활용한 위치 기반 사진 저장 및 조회
- **지도 클러스터링**: 줌 레벨에 따른 동적 그리드 기반 클러스터링으로 대량의 위치 데이터 시각화

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
└── mapping/                         # Domain <-> DTO 변환
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

`/map/me` API에서 사진 데이터 버전(`dataVersion`)을 관리합니다. 클라이언트가 `lastDataVersion`을 전달하면, 데이터 변경이 없을 경우 클러스터/사진 데이터를 생략(null)
하여 응답 크기를 줄입니다.

- 적용 범위는 **요청 시점의 뷰포트(bbox) + album 필터 기준**입니다.
- 값이 같으면 `clusters`/`photos`만 null로 내려가며, 위치/앨범 목록/집계 값은 계속 응답됩니다.
- 현재 증분 버전 파라미터(`lastDataVersion`)는 `/map/me`에만 제공됩니다.

> 참고: 기본 앨범(`isDefault=true`) 요청은 `albumId`를 `null`로 정규화하여 처리합니다.
> `dataVersion` 계산과 실제 사진 조회 모두 동일한 정규화 기준을 사용합니다.

**Technical Decisions & Trade-offs**

| 주제                   | 선택                           | 이유                        | 트레이드오프               |
|----------------------|------------------------------|---------------------------|----------------------|
| `lastDataVersion` 범위 | `/map/me`에만 적용               | 지도 핵심 데이터 통합 응답이라 효과가 큼   | 다른 API는 HTTP 캐시에 의존  |
| 버전 계산 단위             | 뷰포트(bbox)+앨범 필터 기준           | 화면과 무관한 변경으로 재조회되는 것 방지   | 필터 정합성/엣지 케이스 복잡도 증가 |
| 클러스터 prefetch        | 방향/속도 기반 선행 적재               | 체감 지연 감소, 불필요 prefetch 억제 | 로직 복잡도 증가            |
| 캐시 무효화               | 포인트 기반 + 필요 시 커플 단위          | 캐시 적중률 유지                 | 무효화 조건 관리 비용 증가      |
| DB 병렬도               | 세마포어로 상한 제어                  | 커넥션 풀 고갈 방지, 안정성 확보       | 피크 순간 처리량 일부 희생      |
| 공간 쿼리 범위             | grid margin 확장 조회            | 경계 셀 누락 완화                | 조회 범위 증가로 단건 비용 상승   |
| 캐시 키 전략              | bbox를 격자 단위로 정렬              | 미세 pan에도 키 안정화, 재사용률 향상   | 키 계산/좌표 변환 로직 필요     |
| 기본 앨범 모델             | 조회 시점 병합 집계                  | UX 단순화(전체 사진 보기)          | 조회/캐시 무효화 경계 복잡      |
| Presigned URL 멱등성    | `X-Idempotency-Key` + 3분 TTL | 중복 발급 억제, 운영 단순화          | 영구 멱등성은 아님           |

### HTTP Caching

**Cache-Control**

엔드포인트 특성에 따라 차등화된 `Cache-Control` 헤더를 설정합니다.

| 패턴                | 정책                      |
|-------------------|-------------------------|
| 역지오코딩, 장소 검색      | `private, max-age=3600` |
| 앨범 정보, 사진 상세      | `private, max-age=300`  |
| 목록 조회             | `private, max-age=60`   |
| 지도 ME (`/map/me`) | `private, max-age=30`   |
| 쓰기 / 인증           | `no-store`              |

**ETag**

`ShallowEtagHeaderFilter`로 모든 GET 응답에 ETag를 자동 생성합니다. `max-age` 만료 후 304 Not Modified 응답으로 본문 전송을 생략하여 대역폭을 절약합니다.

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
