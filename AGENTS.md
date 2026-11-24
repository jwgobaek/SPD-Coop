# 작업 시작 순서

AGENTS.md → PROGRESS.md → PLANS.md → KNOWN_ISSUES.md → TEST_REPORT.md → git status 순으로 확인하고 이전 완료 조건을 점검한다. 사용자 변경을 보존하고 다음 미완료 단계부터 작업한다.

Java/libGDX 및 Wrapper를 유지한다. 변경 전 기준 테스트를 확보하고 작은 단위로 검증한다. iPad/Cloud만으로 수행하며 로컬 PC, ADB, 에뮬레이터를 요구하지 않는다. 단일 영웅 포인터를 바꿔 끼우거나 테스트 전용 엔진을 만들지 않는다. 실제 원본 코드 경로와 협동 저장 영역을 검증한다.

요구사항은 GOALS.md, 실행 경계는 ARCHITECTURE.md, 미정 규칙은 DECISIONS.md를 따른다. 작업 종료 시 진행 상황·실제 명령과 결과·알려진 문제를 갱신한다. PASS/FAIL/NOT RUN을 구별한다. APK·서명 비밀은 Git에 넣지 않는다. 외부 공개 릴리스에는 명시적 승인이 필요하다.

LAN 변경은 docs/coop/lan-protocol.md와 실제 프레임을 함께 갱신한다. 수신 스레드에서 Dungeon/CoopSession을 호출하지 않는다. 독립 JVM 시나리오의 좌석별 적용 횟수와 실제 호스트 상태를 함께 검증한다.

SYNC1에서 새 상태를 공개할 때는 SeatProjection의 좌석별 필터, 원본 직접 시야, StateImage 정규화와 DisplayReplica의 원자적 적용을 함께 검증한다. 행동 결과 조회 전에 연결·좌석을 인증한다. 상세 형식은 docs/coop/state-sync.md에 있다.

협동 규칙을 수정할 때는 두 좌석의 직접 시야·기억·소유권과 사망 상태를 함께 검사한다. 구역 계산은 Actor나 RNG를 변경하지 않는다. 부활은 앙크 소유자와 원래 Actor ID를 보존하고 확정된 행동의 재전송을 검증한다. 현재 지원 범위와 Phase 7 저장 경계는 ARCHITECTURE.md 및 docs/coop/state-sync.md를 참조한다.

협동 저장 변경은 docs/coop/save-format.md와 docs/coop/reconnect.md를 함께 확인한다. 영수증과 엔진 이미지를 같은 세대에 발행한 뒤 성공을 반환한다. 복원 시험은 기존 호스트와 클라이언트를 종료하고 새 프로세스에서 수행한다. 개인 키, 세션 파일, 키가 들어가는 프로세스 입력은 검증 ZIP에 넣지 않는다.
