# 우산 대여 서비스 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** QR 코드 기반 우산 대여/반납 웹 서비스 구현 — 사용자 대여/반납 폼, 관리자 관리 화면, 미반납자 자동 SMS 알림 포함.

**Architecture:** Spring Boot MVC + Thymeleaf 서버 사이드 렌더링. Spring Security In-Memory 인증으로 `/admin/**` 보호. Spring Scheduler로 매일 07:00 알리고 SMS API 호출.

**Tech Stack:** Spring Boot 4.0.6, Java 21, Thymeleaf, DaisyUI 4 (CDN), PostgreSQL, Spring Security 6, Aligo SMS API, Gradle

---

## 파일 구조

```
src/main/java/com/chuseok22/umbrellareturn/
├── config/
│   └── SecurityConfig.java
├── entity/
│   ├── UmbrellaStatus.java        (enum: AVAILABLE, RENTED)
│   ├── RentalStatus.java          (enum: RENTED, RETURNED)
│   ├── Umbrella.java
│   ├── Rental.java
│   └── SmsLog.java
├── repository/
│   ├── UmbrellaRepository.java
│   ├── RentalRepository.java
│   └── SmsLogRepository.java
├── service/
│   ├── UmbrellaService.java
│   ├── RentalService.java
│   └── SmsService.java
├── controller/
│   ├── RentalController.java
│   └── admin/
│       ├── AdminDashboardController.java
│       ├── AdminRentalController.java
│       ├── AdminUmbrellaController.java
│       └── AdminSmsController.java
├── dto/
│   ├── RentForm.java
│   ├── ReturnForm.java
│   └── UmbrellaForm.java
├── client/
│   └── AligoSmsClient.java
└── scheduler/
    └── SmsScheduler.java

src/main/resources/
├── templates/
│   ├── fragments/
│   │   └── layout.html
│   ├── index.html
│   ├── rent.html
│   ├── return.html
│   ├── complete.html
│   └── admin/
│       ├── login.html
│       ├── dashboard.html
│       ├── rentals.html
│       ├── history.html
│       ├── umbrellas.html
│       └── sms.html
├── application.yml
├── application-local.yml
└── application-prod.yml

src/test/java/com/chuseok22/umbrellareturn/
├── service/
│   ├── UmbrellaServiceTest.java
│   ├── RentalServiceTest.java
│   └── SmsServiceTest.java
└── controller/
    ├── RentalControllerTest.java
    └── admin/
        ├── AdminUmbrellaControllerTest.java
        └── AdminRentalControllerTest.java
```

---

## Task 1: 프로젝트 설정 (의존성 + Security + application.yml)

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/chuseok22/umbrellareturn/config/SecurityConfig.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/application-local.yml`

- [ ] **Step 1: build.gradle 의존성 교체**

기존 dependencies 블록 전체를 아래로 교체한다:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.thymeleaf.extras:thymeleaf-extras-springsecurity6'
    runtimeOnly 'org.postgresql:postgresql'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-thymeleaf-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

- [ ] **Step 2: SecurityConfig 생성**

`src/main/java/com/chuseok22/umbrellareturn/config/SecurityConfig.java`:

```java
package com.chuseok22.umbrellareturn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("chuseok22")
            .password("{noop}interface518")
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
```

- [ ] **Step 3: application.yml 수정 (로컬/운영 프로파일 분리)**

`src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: umbrella-return
  profiles:
    active: local
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        show_sql: true
        format_sql: true
        use_sql_comments: true

aligo:
  api-key: ${ALIGO_API_KEY:test-key}
  user-id: ${ALIGO_USER_ID:test-user}
  sender: ${ALIGO_SENDER:01000000000}
```

`src/main/resources/application-local.yml` (새 파일):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/umbrella_return
    username: postgres
    password: postgres
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add build.gradle src/main/java/com/chuseok22/umbrellareturn/config/SecurityConfig.java src/main/resources/application.yml src/main/resources/application-local.yml
git commit -m "chore: add security/validation dependencies and configure spring security"
```

---

## Task 2: 도메인 레이어 (엔티티 + 레포지토리)

**Files:**
- Create: `src/main/java/com/chuseok22/umbrellareturn/entity/UmbrellaStatus.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/entity/RentalStatus.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/entity/Umbrella.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/entity/Rental.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/entity/SmsLog.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/repository/UmbrellaRepository.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/repository/RentalRepository.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/repository/SmsLogRepository.java`

- [ ] **Step 1: UmbrellaStatus enum 생성**

```java
package com.chuseok22.umbrellareturn.entity;

public enum UmbrellaStatus {
    AVAILABLE, RENTED
}
```

- [ ] **Step 2: RentalStatus enum 생성**

```java
package com.chuseok22.umbrellareturn.entity;

public enum RentalStatus {
    RENTED, RETURNED
}
```

- [ ] **Step 3: Umbrella 엔티티 생성**

```java
package com.chuseok22.umbrellareturn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "umbrella")
public class Umbrella {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UmbrellaStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Umbrella() {}

    public Umbrella(String number) {
        this.number = number;
        this.status = UmbrellaStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public UmbrellaStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void markRented() { this.status = UmbrellaStatus.RENTED; }
    public void markAvailable() { this.status = UmbrellaStatus.AVAILABLE; }
    public boolean isAvailable() { return this.status == UmbrellaStatus.AVAILABLE; }
}
```

- [ ] **Step 4: Rental 엔티티 생성**

```java
package com.chuseok22.umbrellareturn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental")
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "umbrella_id", nullable = false)
    private Umbrella umbrella;

    @Column(nullable = false, length = 50)
    private String borrowerName;

    @Column(nullable = false, length = 20)
    private String borrowerPhone;

    @Column(nullable = false, length = 20)
    private String borrowerStudentId;

    @Column(nullable = false)
    private LocalDateTime rentedAt;

    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;

    protected Rental() {}

    public Rental(Umbrella umbrella, String borrowerName, String borrowerPhone, String borrowerStudentId) {
        this.umbrella = umbrella;
        this.borrowerName = borrowerName;
        this.borrowerPhone = borrowerPhone;
        this.borrowerStudentId = borrowerStudentId;
        this.rentedAt = LocalDateTime.now();
        this.status = RentalStatus.RENTED;
    }

    public Long getId() { return id; }
    public Umbrella getUmbrella() { return umbrella; }
    public String getBorrowerName() { return borrowerName; }
    public String getBorrowerPhone() { return borrowerPhone; }
    public String getBorrowerStudentId() { return borrowerStudentId; }
    public LocalDateTime getRentedAt() { return rentedAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public RentalStatus getStatus() { return status; }

    public void markReturned() {
        this.returnedAt = LocalDateTime.now();
        this.status = RentalStatus.RETURNED;
    }

    public boolean isActive() { return this.status == RentalStatus.RENTED; }
}
```

- [ ] **Step 5: SmsLog 엔티티 생성**

```java
package com.chuseok22.umbrellareturn.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_log")
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String response;

    protected SmsLog() {}

    public SmsLog(Rental rental, String phone, String message, boolean success, String response) {
        this.rental = rental;
        this.phone = phone;
        this.message = message;
        this.sentAt = LocalDateTime.now();
        this.success = success;
        this.response = response;
    }

    public Long getId() { return id; }
    public Rental getRental() { return rental; }
    public String getPhone() { return phone; }
    public String getMessage() { return message; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isSuccess() { return success; }
    public String getResponse() { return response; }
}
```

