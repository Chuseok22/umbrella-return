# 동아리 우산 대여 서비스 설계 문서

## 개요

동아리 우산 무료 대여 서비스. 기존 카카오톡 수기 방식의 반납 관리 문제를 해결하기 위해 웹 서비스로 전환. QR 코드 기반으로 대여/반납을 처리하고, 미반납자에게 매일 오전 7시 SMS 자동 발송.

---

## 기술 스택

| 항목 | 선택 |
|------|------|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.0.6 |
| 뷰 | Thymeleaf + DaisyUI (CDN) |
| DB | PostgreSQL |
| 인증 | Spring Security (In-Memory) |
| SMS | 알리고 API |
| 빌드 | Gradle |

---

## 아키텍처

### 전체 구조

```
[QR 코드] → GET /
              ├── GET /rent  → POST /rent
              └── GET /return → POST /return

[관리자] → GET /admin/login → POST /admin/login (Spring Security)
           └── /admin/**  (인증 필요)
               ├── /admin            대시보드
               ├── /admin/rentals    현재 대여 현황 + 반납 처리
               ├── /admin/history    대여/반납 이력
               ├── /admin/umbrellas  우산 등록/관리
               ├── /admin/sms        SMS 발송 내역
               └── /admin/sms/send   SMS 수동 발송

[Spring Scheduler] → 매일 07:00 → 미반납자 조회 → 알리고 SMS 발송
```

### 레이어 구성

```
Controller → Service → Repository → PostgreSQL
                  ↓
            AligoSmsClient (외부 API)
```

### 패키지 구조

```
com.chuseok22.umbrellareturn
├── controller/
│   ├── RentalController           사용자 대여/반납
│   └── admin/
│       ├── AdminDashboardController
│       ├── AdminRentalController
│       ├── AdminUmbrellaController
│       └── AdminSmsController
├── service/
│   ├── RentalService
│   ├── UmbrellaService
│   └── SmsService
├── repository/
├── entity/
├── scheduler/
│   └── SmsScheduler
├── config/
│   └── SecurityConfig
└── client/
    └── AligoSmsClient
```

---

## 데이터베이스 스키마

### umbrella (우산)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 증가 |
| number | VARCHAR(20) UNIQUE NOT NULL | 우산 번호 |
| status | VARCHAR(20) NOT NULL | AVAILABLE, RENTED |
| created_at | TIMESTAMP NOT NULL | 등록 일시 |

### rental (대여 기록)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 증가 |
| umbrella_id | BIGINT FK | umbrella.id |
| borrower_name | VARCHAR(50) NOT NULL | 대여자 이름 |
| borrower_phone | VARCHAR(20) NOT NULL | 대여자 전화번호 |
| borrower_student_id | VARCHAR(20) NOT NULL | 대여자 학번 |
| rented_at | TIMESTAMP NOT NULL | 대여 일시 |
| returned_at | TIMESTAMP NULL | 반납 일시 (NULL = 대여 중) |
| status | VARCHAR(20) NOT NULL | RENTED, RETURNED |

### sms_log (SMS 발송 내역)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 자동 증가 |
| rental_id | BIGINT FK | rental.id |
| phone | VARCHAR(20) NOT NULL | 수신 번호 |
| message | TEXT NOT NULL | 발송 메시지 |
| sent_at | TIMESTAMP NOT NULL | 발송 일시 |
| success | BOOLEAN NOT NULL | 발송 성공 여부 |
| response | TEXT NULL | 알리고 API 응답 원문 |

### 주요 쿼리 패턴

- 대여 가능 우산 조회: `status = AVAILABLE`
- 미반납자 조회 (스케줄러): `status = RENTED AND DATE(rented_at) < TODAY`
- 대여 이력: rental 전체 + umbrella JOIN

---

## 사용자 화면 흐름

### 메인 페이지 (`GET /`)
QR 코드 접속 시 첫 화면. "대여하기" / "반납하기" 두 버튼만 표시. 모바일 중심 단순 레이아웃.

### 대여 페이지 (`GET /rent` → `POST /rent`)

