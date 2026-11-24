# 작업 시작 순서

AGENTS.md → PROGRESS.md → PLANS.md → KNOWN_ISSUES.md → TEST_REPORT.md → git status 순으로 확인하고 이전 완료 조건을 점검한다. 사용자 변경을 보존하고 다음 미완료 단계부터 작업한다.

Java/libGDX 및 Wrapper를 유지한다. 변경 전 기준 테스트를 확보하고 작은 단위로 검증한다. iPad/Cloud만으로 수행하며 로컬 PC, ADB, 에뮬레이터를 요구하지 않는다. 단일 영웅 포인터를 바꿔 끼우거나 테스트 전용 엔진을 만들지 않는다. 실제 원본 코드 경로와 협동 저장 영역을 검증한다.

요구사항은 GOALS.md, 실행 경계는 ARCHITECTURE.md, 미정 규칙은 DECISIONS.md를 따른다. 작업 종료 시 진행 상황·실제 명령과 결과·알려진 문제를 갱신한다. PASS/FAIL/NOT RUN을 구별한다. APK·서명 비밀은 Git에 넣지 않는다. 외부 공개 릴리스에는 명시적 승인이 필요하다.

LAN 변경은 docs/coop/lan-protocol.md와 실제 프레임을 함께 갱신한다. 수신 스레드에서 Dungeon/CoopSession을 호출하지 않는다. 독립 JVM 시나리오의 좌석별 적용 횟수와 실제 호스트 상태를 함께 검증한다.

SYNC1에서 새 상태를 공개할 때는 SeatProjection의 좌석별 필터, 원본 직접 시야, StateImage 정규화와 DisplayReplica의 원자적 적용을 함께 검증한다. 행동 결과 조회 전에 연결·좌석을 인증한다. 상세 형식은 docs/coop/state-sync.md에 있다.

협동 규칙을 수정할 때는 두 좌석의 직접 시야·기억·소유권과 사망 상태를 함께 검사한다. 구역 계산은 Actor나 RNG를 변경하지 않는다. 부활은 앙크 소유자와 원래 Actor ID를 보존하고 확정된 행동의 재전송을 검증한다. 현재 지원 범위와 Phase 7 저장 경계는 ARCHITECTURE.md 및 docs/coop/state-sync.md를 참조한다.

협동 저장 변경은 docs/coop/save-format.md와 docs/coop/reconnect.md를 함께 확인한다. 영수증과 엔진 이미지를 같은 세대에 발행한 뒤 성공을 반환한다. 복원 시험은 기존 호스트와 클라이언트를 종료하고 새 프로세스에서 수행한다. 개인 키, 세션 파일, 키가 들어가는 프로세스 입력은 검증 ZIP에 넣지 않는다.

## Phase 8 협동 알파

협동 화면은 UI-ARCHITECTURE.md의 Frame과 PublicDungeon만 읽는다. 원본 단일 엔진과 렌더 스레드의 RNG 및 음향을 격리한다. 행동·좌석 키·저장 세대의 내구성을 유지하고, Desktop 화면 검증과 Android 실기기 검증을 따로 기록한다.

새 하수도 콘텐츠는 기존 마력탄·층 전환·레거시 저장 회귀를 유지한다. 비활성 층의 Actor를 등록하지 않는다. 층 변경은 좌석별 전체 스냅샷으로 공개하고, 미지원 목적지와 실제 저장 실패를 구분한다.

하수도 구역 작업은 docs/coop/sewer-region.md와 생성 조사표를 기준으로 이어간다. 생성 조사, 교전 위치를 준비한 시험, 원본 층에서의 정상 진행 기록을 각각 구분한다.

## 하수도 회귀

미믹·석상 변경 시 위장과 몬스터 내부 소유권까지 대조한다. 새 조합을 허용하기 전 양 좌석의 원본 효과와 휴면 층 복원을 검증한다. 시드별 시험에서 계단 배치와 원본 시작 위치 경로를 구분한다. [구역 검증](docs/coop/sewer-region.md)