- [ ] **Step 6: UmbrellaRepository 생성**

```java
package com.chuseok22.umbrellareturn.repository;

import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.entity.UmbrellaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UmbrellaRepository extends JpaRepository<Umbrella, Long> {
    List<Umbrella> findByStatusOrderByNumberAsc(UmbrellaStatus status);
    List<Umbrella> findAllByOrderByNumberAsc();
    Optional<Umbrella> findByNumber(String number);
    boolean existsByNumber(String number);
}
```

- [ ] **Step 7: RentalRepository 생성**

```java
package com.chuseok22.umbrellareturn.repository;

import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findByStatusOrderByRentedAtDesc(RentalStatus status);

    List<Rental> findAllByOrderByRentedAtDesc();

    Optional<Rental> findByUmbrella_NumberAndBorrowerNameAndBorrowerPhoneAndStatus(
        String umbrellaNumber, String borrowerName, String borrowerPhone, RentalStatus status
    );

    // 오늘 자정(todayStart) 이전에 대여 시작한 미반납 건 조회
    @Query("SELECT r FROM Rental r JOIN FETCH r.umbrella WHERE r.status = 'RENTED' AND r.rentedAt < :todayStart")
    List<Rental> findUnreturnedBefore(@Param("todayStart") LocalDateTime todayStart);

    long countByStatus(RentalStatus status);
}
```

- [ ] **Step 8: SmsLogRepository 생성**

```java
package com.chuseok22.umbrellareturn.repository;

import com.chuseok22.umbrellareturn.entity.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    List<SmsLog> findAllByOrderBySentAtDesc();
}
```

- [ ] **Step 9: 빌드 확인**

```bash
./gradlew build -x test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 10: 커밋**

```bash
git add src/main/java/com/chuseok22/umbrellareturn/entity/ src/main/java/com/chuseok22/umbrellareturn/repository/
git commit -m "feat: add domain entities and repositories"
```

---

## Task 3: 서비스 레이어 (UmbrellaService + RentalService)

**Files:**
- Create: `src/main/java/com/chuseok22/umbrellareturn/dto/RentForm.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/dto/ReturnForm.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/dto/UmbrellaForm.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/service/UmbrellaService.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/service/RentalService.java`
- Create: `src/test/java/com/chuseok22/umbrellareturn/service/UmbrellaServiceTest.java`
- Create: `src/test/java/com/chuseok22/umbrellareturn/service/RentalServiceTest.java`

- [ ] **Step 1: DTO 클래스 생성**

`src/main/java/com/chuseok22/umbrellareturn/dto/RentForm.java`:

```java
package com.chuseok22.umbrellareturn.dto;

import jakarta.validation.constraints.NotBlank;

public class RentForm {

    @NotBlank
    private String borrowerName;

    @NotBlank
    private String borrowerPhone;

    @NotBlank
    private String borrowerStudentId;

    @NotBlank
    private String umbrellaNumber;

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getBorrowerPhone() { return borrowerPhone; }
    public void setBorrowerPhone(String borrowerPhone) { this.borrowerPhone = borrowerPhone; }
    public String getBorrowerStudentId() { return borrowerStudentId; }
    public void setBorrowerStudentId(String borrowerStudentId) { this.borrowerStudentId = borrowerStudentId; }
    public String getUmbrellaNumber() { return umbrellaNumber; }
    public void setUmbrellaNumber(String umbrellaNumber) { this.umbrellaNumber = umbrellaNumber; }
}
```

`src/main/java/com/chuseok22/umbrellareturn/dto/ReturnForm.java`:

```java
package com.chuseok22.umbrellareturn.dto;

import jakarta.validation.constraints.NotBlank;

public class ReturnForm {

    @NotBlank
    private String borrowerName;

    @NotBlank
    private String borrowerPhone;

    @NotBlank
    private String umbrellaNumber;

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getBorrowerPhone() { return borrowerPhone; }
    public void setBorrowerPhone(String borrowerPhone) { this.borrowerPhone = borrowerPhone; }
    public String getUmbrellaNumber() { return umbrellaNumber; }
    public void setUmbrellaNumber(String umbrellaNumber) { this.umbrellaNumber = umbrellaNumber; }
}
```

`src/main/java/com/chuseok22/umbrellareturn/dto/UmbrellaForm.java`:

```java
package com.chuseok22.umbrellareturn.dto;

import jakarta.validation.constraints.NotBlank;

public class UmbrellaForm {

