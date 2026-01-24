# Lokit

> 사진과 장소를 함께 기록하는 지도 기반 앨범 서비스

Lokit은 사진을 앨범 단위로 관리하고, 촬영 위치를 기반으로 지도 위에서 추억을 한눈에 돌아볼 수 있는 모바일 중심 서비스입니다.

<br>

## Highlights

- **이미지 처리**: AWS S3 Presigned URL 기반 대용량 이미지 업로드 및 관리
- **공간 데이터 처리**: PostGIS + Hibernate Spatial을 활용한 위치 기반 사진 저장 및 조회
- **지도 클러스터링**: 줌 레벨에 따른 동적 그리드 기반 클러스터링으로 대량의 위치 데이터 시각화

<br>

## Tech Stack

| Category  | Technologies                            |
|-----------|-----------------------------------------|
| Language  | Kotlin 2.2, Java 24                     |
| Framework | Spring Boot 4.0                         |
| Database  | PostgreSQL, PostGIS (공간 데이터), H2 (Test) |
| ORM       | Spring Data JPA                         |
| Auth      | Spring Security, JWT (jjwt)             |
| Cloud     | AWS S3 (이미지 스토리지)                       |
| Docs      | SpringDoc OpenAPI (Swagger)             |
| Build     | Gradle (Kotlin DSL)                     |
| Infra     | Docker, Docker Compose                  |

<br>

## Architecture

```
src/main/kotlin/kr/co/lokit/api/
├── config/                    # 설정 (Security, Web, S3, OpenAPI 등)
├── domain/                    # 도메인별 비즈니스 로직
│   ├── user/                  # 사용자 인증
│   ├── workspace/             # 워크스페이스 관리
│   ├── album/                 # 앨범 CRUD
│   ├── photo/                 # 사진 업로드 및 관리
│   └── map/                   # 지도 기반 조회 및 클러스터링
└── common/                    # 공통 유틸리티, 예외 처리
```

각 도메인은 **DDD(Domain Driven Design)** 패턴에 따라 다음 계층으로 구성됩니다:

```
presentation  →  application  →  domain  →  infrastructure
(Controller)     (Service)       (Model)     (Repository)
```

<br>

## Features

### Authentication

- 이메일 기반 로그인 (자동 회원가입)
- JWT Access Token / Refresh Token 발급

### Workspace

- 워크스페이스 생성 및 초대 코드 발급
- 초대 코드를 통한 워크스페이스 참여

### Album

- 앨범 생성, 수정, 삭제
- 앨범별 사진 관리 및 썸네일 자동 설정

### Photo

- **AWS S3 Presigned URL**을 통한 대용량 이미지 직접 업로드
- 사진 메타데이터 저장
- **PostGIS Point 타입**으로 GPS 좌표(위도/경도) 저장

### Map

- **Bounding Box 기반 공간 쿼리**로 화면 내 사진만 조회
- **줌 레벨 기반 동적 클러스터링**
    - Zoom ≥ 15: 개별 사진 마커 표시
    - Zoom < 15: 그리드 기반 클러스터로 그룹화
- JTS(Java Topology Suite)를 활용한 기하학 연산

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
