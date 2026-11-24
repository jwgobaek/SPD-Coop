# 작업 시작 순서

AGENTS.md → PROGRESS.md → PLANS.md → KNOWN_ISSUES.md → TEST_REPORT.md → git status 순으로 확인하고 이전 완료 조건을 점검한다. 사용자 변경을 보존하고 다음 미완료 단계부터 작업한다.

Java/libGDX 및 Wrapper를 유지한다. 변경 전 기준 테스트를 확보하고 작은 단위로 검증한다. iPad/Cloud만으로 수행하며 로컬 PC, ADB, 에뮬레이터를 요구하지 않는다. 단일 영웅 포인터를 바꿔 끼우거나 테스트 전용 엔진을 만들지 않는다. 실제 원본 코드 경로와 협동 저장 영역을 검증한다.

요구사항은 GOALS.md, 실행 경계는 ARCHITECTURE.md, 미정 규칙은 DECISIONS.md를 따른다. 작업 종료 시 진행 상황·실제 명령과 결과·알려진 문제를 갱신한다. PASS/FAIL/NOT RUN을 구별한다. APK·서명 비밀은 Git에 넣지 않는다. 외부 공개 릴리스에는 명시적 승인이 필요하다.

LAN 변경은 docs/coop/lan-protocol.md와 실제 프레임을 함께 갱신한다. 수신 스레드에서 Dungeon/CoopSession을 호출하지 않는다. 독립 JVM 시나리오의 좌석별 적용 횟수와 실제 호스트 상태를 함께 검증한다.
