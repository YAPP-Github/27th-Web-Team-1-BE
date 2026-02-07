# Lokit

> 우리만의 이야기를, 지도에 Lokit
> 사진과 장소를 함께 기록하는 지도 기반 앨범 서비스

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
| `mapCells`       | 3분  | 300      | 그리드 셀 단위 클러스터 캐시      |
| `mapPhotos`      | 1분  | 500      | Bounding Box 기반 사진 캐시 |
| `reverseGeocode` | 1시간 | 500      | 역지오코딩 결과              |
| `userCouple`     | 1시간 | 500      | 사용자-커플 매핑             |
| `presignedUrl`   | 5분  | 100      | S3 Presigned URL      |

**Grid Cell Caching**

지도 클러스터 조회 시 Bounding Box 내 모든 그리드 셀을 계산하고, 캐시된 셀은 건너뛰어 미캐싱 셀만 DB에서 조회합니다. 지도 이동 시 겹치는 영역의 재조회를 방지합니다.

**dataVersion (증분 캐시)**

`/map/me` API에서 커플의 사진 데이터 버전(`dataVersion`)을 관리합니다. 클라이언트가 `lastDataVersion`을 전달하면, 데이터 변경이 없을 경우 클러스터/사진 데이터를 생략(null)
하여 응답 크기를 대폭 줄입니다. 사진 업로드/삭제/앨범 이동 시에만 버전이 증가합니다.

### HTTP Caching

**Cache-Control**

엔드포인트 특성에 따라 차등화된 `Cache-Control` 헤더를 설정합니다.

| 패턴           | 정책                      |
|--------------|-------------------------|
| 역지오코딩, 장소 검색 | `private, max-age=3600` |
| 앨범 정보, 사진 상세 | `private, max-age=300`  |
| 목록 조회        | `private, max-age=60`   |
| 지도 홈         | `private, max-age=30`   |
| 쓰기 / 인증      | `no-store`              |

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