입력 필드:
- 이름 (text)
- 전화번호 (tel)
- 학번 (text)
- 우산 번호 (select — AVAILABLE 상태 우산만 표시)

처리 로직:
1. 해당 우산이 여전히 AVAILABLE인지 재검증 (@Transactional + status 재조회로 동시 접근 방어)
2. rental 레코드 생성
3. umbrella status → RENTED
4. 완료 페이지로 redirect (PRG 패턴)

### 반납 페이지 (`GET /return` → `POST /return`)

입력 필드:
- 이름 (text)
- 전화번호 (tel)
- 우산 번호 (select — RENTED 상태 우산만 표시)

처리 로직:
1. 이름 + 전화번호 + 우산번호 3가지 일치 검증
2. rental.returned_at 설정, status → RETURNED
3. umbrella status → AVAILABLE
4. 완료 페이지로 redirect

### 완료 페이지
대여 완료 / 반납 완료 메시지 표시. 메인으로 돌아가는 버튼.

### 오류 처리
- 우산 이미 대여 중 → 오류 메시지와 함께 폼 재표시
- 반납 정보 불일치 → 오류 메시지와 함께 폼 재표시

---

## 관리자 기능

### 인증
- Spring Security In-Memory 유저
- 계정: id=chuseok22, pw=interface518
- `/admin/**` 전체 보호
- 로그인 페이지: `/admin/login`

### 대시보드 (`/admin`)
- 전체 우산 수 / 대여 중 수 / 반납 가능 수 요약 카드
- 현재 대여 중 목록 간략 표시

### 대여 현황 (`/admin/rentals`)
- 현재 대여 중 전체 목록 (이름, 학번, 전화번호, 우산번호, 대여일시)
- 각 행에 반납 처리 버튼 → `POST /admin/rentals/{id}/return`

### 대여/반납 이력 (`/admin/history`)
- 전체 대여 기록 (반납 완료 포함)
- 날짜 필터: 1차 구현 범위 제외 (전체 이력 목록만 표시)

### 우산 관리 (`/admin/umbrellas`)
- 등록된 우산 목록 (번호, 상태)
- 우산 번호 입력 후 등록 → `POST /admin/umbrellas`
- 삭제 버튼 (AVAILABLE 상태만 삭제 가능)

### SMS 관리 (`/admin/sms`)
- 발송 내역 목록 (수신자, 메시지, 발송 시각, 성공 여부)
- 수동 발송 버튼 → 현재 미반납자 전체에게 즉시 SMS 발송

---

## SMS 연동

### 알리고 API

```
HTTP POST https://apis.aligo.in/send/
파라미터:
- key     : API 키
- user_id : 알리고 계정 ID
- sender  : 발신 번호
- receiver: 수신 번호
- msg     : 문자 내용
```

`AligoSmsClient`에서 Spring Boot 4의 `RestClient`로 호출.

### 스케줄러

```
매일 07:00 (cron = "0 0 7 * * *")
→ status = RENTED AND DATE(rented_at) < TODAY 조회
→ 알리고 API 건별 호출
→ sms_log 저장
```

### SMS 메시지

```
[인터페이스] 우산 반납 알림
{이름}님, 대여하신 {우산번호}번 우산을
아직 반납하지 않으셨습니다.
우산함 앞 QR코드로 반납 부탁드립니다.
```

---

## UI 구성

### DaisyUI 적용

```html
<link href="https://cdn.jsdelivr.net/npm/daisyui@latest/dist/full.css" rel="stylesheet"/>
<script src="https://cdn.tailwindcss.com"></script>
```

- 빌드 도구 없이 CDN으로 적용
- 모바일 + 데스크톱 반응형
- Thymeleaf 레이아웃 템플릿으로 공통 레이아웃 관리

---

## 추가 의존성

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
```

---

## 환경변수

```yaml
# application-prod.yml
aligo:
  api-key: ${ALIGO_API_KEY}
  user-id: ${ALIGO_USER_ID}
  sender: ${ALIGO_SENDER}

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

---

## 미결 사항

- 없음
