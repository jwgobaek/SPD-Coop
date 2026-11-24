# 2026-11-24 — Phase 0 / Phase 1

시작 시 /workspace에는 연결된 Git checkout, 프로젝트 문서, Android SDK, javac, 시스템 Gradle이 없었다. Java 21 실행기와 gh는 있었다. 기존 디렉터리를 보존하고 spd-coop에 공식 태그를 shallow clone했다. 공식 ls-remote 결과와 로컬 tag^{commit}, HEAD가 모두 57a4e06a4caf162446d1c28caa7983f0493fecf0이다. 개발 브랜치는 coop/baseline이다.

- Phase 0 클라우드 기준 충족. 소스 변경 전 원본 APK와 독립 ID의 개발 기준 APK를 각각 생성하고 검사했다. CI 정의와 빌드 절차 작성 완료. GitHub 저장과 실제 Actions 실행은 권한 연결 대기.
- Phase 1 분석 기준 충족. ARCHITECTURE.md에 주요 실행 경로, 전역 상태, 실제 수정 지점과 Headless 제약을 정리했다. docs/coop/dependencies.tsv는 전체 Java 소스 어휘 색인이다. 게임 Java 클래스나 메서드 수정 없음.
- 다음 단계는 PLANS.md의 원본 JVM 회귀 데이터 수집이다. 이 기준 APK에서 제공하는 게임 모드는 원본 싱글플레이다.

## Phase 2 첫 Headless 회귀

사용자 저장소 jwgobaek/SPD-Coop를 remote coop으로 연결했다. master가 공식 기준 커밋과 일치하며 gh api user로 jwgobaek 인증에 성공했다. coop/baseline push는 403으로 거부되어 로컬 coop/phase2에서 계속했다. 기준 태그의 별도 checkout과 작업 checkout의 JVM에서 전사 시드 402의 첫 하수도 층을 실제 생성했다. 원본 지형 해시 비교, 층 진입, 저장된 층 지형 복원 테스트를 통과했다. 다음은 몹·아이템/장비와 전체 영웅 상태 비교 및 행동 완료 신호의 회귀 테스트다.

## Phase 2 엔진 기반 검증 완료 — 2026-11-24

coop/baseline과 coop/phase2를 사용자 GitHub 저장소에 push했다. 초안 PR #1에서 검토할 수 있다. 엔진/테스트 코드 커밋 f96289c. 9건의 Headless 회귀 및 독립 Android APK 검사를 통과했다. 실제 actor 스레드의 진행과 종료를 직접 검증할 수 있어 스케줄러 메서드 추출이 필요하지 않았다. 테스트에서 스프라이트의 시각적 완료만 제어한다. 상세 범위는 TEST_REPORT.md 참조.

GitHub 쪽 실행은 0건이다. 공식 문서상 push는 기본 브랜치 병합 전에 가능하다. 포크 활성 상태는 관리 API 접근 제한으로 미확인이다. 동일 검증 스크립트로 Codex Cloud 빌드를 제공했다. Phase 3은 별도 작업에서 이 회귀 기준 위에 오프라인 2인 세션 도입으로 시작한다.

## Phase 3 핵심 클라우드 검증 완료 — 2026-11-24

시작 시 원격 coop/phase2는 61deb84였고 기존 PR #1은 master 대상 초안이었다. 수정 전 작업 트리는 깨끗했다. coop/phase3에서 실제 1층 월드·두 Hero·단일 Actor 스케줄러·기본 근접전과 소유자별 가방/장비를 구현했다. 엔진과 테스트 커밋 8207671d628240d2310f230503ca8ec52d865d53을 사용자 저장소의 새 브랜치로 일반 푸시했다.

- 기존 9건과 협동 10건, 빌드 후 협동 10건 × 3회 모두 PASS. 공식 Android 빌드 스크립트가 생성한 디버그 APK의 독립 ID·서명·체크섬을 확인했다. 상세 명령과 지원 경계는 TEST_REPORT.md와 ARCHITECTURE.md에 있다.
- 검증 범위에서 Phase 3의 핵심 기준을 충족해 Phase 4에 진입할 수 있다. 다음 작업은 기존 엔진의 좌석/턴 입력을 실제 LAN 호스트와 별도 클라이언트 프로세스의 로비에 연결하는 것이다. 저장과 재접속, Android 협동 UI, 전체 콘텐츠는 이후 단계다.
- GitHub 호스팅 실행과 실기기 검증은 NOT RUN. 빌드·테스트 ZIP은 docs/coop/phase3-cloud-evidence.zip에 보존했다. APK는 Cloud 결과물이다.

## Phase 4 핵심 클라우드 검증 완료 — 2026-11-24

검증 대상 커밋 5aac3b9를 사용자 저장소의 coop/phase4에 일반 푸시했다. 원본 엔진을 유지하면서 실제 UDP/TCP, 로비 상태 전이, 연결 기준 권한 검사, 같은 엔진 스레드에서의 실제 행동 실행과 단절 처리를 추가했다. 기존 19건과 신규 네트워크 6건이 통과했다. 하나의 독립 JVM 시나리오에서 게스트 참가·종료 50회와 두 좌석의 유효 행동 310회를 확인했다.

