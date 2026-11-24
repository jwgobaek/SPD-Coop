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