    @NotBlank
    private String number;

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
}
```

- [ ] **Step 2: UmbrellaService 생성**

```java
package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.entity.UmbrellaStatus;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UmbrellaService {

    private final UmbrellaRepository umbrellaRepository;

    public UmbrellaService(UmbrellaRepository umbrellaRepository) {
        this.umbrellaRepository = umbrellaRepository;
    }

    public List<Umbrella> findAvailable() {
        return umbrellaRepository.findByStatusOrderByNumberAsc(UmbrellaStatus.AVAILABLE);
    }

    public List<Umbrella> findRented() {
        return umbrellaRepository.findByStatusOrderByNumberAsc(UmbrellaStatus.RENTED);
    }

    public List<Umbrella> findAll() {
        return umbrellaRepository.findAllByOrderByNumberAsc();
    }

    @Transactional
    public void register(String number) {
        if (umbrellaRepository.existsByNumber(number)) {
            throw new IllegalArgumentException("이미 등록된 우산 번호입니다: " + number);
        }
        umbrellaRepository.save(new Umbrella(number));
    }

    @Transactional
    public void delete(Long id) {
        Umbrella umbrella = umbrellaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 우산입니다."));
        if (!umbrella.isAvailable()) {
            throw new IllegalStateException("대여 중인 우산은 삭제할 수 없습니다.");
        }
        umbrellaRepository.delete(umbrella);
    }

    public long countAvailable() {
        return umbrellaRepository.findByStatusOrderByNumberAsc(UmbrellaStatus.AVAILABLE).size();
    }

    public long countTotal() {
        return umbrellaRepository.count();
    }
}
```

- [ ] **Step 3: UmbrellaServiceTest 작성**

```java
package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.entity.UmbrellaStatus;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UmbrellaServiceTest {

    @Mock UmbrellaRepository umbrellaRepository;
    @InjectMocks UmbrellaService umbrellaService;

    @Test
    void 중복_번호_등록_시_예외() {
        given(umbrellaRepository.existsByNumber("001")).willReturn(true);

        assertThatThrownBy(() -> umbrellaService.register("001"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 등록된");
    }

    @Test
    void 정상_등록() {
        given(umbrellaRepository.existsByNumber("002")).willReturn(false);
        given(umbrellaRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> umbrellaService.register("002")).doesNotThrowAnyException();
        then(umbrellaRepository).should().save(any(Umbrella.class));
    }

    @Test
    void 대여중_우산_삭제_시_예외() {
        Umbrella rented = new Umbrella("003");
        rented.markRented();
        given(umbrellaRepository.findById(1L)).willReturn(Optional.of(rented));

        assertThatThrownBy(() -> umbrellaService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("대여 중인 우산");
    }
}
```

- [ ] **Step 4: UmbrellaService 테스트 실행**

```bash
./gradlew test --tests "com.chuseok22.umbrellareturn.service.UmbrellaServiceTest"
```

Expected: 3 tests PASSED

- [ ] **Step 5: RentalService 생성**

```java
package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.dto.ReturnForm;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.RentalStatus;
import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.entity.UmbrellaStatus;
import com.chuseok22.umbrellareturn.repository.RentalRepository;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RentalService {

    private final RentalRepository rentalRepository;
    private final UmbrellaRepository umbrellaRepository;

    public RentalService(RentalRepository rentalRepository, UmbrellaRepository umbrellaRepository) {
        this.rentalRepository = rentalRepository;
        this.umbrellaRepository = umbrellaRepository;
    }

    @Transactional
    public void rent(RentForm form) {
        Umbrella umbrella = umbrellaRepository.findByNumber(form.getUmbrellaNumber())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 우산 번호입니다."));

        if (!umbrella.isAvailable()) {
            throw new IllegalStateException("이미 대여 중인 우산입니다.");
        }

        umbrella.markRented();
        rentalRepository.save(new Rental(
            umbrella,
            form.getBorrowerName(),
            form.getBorrowerPhone(),
            form.getBorrowerStudentId()
        ));
    }

    @Transactional
    public void returnUmbrella(ReturnForm form) {
        Rental rental = rentalRepository.findByUmbrella_NumberAndBorrowerNameAndBorrowerPhoneAndStatus(
            form.getUmbrellaNumber(), form.getBorrowerName(), form.getBorrowerPhone(), RentalStatus.RENTED
        ).orElseThrow(() -> new IllegalArgumentException("대여 정보가 일치하지 않습니다."));

        rental.markReturned();
        rental.getUmbrella().markAvailable();
    }

    @Transactional
    public void adminReturn(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 내역입니다."));
        rental.markReturned();
        rental.getUmbrella().markAvailable();
    }

    public List<Rental> findActiveRentals() {
        return rentalRepository.findByStatusOrderByRentedAtDesc(RentalStatus.RENTED);
    }

    public List<Rental> findAllRentals() {
        return rentalRepository.findAllByOrderByRentedAtDesc();
    }

    public List<Rental> findUnreturnedBefore(LocalDateTime todayStart) {
        return rentalRepository.findUnreturnedBefore(todayStart);
    }

    public long countActive() {
        return rentalRepository.countByStatus(RentalStatus.RENTED);
    }
}
```

- [ ] **Step 6: RentalServiceTest 작성**

```java
package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.dto.ReturnForm;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.RentalStatus;
import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.repository.RentalRepository;
import com.chuseok22.umbrellareturn.repository.UmbrellaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock RentalRepository rentalRepository;
    @Mock UmbrellaRepository umbrellaRepository;
    @InjectMocks RentalService rentalService;

    private RentForm rentForm(String umbrellaNumber) {
        RentForm form = new RentForm();
        form.setUmbrellaNumber(umbrellaNumber);
        form.setBorrowerName("홍길동");
        form.setBorrowerPhone("01012345678");
        form.setBorrowerStudentId("20230001");
        return form;
    }

    @Test
    void 이미_대여중인_우산_대여_시_예외() {
        Umbrella rented = new Umbrella("001");
        rented.markRented();
        given(umbrellaRepository.findByNumber("001")).willReturn(Optional.of(rented));

        assertThatThrownBy(() -> rentalService.rent(rentForm("001")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 대여 중");
    }

    @Test
    void 정상_대여() {
        Umbrella available = new Umbrella("002");
        given(umbrellaRepository.findByNumber("002")).willReturn(Optional.of(available));
        given(rentalRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> rentalService.rent(rentForm("002"))).doesNotThrowAnyException();
        then(rentalRepository).should().save(any(Rental.class));
    }

    @Test
    void 반납_정보_불일치_시_예외() {
        ReturnForm form = new ReturnForm();
        form.setUmbrellaNumber("001");
        form.setBorrowerName("홍길동");
        form.setBorrowerPhone("01099999999");

        given(rentalRepository.findByUmbrella_NumberAndBorrowerNameAndBorrowerPhoneAndStatus(
            "001", "홍길동", "01099999999", RentalStatus.RENTED
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> rentalService.returnUmbrella(form))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("일치하지 않습니다");
    }
}
```

- [ ] **Step 7: RentalService 테스트 실행**

```bash
./gradlew test --tests "com.chuseok22.umbrellareturn.service.RentalServiceTest"
```

Expected: 3 tests PASSED

- [ ] **Step 8: 커밋**

```bash
git add src/main/java/com/chuseok22/umbrellareturn/dto/ src/main/java/com/chuseok22/umbrellareturn/service/ src/test/java/com/chuseok22/umbrellareturn/service/
git commit -m "feat: add dto, umbrella/rental service with tests"
```

---

## Task 4: SMS 클라이언트 + 서비스 + 스케줄러

**Files:**
- Create: `src/main/java/com/chuseok22/umbrellareturn/client/AligoSmsClient.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/service/SmsService.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/scheduler/SmsScheduler.java`
- Create: `src/test/java/com/chuseok22/umbrellareturn/service/SmsServiceTest.java`

- [ ] **Step 1: application.yml에 aligo 설정 바인딩 클래스 생성**

`src/main/java/com/chuseok22/umbrellareturn/config/AligoProperties.java`:

```java
package com.chuseok22.umbrellareturn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aligo")
public class AligoProperties {

    private String apiKey;
    private String userId;
    private String sender;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
}
```

`UmbrellaReturnApplication.java`에 `@EnableConfigurationProperties` 또는 `@ConfigurationPropertiesScan` 추가:

```java
package com.chuseok22.umbrellareturn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UmbrellaReturnApplication {
    public static void main(String[] args) {
        SpringApplication.run(UmbrellaReturnApplication.class, args);
    }
}
```

- [ ] **Step 2: AligoSmsClient 생성**

```java
package com.chuseok22.umbrellareturn.client;

import com.chuseok22.umbrellareturn.config.AligoProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class AligoSmsClient {

    private static final String ALIGO_API_URL = "https://apis.aligo.in/send/";

    private final AligoProperties aligoProperties;
    private final RestClient restClient;

    public AligoSmsClient(AligoProperties aligoProperties) {
        this.aligoProperties = aligoProperties;
        this.restClient = RestClient.create();
    }

    // 발송 성공 시 true 반환
    public boolean send(String receiver, String message) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("key", aligoProperties.getApiKey());
        params.add("user_id", aligoProperties.getUserId());
        params.add("sender", aligoProperties.getSender());
        params.add("receiver", receiver);
        params.add("msg", message);
        params.add("msg_type", "SMS");

        try {
            String response = restClient.post()
                .uri(ALIGO_API_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(String.class);

            // result_code: "1" 이면 성공
            return response != null && response.contains("\"result_code\":\"1\"");
        } catch (Exception e) {
            return false;
        }
    }
}
```

- [ ] **Step 3: SmsService 생성**

```java
package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.client.AligoSmsClient;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.SmsLog;
import com.chuseok22.umbrellareturn.repository.SmsLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SmsService {

    private final AligoSmsClient aligoSmsClient;
    private final SmsLogRepository smsLogRepository;
    private final RentalService rentalService;

    public SmsService(AligoSmsClient aligoSmsClient, SmsLogRepository smsLogRepository, RentalService rentalService) {
        this.aligoSmsClient = aligoSmsClient;
        this.smsLogRepository = smsLogRepository;
        this.rentalService = rentalService;
    }

    @Transactional
    public int sendReminderToAll() {
        java.time.LocalDateTime todayStart = java.time.LocalDate.now().atStartOfDay();
        List<Rental> unreturned = rentalService.findUnreturnedBefore(todayStart);

        int successCount = 0;
        for (Rental rental : unreturned) {
            String message = buildMessage(rental);
            boolean success = aligoSmsClient.send(rental.getBorrowerPhone(), message);
            smsLogRepository.save(new SmsLog(rental, rental.getBorrowerPhone(), message, success, null));
            if (success) successCount++;
        }
        return successCount;
    }

    public List<SmsLog> findAllLogs() {
        return smsLogRepository.findAllByOrderBySentAtDesc();
    }

    private String buildMessage(Rental rental) {
        return String.format(
            "[인터페이스] 우산 반납 알림\n%s님, 대여하신 %s번 우산을\n아직 반납하지 않으셨습니다.\n우산함 앞 QR코드로 반납 부탁드립니다.",
            rental.getBorrowerName(),
            rental.getUmbrella().getNumber()
        );
    }
}
```

- [ ] **Step 4: SmsScheduler 생성**

```java
package com.chuseok22.umbrellareturn.scheduler;

import com.chuseok22.umbrellareturn.service.SmsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SmsScheduler {

    private final SmsService smsService;

    public SmsScheduler(SmsService smsService) {
        this.smsService = smsService;
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyReminder() {
        smsService.sendReminderToAll();
    }
}
```

- [ ] **Step 5: SmsServiceTest 작성**

```java
package com.chuseok22.umbrellareturn.service;

import com.chuseok22.umbrellareturn.client.AligoSmsClient;
import com.chuseok22.umbrellareturn.entity.Rental;
import com.chuseok22.umbrellareturn.entity.Umbrella;
import com.chuseok22.umbrellareturn.repository.SmsLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock AligoSmsClient aligoSmsClient;
    @Mock SmsLogRepository smsLogRepository;
    @Mock RentalService rentalService;
    @InjectMocks SmsService smsService;

    @Test
    void 미반납자_전체_발송_성공_카운트() {
        Umbrella umbrella = new Umbrella("001");
        Rental rental = new Rental(umbrella, "홍길동", "01012345678", "20230001");

        given(rentalService.findUnreturnedBefore(any(LocalDateTime.class)))
            .willReturn(List.of(rental));
        given(aligoSmsClient.send(anyString(), anyString())).willReturn(true);
        given(smsLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        int count = smsService.sendReminderToAll();

        assertThat(count).isEqualTo(1);
        then(smsLogRepository).should().save(any());
    }

    @Test
    void 미반납자_없으면_발송_안함() {
        given(rentalService.findUnreturnedBefore(any(LocalDateTime.class)))
            .willReturn(List.of());

        int count = smsService.sendReminderToAll();

        assertThat(count).isEqualTo(0);
        then(aligoSmsClient).shouldHaveNoInteractions();
    }
}
```

- [ ] **Step 6: 테스트 실행**

```bash
./gradlew test --tests "com.chuseok22.umbrellareturn.service.SmsServiceTest"
```

Expected: 2 tests PASSED

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/chuseok22/umbrellareturn/client/ src/main/java/com/chuseok22/umbrellareturn/service/SmsService.java src/main/java/com/chuseok22/umbrellareturn/scheduler/ src/main/java/com/chuseok22/umbrellareturn/config/AligoProperties.java src/main/java/com/chuseok22/umbrellareturn/UmbrellaReturnApplication.java src/test/java/com/chuseok22/umbrellareturn/service/SmsServiceTest.java
git commit -m "feat: add aligo sms client, sms service, and daily scheduler"
```

---

## Task 5: 사용자 화면 (컨트롤러 + Thymeleaf 템플릿)

**Files:**
- Create: `src/main/java/com/chuseok22/umbrellareturn/controller/RentalController.java`
- Create: `src/main/resources/templates/fragments/layout.html`
- Create: `src/main/resources/templates/index.html`
- Create: `src/main/resources/templates/rent.html`
- Create: `src/main/resources/templates/return.html`
- Create: `src/main/resources/templates/complete.html`
- Create: `src/test/java/com/chuseok22/umbrellareturn/controller/RentalControllerTest.java`

- [ ] **Step 1: RentalController 생성**

```java
package com.chuseok22.umbrellareturn.controller;

import com.chuseok22.umbrellareturn.dto.RentForm;
import com.chuseok22.umbrellareturn.dto.ReturnForm;
import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RentalController {

    private final RentalService rentalService;
    private final UmbrellaService umbrellaService;

    public RentalController(RentalService rentalService, UmbrellaService umbrellaService) {
        this.rentalService = rentalService;
        this.umbrellaService = umbrellaService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/rent")
    public String rentForm(Model model) {
        model.addAttribute("rentForm", new RentForm());
        model.addAttribute("availableUmbrellas", umbrellaService.findAvailable());
        return "rent";
    }

    @PostMapping("/rent")
    public String rent(@Valid @ModelAttribute RentForm rentForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("availableUmbrellas", umbrellaService.findAvailable());
            return "rent";
        }
        try {
            rentalService.rent(rentForm);
            redirectAttributes.addFlashAttribute("type", "rent");
            redirectAttributes.addFlashAttribute("name", rentForm.getBorrowerName());
            redirectAttributes.addFlashAttribute("umbrellaNumber", rentForm.getUmbrellaNumber());
            return "redirect:/complete";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("availableUmbrellas", umbrellaService.findAvailable());
            return "rent";
        }
    }

    @GetMapping("/return")
    public String returnForm(Model model) {
        model.addAttribute("returnForm", new ReturnForm());
        model.addAttribute("rentedUmbrellas", umbrellaService.findRented());
        return "return";
    }

    @PostMapping("/return")
    public String returnUmbrella(@Valid @ModelAttribute ReturnForm returnForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rentedUmbrellas", umbrellaService.findRented());
            return "return";
        }
        try {
            rentalService.returnUmbrella(returnForm);
            redirectAttributes.addFlashAttribute("type", "return");
            redirectAttributes.addFlashAttribute("name", returnForm.getBorrowerName());
            redirectAttributes.addFlashAttribute("umbrellaNumber", returnForm.getUmbrellaNumber());
            return "redirect:/complete";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("rentedUmbrellas", umbrellaService.findRented());
            return "return";
        }
    }

    @GetMapping("/complete")
    public String complete() {
        return "complete";
    }
}
```

- [ ] **Step 2: 공통 레이아웃 프래그먼트 생성**

`src/main/resources/templates/fragments/layout.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="head(title)">
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title th:text="${title} + ' - 인터페이스 우산 대여'">인터페이스 우산 대여</title>
    <link href="https://cdn.jsdelivr.net/npm/daisyui@4.12.10/dist/full.min.css" rel="stylesheet" type="text/css"/>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
</html>
```

- [ ] **Step 3: 메인 페이지 생성**

`src/main/resources/templates/index.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{fragments/layout :: head('홈')}"></head>
<body class="min-h-screen bg-base-200 flex items-center justify-center">
    <div class="card w-96 bg-base-100 shadow-xl">
        <div class="card-body items-center text-center gap-6">
            <h1 class="card-title text-2xl">☂️ 우산 대여 서비스</h1>
            <p class="text-base-content/70">인터페이스 동아리 무료 우산 대여</p>
            <div class="flex flex-col gap-3 w-full">
                <a th:href="@{/rent}" class="btn btn-primary btn-lg">우산 대여하기</a>
                <a th:href="@{/return}" class="btn btn-outline btn-lg">우산 반납하기</a>
            </div>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 4: 대여 페이지 생성**

`src/main/resources/templates/rent.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{fragments/layout :: head('우산 대여')}"></head>
<body class="min-h-screen bg-base-200 flex items-center justify-center p-4">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
        <div class="card-body">
            <h2 class="card-title text-xl mb-4">우산 대여</h2>

            <div th:if="${errorMessage}" class="alert alert-error mb-4">
                <span th:text="${errorMessage}"></span>
            </div>

            <form th:action="@{/rent}" th:object="${rentForm}" method="post" class="flex flex-col gap-4">
                <div class="form-control">
                    <label class="label"><span class="label-text">이름</span></label>
                    <input type="text" th:field="*{borrowerName}" placeholder="홍길동"
                           class="input input-bordered" th:classappend="${#fields.hasErrors('borrowerName')} ? 'input-error'"/>
                    <span th:if="${#fields.hasErrors('borrowerName')}" th:errors="*{borrowerName}" class="text-error text-sm mt-1"></span>
                </div>

                <div class="form-control">
                    <label class="label"><span class="label-text">전화번호</span></label>
                    <input type="tel" th:field="*{borrowerPhone}" placeholder="01012345678"
                           class="input input-bordered" th:classappend="${#fields.hasErrors('borrowerPhone')} ? 'input-error'"/>
                    <span th:if="${#fields.hasErrors('borrowerPhone')}" th:errors="*{borrowerPhone}" class="text-error text-sm mt-1"></span>
                </div>

                <div class="form-control">
                    <label class="label"><span class="label-text">학번</span></label>
                    <input type="text" th:field="*{borrowerStudentId}" placeholder="20230001"
                           class="input input-bordered" th:classappend="${#fields.hasErrors('borrowerStudentId')} ? 'input-error'"/>
                    <span th:if="${#fields.hasErrors('borrowerStudentId')}" th:errors="*{borrowerStudentId}" class="text-error text-sm mt-1"></span>
                </div>

                <div class="form-control">
                    <label class="label"><span class="label-text">우산 번호</span></label>
                    <select th:field="*{umbrellaNumber}" class="select select-bordered"
                            th:classappend="${#fields.hasErrors('umbrellaNumber')} ? 'select-error'">
                        <option value="">우산을 선택하세요</option>
                        <option th:each="u : ${availableUmbrellas}"
                                th:value="${u.number}"
                                th:text="${u.number} + '번'"></option>
                    </select>
                    <span th:if="${#fields.hasErrors('umbrellaNumber')}" th:errors="*{umbrellaNumber}" class="text-error text-sm mt-1"></span>
                </div>

                <div class="flex gap-2 mt-2">
                    <a th:href="@{/}" class="btn btn-ghost flex-1">돌아가기</a>
                    <button type="submit" class="btn btn-primary flex-1">대여하기</button>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 5: 반납 페이지 생성**

`src/main/resources/templates/return.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{fragments/layout :: head('우산 반납')}"></head>
<body class="min-h-screen bg-base-200 flex items-center justify-center p-4">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
        <div class="card-body">
            <h2 class="card-title text-xl mb-4">우산 반납</h2>

            <div th:if="${errorMessage}" class="alert alert-error mb-4">
                <span th:text="${errorMessage}"></span>
            </div>

            <form th:action="@{/return}" th:object="${returnForm}" method="post" class="flex flex-col gap-4">
                <div class="form-control">
                    <label class="label"><span class="label-text">이름</span></label>
                    <input type="text" th:field="*{borrowerName}" placeholder="홍길동"
                           class="input input-bordered" th:classappend="${#fields.hasErrors('borrowerName')} ? 'input-error'"/>
                    <span th:if="${#fields.hasErrors('borrowerName')}" th:errors="*{borrowerName}" class="text-error text-sm mt-1"></span>
                </div>

                <div class="form-control">
                    <label class="label"><span class="label-text">전화번호</span></label>
                    <input type="tel" th:field="*{borrowerPhone}" placeholder="01012345678"
                           class="input input-bordered" th:classappend="${#fields.hasErrors('borrowerPhone')} ? 'input-error'"/>
                    <span th:if="${#fields.hasErrors('borrowerPhone')}" th:errors="*{borrowerPhone}" class="text-error text-sm mt-1"></span>
                </div>

                <div class="form-control">
                    <label class="label"><span class="label-text">우산 번호</span></label>
                    <select th:field="*{umbrellaNumber}" class="select select-bordered"
                            th:classappend="${#fields.hasErrors('umbrellaNumber')} ? 'select-error'">
                        <option value="">우산을 선택하세요</option>
                        <option th:each="u : ${rentedUmbrellas}"
                                th:value="${u.number}"
                                th:text="${u.number} + '번'"></option>
                    </select>
                    <span th:if="${#fields.hasErrors('umbrellaNumber')}" th:errors="*{umbrellaNumber}" class="text-error text-sm mt-1"></span>
                </div>

                <div class="flex gap-2 mt-2">
                    <a th:href="@{/}" class="btn btn-ghost flex-1">돌아가기</a>
                    <button type="submit" class="btn btn-primary flex-1">반납하기</button>
                </div>
            </form>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 6: 완료 페이지 생성**

`src/main/resources/templates/complete.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{fragments/layout :: head('완료')}"></head>
<body class="min-h-screen bg-base-200 flex items-center justify-center p-4">
    <div class="card w-full max-w-md bg-base-100 shadow-xl">
        <div class="card-body items-center text-center gap-4">
            <div th:if="${type == 'rent'}">
                <div class="text-5xl mb-2">✅</div>
                <h2 class="card-title text-xl">대여 완료!</h2>
                <p class="text-base-content/70">
                    <span th:text="${name}"></span>님,
                    <span th:text="${umbrellaNumber}"></span>번 우산을 대여했습니다.
                </p>
                <p class="text-sm text-warning">반납 기한을 지켜주세요. 다음날부터 알림이 발송됩니다.</p>
            </div>
            <div th:if="${type == 'return'}">
                <div class="text-5xl mb-2">🎉</div>
                <h2 class="card-title text-xl">반납 완료!</h2>
                <p class="text-base-content/70">
                    <span th:text="${name}"></span>님,
                    <span th:text="${umbrellaNumber}"></span>번 우산을 반납했습니다.
                </p>
            </div>
            <div th:if="${type == null}">
                <h2 class="card-title text-xl">처리 완료</h2>
            </div>
            <a th:href="@{/}" class="btn btn-primary w-full mt-2">처음으로</a>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 7: RentalControllerTest 작성**

```java
package com.chuseok22.umbrellareturn.controller;

import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RentalController.class)
class RentalControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RentalService rentalService;
    @MockitoBean UmbrellaService umbrellaService;

    @Test
    void 메인_페이지_접근() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"));
    }

    @Test
    void 대여_폼_접근() throws Exception {
        given(umbrellaService.findAvailable()).willReturn(List.of());

        mockMvc.perform(get("/rent"))
            .andExpect(status().isOk())
            .andExpect(view().name("rent"))
            .andExpect(model().attributeExists("rentForm", "availableUmbrellas"));
    }

    @Test
    void 반납_폼_접근() throws Exception {
        given(umbrellaService.findRented()).willReturn(List.of());

        mockMvc.perform(get("/return"))
            .andExpect(status().isOk())
            .andExpect(view().name("return"))
            .andExpect(model().attributeExists("returnForm", "rentedUmbrellas"));
    }
}
```

- [ ] **Step 8: 테스트 실행**

```bash
./gradlew test --tests "com.chuseok22.umbrellareturn.controller.RentalControllerTest"
```

Expected: 3 tests PASSED

- [ ] **Step 9: 커밋**

```bash
git add src/main/java/com/chuseok22/umbrellareturn/controller/RentalController.java src/main/resources/templates/ src/test/java/com/chuseok22/umbrellareturn/controller/RentalControllerTest.java
git commit -m "feat: add user-facing rental/return controller and thymeleaf templates"
```

---

## Task 6: 관리자 - 우산 관리 + 공통 레이아웃

**Files:**
- Create: `src/main/resources/templates/admin/layout.html`
- Create: `src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminUmbrellaController.java`
- Create: `src/main/resources/templates/admin/login.html`
- Create: `src/main/resources/templates/admin/umbrellas.html`
- Create: `src/test/java/com/chuseok22/umbrellareturn/controller/admin/AdminUmbrellaControllerTest.java`

- [ ] **Step 1: 관리자 공통 레이아웃 생성**

`src/main/resources/templates/admin/layout.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security" lang="ko">
<head th:fragment="head(title)">
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title th:text="${title} + ' - 관리자'">관리자</title>
    <link href="https://cdn.jsdelivr.net/npm/daisyui@4.12.10/dist/full.min.css" rel="stylesheet" type="text/css"/>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<nav th:fragment="navbar" class="navbar bg-base-100 shadow-md">
    <div class="navbar-start">
        <a th:href="@{/admin}" class="btn btn-ghost text-xl">☂️ 관리자</a>
    </div>
    <div class="navbar-center hidden lg:flex">
        <ul class="menu menu-horizontal px-1">
            <li><a th:href="@{/admin/rentals}">대여 현황</a></li>
            <li><a th:href="@{/admin/history}">이력 조회</a></li>
            <li><a th:href="@{/admin/umbrellas}">우산 관리</a></li>
            <li><a th:href="@{/admin/sms}">SMS 관리</a></li>
        </ul>
    </div>
    <div class="navbar-end">
        <form th:action="@{/admin/logout}" method="post">
            <button type="submit" class="btn btn-ghost btn-sm">로그아웃</button>
        </form>
    </div>
</nav>
</html>
```

- [ ] **Step 2: 관리자 로그인 페이지 생성**

`src/main/resources/templates/admin/login.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{fragments/layout :: head('관리자 로그인')}"></head>
<body class="min-h-screen bg-base-200 flex items-center justify-center">
    <div class="card w-96 bg-base-100 shadow-xl">
        <div class="card-body">
            <h2 class="card-title text-xl mb-4">관리자 로그인</h2>

            <div th:if="${param.error}" class="alert alert-error mb-4">
                <span>아이디 또는 비밀번호가 올바르지 않습니다.</span>
            </div>
            <div th:if="${param.logout}" class="alert alert-success mb-4">
                <span>로그아웃 되었습니다.</span>
            </div>

            <form th:action="@{/admin/login}" method="post" class="flex flex-col gap-4">
                <div class="form-control">
                    <label class="label"><span class="label-text">아이디</span></label>
                    <input type="text" name="username" class="input input-bordered" autocomplete="username"/>
                </div>
                <div class="form-control">
                    <label class="label"><span class="label-text">비밀번호</span></label>
                    <input type="password" name="password" class="input input-bordered" autocomplete="current-password"/>
                </div>
                <button type="submit" class="btn btn-primary mt-2">로그인</button>
            </form>
        </div>
    </div>
</body>
</html>
```

- [ ] **Step 3: AdminUmbrellaController 생성**

```java
package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.dto.UmbrellaForm;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/umbrellas")
public class AdminUmbrellaController {

    private final UmbrellaService umbrellaService;

    public AdminUmbrellaController(UmbrellaService umbrellaService) {
        this.umbrellaService = umbrellaService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("umbrellas", umbrellaService.findAll());
        model.addAttribute("umbrellaForm", new UmbrellaForm());
        return "admin/umbrellas";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute UmbrellaForm umbrellaForm,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("umbrellas", umbrellaService.findAll());
            return "admin/umbrellas";
        }
        try {
            umbrellaService.register(umbrellaForm.getNumber());
            redirectAttributes.addFlashAttribute("successMessage", umbrellaForm.getNumber() + "번 우산이 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/umbrellas";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            umbrellaService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "우산이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/umbrellas";
    }
}
```

- [ ] **Step 4: 우산 관리 템플릿 생성**

`src/main/resources/templates/admin/umbrellas.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{admin/layout :: head('우산 관리')}"></head>
<body class="bg-base-200 min-h-screen">
<nav th:replace="~{admin/layout :: navbar}"></nav>

<main class="container mx-auto px-4 py-8 max-w-4xl">
    <h1 class="text-2xl font-bold mb-6">우산 관리</h1>

    <div th:if="${successMessage}" class="alert alert-success mb-4">
        <span th:text="${successMessage}"></span>
    </div>
    <div th:if="${errorMessage}" class="alert alert-error mb-4">
        <span th:text="${errorMessage}"></span>
    </div>

    <!-- 우산 등록 폼 -->
    <div class="card bg-base-100 shadow mb-6">
        <div class="card-body">
            <h2 class="card-title text-lg">우산 등록</h2>
            <form th:action="@{/admin/umbrellas}" th:object="${umbrellaForm}" method="post" class="flex gap-2">
                <input type="text" th:field="*{number}" placeholder="우산 번호 (예: 001)"
                       class="input input-bordered flex-1" th:classappend="${#fields.hasErrors('number')} ? 'input-error'"/>
                <button type="submit" class="btn btn-primary">등록</button>
            </form>
            <span th:if="${#fields.hasErrors('number')}" th:errors="*{number}" class="text-error text-sm"></span>
        </div>
    </div>

    <!-- 우산 목록 -->
    <div class="card bg-base-100 shadow">
        <div class="card-body">
            <h2 class="card-title text-lg">등록된 우산 (<span th:text="${umbrellas.size()}"></span>개)</h2>
            <div class="overflow-x-auto">
                <table class="table">
                    <thead>
                        <tr>
                            <th>우산 번호</th>
                            <th>상태</th>
                            <th>등록일시</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr th:each="u : ${umbrellas}">
                            <td th:text="${u.number}"></td>
                            <td>
                                <span th:if="${u.status.name() == 'AVAILABLE'}" class="badge badge-success">대여 가능</span>
                                <span th:if="${u.status.name() == 'RENTED'}" class="badge badge-warning">대여 중</span>
                            </td>
                            <td th:text="${#temporals.format(u.createdAt, 'yyyy-MM-dd HH:mm')}"></td>
                            <td>
                                <form th:if="${u.status.name() == 'AVAILABLE'}"
                                      th:action="@{/admin/umbrellas/{id}/delete(id=${u.id})}" method="post"
                                      onsubmit="return confirm('삭제하시겠습니까?')">
                                    <button type="submit" class="btn btn-error btn-xs">삭제</button>
                                </form>
                            </td>
                        </tr>
                        <tr th:if="${umbrellas.isEmpty()}">
                            <td colspan="4" class="text-center text-base-content/50">등록된 우산이 없습니다.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</main>
</body>
</html>
```

- [ ] **Step 5: AdminUmbrellaControllerTest 작성**

```java
package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.service.UmbrellaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUmbrellaController.class)
class AdminUmbrellaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UmbrellaService umbrellaService;

    @Test
    void 인증없이_접근_시_로그인_리다이렉트() throws Exception {
        mockMvc.perform(get("/admin/umbrellas"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void 인증_후_우산_목록_접근() throws Exception {
        given(umbrellaService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/umbrellas").with(user("chuseok22").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/umbrellas"))
            .andExpect(model().attributeExists("umbrellas", "umbrellaForm"));
    }
}
```

- [ ] **Step 6: 테스트 실행**

```bash
./gradlew test --tests "com.chuseok22.umbrellareturn.controller.admin.AdminUmbrellaControllerTest"
```

Expected: 2 tests PASSED

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminUmbrellaController.java src/main/resources/templates/admin/ src/test/java/com/chuseok22/umbrellareturn/controller/admin/AdminUmbrellaControllerTest.java
git commit -m "feat: add admin umbrella management controller and templates"
```

---

## Task 7: 관리자 - 대시보드 + 대여 관리

**Files:**
- Create: `src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminDashboardController.java`
- Create: `src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminRentalController.java`
- Create: `src/main/resources/templates/admin/dashboard.html`
- Create: `src/main/resources/templates/admin/rentals.html`
- Create: `src/main/resources/templates/admin/history.html`
- Create: `src/test/java/com/chuseok22/umbrellareturn/controller/admin/AdminRentalControllerTest.java`

- [ ] **Step 1: AdminDashboardController 생성**

```java
package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UmbrellaService umbrellaService;
    private final RentalService rentalService;

    public AdminDashboardController(UmbrellaService umbrellaService, RentalService rentalService) {
        this.umbrellaService = umbrellaService;
        this.rentalService = rentalService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUmbrellas", umbrellaService.countTotal());
        model.addAttribute("availableUmbrellas", umbrellaService.countAvailable());
        model.addAttribute("activeRentals", rentalService.countActive());
        model.addAttribute("recentRentals", rentalService.findActiveRentals());
        return "admin/dashboard";
    }
}
```

- [ ] **Step 2: AdminRentalController 생성**

```java
package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.service.RentalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rentals")
public class AdminRentalController {

    private final RentalService rentalService;

    public AdminRentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rentals", rentalService.findActiveRentals());
        return "admin/rentals";
    }

    @PostMapping("/{id}/return")
    public String adminReturn(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rentalService.adminReturn(id);
            redirectAttributes.addFlashAttribute("successMessage", "반납 처리되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/rentals";
    }
}
```

- [ ] **Step 3: AdminDashboardController에 이력 엔드포인트 추가**

`AdminDashboardController`는 `@RequestMapping("/admin")`이므로 `@GetMapping("/history")`를 추가하면 `/admin/history`로 매핑된다.

`AdminDashboardController.java`의 `dashboard()` 메서드 아래에 추가:

```java
@GetMapping("/history")
public String history(Model model) {
    model.addAttribute("rentals", rentalService.findAllRentals());
    return "admin/history";
}
```

- [ ] **Step 4: 대시보드 템플릿 생성**

`src/main/resources/templates/admin/dashboard.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{admin/layout :: head('대시보드')}"></head>
<body class="bg-base-200 min-h-screen">
<nav th:replace="~{admin/layout :: navbar}"></nav>

<main class="container mx-auto px-4 py-8 max-w-5xl">
    <h1 class="text-2xl font-bold mb-6">대시보드</h1>

    <!-- 요약 카드 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div class="stat bg-base-100 rounded-box shadow">
            <div class="stat-title">전체 우산</div>
            <div class="stat-value" th:text="${totalUmbrellas}">0</div>
        </div>
        <div class="stat bg-base-100 rounded-box shadow">
            <div class="stat-title">대여 중</div>
            <div class="stat-value text-warning" th:text="${activeRentals}">0</div>
        </div>
        <div class="stat bg-base-100 rounded-box shadow">
            <div class="stat-title">대여 가능</div>
            <div class="stat-value text-success" th:text="${availableUmbrellas}">0</div>
        </div>
    </div>

    <!-- 현재 대여 목록 -->
    <div class="card bg-base-100 shadow">
        <div class="card-body">
            <h2 class="card-title">현재 대여 현황</h2>
            <div class="overflow-x-auto">
                <table class="table">
                    <thead>
                        <tr><th>이름</th><th>학번</th><th>전화번호</th><th>우산 번호</th><th>대여 일시</th></tr>
                    </thead>
                    <tbody>
                        <tr th:each="r : ${recentRentals}">
                            <td th:text="${r.borrowerName}"></td>
                            <td th:text="${r.borrowerStudentId}"></td>
                            <td th:text="${r.borrowerPhone}"></td>
                            <td th:text="${r.umbrella.number}"></td>
                            <td th:text="${#temporals.format(r.rentedAt, 'MM-dd HH:mm')}"></td>
                        </tr>
                        <tr th:if="${recentRentals.isEmpty()}">
                            <td colspan="5" class="text-center text-base-content/50">대여 중인 우산이 없습니다.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</main>
</body>
</html>
```

- [ ] **Step 5: 대여 현황 템플릿 생성**

`src/main/resources/templates/admin/rentals.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{admin/layout :: head('대여 현황')}"></head>
<body class="bg-base-200 min-h-screen">
<nav th:replace="~{admin/layout :: navbar}"></nav>

<main class="container mx-auto px-4 py-8 max-w-5xl">
    <h1 class="text-2xl font-bold mb-6">대여 현황</h1>

    <div th:if="${successMessage}" class="alert alert-success mb-4">
        <span th:text="${successMessage}"></span>
    </div>
    <div th:if="${errorMessage}" class="alert alert-error mb-4">
        <span th:text="${errorMessage}"></span>
    </div>

    <div class="card bg-base-100 shadow">
        <div class="card-body">
            <div class="overflow-x-auto">
                <table class="table">
                    <thead>
                        <tr><th>이름</th><th>학번</th><th>전화번호</th><th>우산 번호</th><th>대여 일시</th><th></th></tr>
                    </thead>
                    <tbody>
                        <tr th:each="r : ${rentals}">
                            <td th:text="${r.borrowerName}"></td>
                            <td th:text="${r.borrowerStudentId}"></td>
                            <td th:text="${r.borrowerPhone}"></td>
                            <td th:text="${r.umbrella.number}"></td>
                            <td th:text="${#temporals.format(r.rentedAt, 'yyyy-MM-dd HH:mm')}"></td>
                            <td>
                                <form th:action="@{/admin/rentals/{id}/return(id=${r.id})}" method="post"
                                      onsubmit="return confirm('반납 처리하시겠습니까?')">
                                    <button type="submit" class="btn btn-success btn-xs">반납 처리</button>
                                </form>
                            </td>
                        </tr>
                        <tr th:if="${rentals.isEmpty()}">
                            <td colspan="6" class="text-center text-base-content/50">대여 중인 우산이 없습니다.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</main>
</body>
</html>
```

- [ ] **Step 6: 이력 템플릿 생성**

`src/main/resources/templates/admin/history.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{admin/layout :: head('대여 이력')}"></head>
<body class="bg-base-200 min-h-screen">
<nav th:replace="~{admin/layout :: navbar}"></nav>

<main class="container mx-auto px-4 py-8 max-w-5xl">
    <h1 class="text-2xl font-bold mb-6">대여/반납 이력</h1>

    <div class="card bg-base-100 shadow">
        <div class="card-body">
            <div class="overflow-x-auto">
                <table class="table">
                    <thead>
                        <tr><th>이름</th><th>학번</th><th>전화번호</th><th>우산 번호</th><th>대여 일시</th><th>반납 일시</th><th>상태</th></tr>
                    </thead>
                    <tbody>
                        <tr th:each="r : ${rentals}">
                            <td th:text="${r.borrowerName}"></td>
                            <td th:text="${r.borrowerStudentId}"></td>
                            <td th:text="${r.borrowerPhone}"></td>
                            <td th:text="${r.umbrella.number}"></td>
                            <td th:text="${#temporals.format(r.rentedAt, 'yyyy-MM-dd HH:mm')}"></td>
                            <td th:text="${r.returnedAt != null ? #temporals.format(r.returnedAt, 'yyyy-MM-dd HH:mm') : '-'}"></td>
                            <td>
                                <span th:if="${r.status.name() == 'RENTED'}" class="badge badge-warning">대여 중</span>
                                <span th:if="${r.status.name() == 'RETURNED'}" class="badge badge-success">반납 완료</span>
                            </td>
                        </tr>
                        <tr th:if="${rentals.isEmpty()}">
                            <td colspan="7" class="text-center text-base-content/50">이력이 없습니다.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</main>
</body>
</html>
```

- [ ] **Step 7: AdminRentalControllerTest 작성**

```java
package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.service.RentalService;
import com.chuseok22.umbrellareturn.service.UmbrellaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminDashboardController.class, AdminRentalController.class})
class AdminRentalControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RentalService rentalService;
    @MockitoBean UmbrellaService umbrellaService;

    @Test
    void 대시보드_접근() throws Exception {
        given(umbrellaService.countTotal()).willReturn(5L);
        given(umbrellaService.countAvailable()).willReturn(3L);
        given(rentalService.countActive()).willReturn(2L);
        given(rentalService.findActiveRentals()).willReturn(List.of());

        mockMvc.perform(get("/admin").with(user("chuseok22").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard"));
    }

    @Test
    void 대여_현황_접근() throws Exception {
        given(rentalService.findActiveRentals()).willReturn(List.of());

        mockMvc.perform(get("/admin/rentals").with(user("chuseok22").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/rentals"));
    }
}
```

- [ ] **Step 8: 테스트 실행**

```bash
./gradlew test --tests "com.chuseok22.umbrellareturn.controller.admin.AdminRentalControllerTest"
```

Expected: 2 tests PASSED

- [ ] **Step 9: 커밋**

```bash
git add src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminDashboardController.java src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminRentalController.java src/main/resources/templates/admin/dashboard.html src/main/resources/templates/admin/rentals.html src/main/resources/templates/admin/history.html src/test/java/com/chuseok22/umbrellareturn/controller/admin/AdminRentalControllerTest.java
git commit -m "feat: add admin dashboard and rental management controller/templates"
```

---

## Task 8: 관리자 - SMS 관리

**Files:**
- Create: `src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminSmsController.java`
- Create: `src/main/resources/templates/admin/sms.html`

- [ ] **Step 1: AdminSmsController 생성**

```java
package com.chuseok22.umbrellareturn.controller.admin;

import com.chuseok22.umbrellareturn.service.SmsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/sms")
public class AdminSmsController {

    private final SmsService smsService;

    public AdminSmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("smsLogs", smsService.findAllLogs());
        return "admin/sms";
    }

    @PostMapping("/send")
    public String sendManual(RedirectAttributes redirectAttributes) {
        try {
            int count = smsService.sendReminderToAll();
            redirectAttributes.addFlashAttribute("successMessage",
                count + "명에게 SMS가 발송되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "SMS 발송 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/sms";
    }
}
```

- [ ] **Step 2: SMS 관리 템플릿 생성**

`src/main/resources/templates/admin/sms.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head th:replace="~{admin/layout :: head('SMS 관리')}"></head>
<body class="bg-base-200 min-h-screen">
<nav th:replace="~{admin/layout :: navbar}"></nav>

<main class="container mx-auto px-4 py-8 max-w-5xl">
    <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold">SMS 관리</h1>
        <form th:action="@{/admin/sms/send}" method="post"
              onsubmit="return confirm('현재 미반납자 전체에게 SMS를 발송하시겠습니까?')">
            <button type="submit" class="btn btn-warning">수동 발송</button>
        </form>
    </div>

    <div th:if="${successMessage}" class="alert alert-success mb-4">
        <span th:text="${successMessage}"></span>
    </div>
    <div th:if="${errorMessage}" class="alert alert-error mb-4">
        <span th:text="${errorMessage}"></span>
    </div>

    <div class="card bg-base-100 shadow">
        <div class="card-body">
            <h2 class="card-title text-lg">발송 내역</h2>
            <div class="overflow-x-auto">
                <table class="table">
                    <thead>
                        <tr><th>수신자</th><th>메시지</th><th>발송 일시</th><th>결과</th></tr>
                    </thead>
                    <tbody>
                        <tr th:each="log : ${smsLogs}">
                            <td th:text="${log.phone}"></td>
                            <td class="max-w-xs truncate" th:text="${log.message}"></td>
                            <td th:text="${#temporals.format(log.sentAt, 'yyyy-MM-dd HH:mm')}"></td>
                            <td>
                                <span th:if="${log.success}" class="badge badge-success">성공</span>
                                <span th:unless="${log.success}" class="badge badge-error">실패</span>
                            </td>
                        </tr>
                        <tr th:if="${smsLogs.isEmpty()}">
                            <td colspan="4" class="text-center text-base-content/50">발송 내역이 없습니다.</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</main>
</body>
</html>
```

- [ ] **Step 3: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: 모든 테스트 PASSED, BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/com/chuseok22/umbrellareturn/controller/admin/AdminSmsController.java src/main/resources/templates/admin/sms.html
git commit -m "feat: add admin sms management controller and template"
```

---

## 완료 기준

- [ ] 전체 테스트 통과: `./gradlew test`
- [ ] 빌드 성공: `./gradlew build`
- [ ] 사용자 대여 플로우: `/` → `/rent` → POST → `/complete`
- [ ] 사용자 반납 플로우: `/` → `/return` → POST → `/complete`
- [ ] 관리자 로그인: `/admin/login` (chuseok22/interface518)
- [ ] 관리자 우산 등록/삭제
- [ ] 관리자 반납 처리
- [ ] 관리자 SMS 수동 발송
- [ ] 스케줄러: 매일 07:00 자동 발송 (로그 확인)