APK 패키지·서명·체크섬과 증거 ZIP 검증을 완료했다. 소스·관리 문서·ZIP은 Git에 보존하고 APK는 Cloud 결과물로 제공한다. 원격 Actions와 Android 실기기 LAN은 NOT RUN이다. 다음 단계는 별도 브랜치에서 상태 버전·행동 결과 캐시·관찰자별 스냅샷을 구현하는 것이다. 실행 경계와 전이는 docs/coop/lan-protocol.md, 검증 상세는 TEST_REPORT.md를 따른다.

## Phase 5 핵심 클라우드 검증 완료 — 2026-11-24

원격 coop/phase4의 757a4a7에서 분기했다. 엔진·APK 검증 커밋 18700148a3770cc050087d72c14e39a06e342281을 coop/phase5로 일반 푸시했다. SYNC1 방에서 좌석별 행동 ID·확정 결과 캐시·상태 버전·직접 시야 공개 모델·변경분과 전체 재동기화를 구현했다. 원본 행동 규칙과 기존 v1 회귀를 유지했다.

최종 실행에서 기존 25건과 신규 4건이 통과했다. SYNC1 정상 확정 1,343회, 캐시 재응답 78건, 전체 재동기화 6회다. 증거 ZIP과 소스는 Git에 보존하고 Android 디버그 APK는 Cloud 결과물로 제공한다. Actions와 실기기는 NOT RUN이다. 새 작업은 PLANS.md의 Phase 6 순서에 따라 탐험 기억, 전투 구역, 소유권 및 제한된 부활 규칙을 확장한다.

## Phase 6 핵심 클라우드 검증 완료 — 2026-11-24

시작 시 로컬과 원격 coop/phase5가 d86d21ffb8ab2203077dc6bf86e7a0efc711bc19에 일치했고 작업 트리는 깨끗했다. 새 coop/phase6에서 코드 변경 전 기존 29건을 모두 재실행했다.

| 단계 | Git 체크포인트 | 구현 및 검증 |
|---|---|---|
| 6A | fd6225d | 좌석·층별 탐험과 시야 밖 지형 기억, 비밀 지형 가림, 재관측 및 스냅샷 복원 |
| 6B | f34d411 | 교전 대상과 통행 경로로 만든 구역의 병합·분리, 좌석별 경계와 스케줄러 불변성 |
| 6C | 90d0359 | 호스트 내부 아이템 ID, 가방·바닥·장비의 귀속, 분할·병합과 타인 획득 차단 |
| 6D | 4b5769c | 치명 피해 직전 HP 기록, 예약 셀·동결된 버프와 Actor, 동료 앙크 부활, 파티 전멸 |
| 통합 | 53770eabfd0c40ead0d4d1848ddf480c2743930b | Foresight 좌석 격리, 숨은 더미 제외, SYNC1 독립 JVM 검증 및 최종 APK |

최종 JUnit 34건과 APK 검사가 통과했다. Phase 6 전용 시나리오에서 3개 시드와 두 직업 배치로 유효 행동 1,023회, 부활 3회, 재동기화 3회 및 파티 전멸 3회를 확인했다. 엔진 커밋은 원격 coop/phase6에서 직접 확인했다. GitHub 호스팅 실행은 0건이다. 소스·문서·검증 ZIP은 Git, 디버그 APK는 Cloud 결과물이다. 다음 구현은 PLANS.md의 Phase 7 호스트 저장과 재접속이다.

[실행 결과](TEST_REPORT.md) · [검증 ZIP](docs/coop/phase6-cloud-evidence.zip)

## Phase 7 클라우드 검증 결과

원격 coop/phase6의 015f299078a5a4ff4da6e8b2ad630ac7e0916c0f와 이전 검증 커밋 53770eabfd0c40ead0d4d1848ddf480c2743930b를 확인했다. 작업 트리는 깨끗했고 새 coop/phase7 브랜치에 변경을 쌓았다. 수정 전 기존 34건을 다시 실행했다.

| 단계 | Git 체크포인트 | 검증 결과 |
|---|---|---|
| 7A | c35cb74 | 두 영웅과 실제 Level, 몬스터 표적, Actor 시계, 탐험·귀속·사망 상태 및 RNG 복원 |
| 7B | 7f926dd | 독립 파일 잠금, 체크섬·세대 식별자, 동기화와 원자적 HEAD 발행, 손상 및 강제 종료 검증 |
| 7C | bd37678 | 잠긴 좌석의 연결별 서명 인증, 양쪽 모델 확인, 행동·영수증 동시 발행 |
| 7D | 32b9abd | 새 호스트·클라이언트 JVM에서 재개, 부활 응답 유실, 저장 장애 및 롤백 epoch 통합 |
| 최종 | da173259ca546d1e289197d4336a4e5421c83c10 | 승격할 백업 무결성 재검사, 플레이어별 성장·강화·버프와 중첩 RNG 스트림 확인, 전체 APK 검증 |

최종 JUnit 44건 PASS. 호스트 재시작 시나리오에서 확정된 행동 1,021건, 새 연결 좌석 인증 54건을 확인했다. GitHub에는 소스·명세·검증 ZIP을 보존하고 개발 APK는 Cloud 결과물로 제공한다. 다음 개발 단계는 PLANS.md의 Phase 8 UI·콘텐츠 통합이다.

[실행 결과](TEST_REPORT.md) · [검증 ZIP](docs/coop/phase7-cloud-evidence.zip)
