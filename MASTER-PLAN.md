# SPD-Coop 개발 기준

## 감옥 6층 상점 · P1-A

감옥 P1-A는 6층 상인의 거래와 재시작 영수증을 다룬다. 이어서 P1-B 투척물 보관, P1-C 감옥 생성·몬스터·Wandmaker, P1-D 다층 저장, P1-E Tengu, P1-F 정상 진행을 검증한다. 11층 동굴은 감옥 검증 이후다.

## 현재 개발 체크포인트

V4 개인 퀵슬롯, V5 가방·카메라·휴식 조작, H1 두 갑옷 석상 조합, H2 두 주문서와 던지는 돌, H6 첫 감옥 층을 연결했다. 다음 콘텐츠 목표는 상점 거래와 감옥 7–10층의 원본 생성·보스전이다. 개별 지원 범위는 [콘텐츠 표](CONTENT-COMPATIBILITY.md)에 기록한다.

## Phase 8 그래픽 체크포인트

원본과의 시각적 일치가 Phase 8의 인수 조건에 포함된다. V1은 네이티브 지형 레이어, V2는 공개 좌석의 HUD·툴바, V3은 장비·가방·아이템 정보창을 기본 협동 화면에 연결한다. 원본과 협동의 실제 장면, 검증한 조작, 남은 차이는 [GRAPHICS-PARITY.md](GRAPHICS-PARITY.md)에 기록한다.

공식 v4.0.2를 기반으로 같은 로컬 네트워크에서 두 명이 원본 던전을 함께 플레이하는 Android 게임을 개발한다. 승인된 설계, 원본 보존 원칙과 초기 지원 플랫폼은 [GOALS.md](GOALS.md), 작업 순서는 [AGENTS.md](AGENTS.md)를 따른다.

| 단계 | 현재 기준 | 확인 자료 |
| --- | --- | --- |
| 0–2 | 원본 소스·빌드와 JVM 싱글플레이 회귀 | [빌드](BUILD.md), [검증](TEST_REPORT.md) |
| 3–5 | 두 영웅, 단일 Actor 스케줄러, 호스트 권한, 좌석별 스냅샷·영수증 | [구조](ARCHITECTURE.md), [상태 동기화](docs/coop/state-sync.md) |
| 6–7 | 독립 시야·탐험, 소유권, 부활, 원자적 저장, 기존 좌석 재접속 | [저장 형식](docs/coop/save-format.md), [재접속](docs/coop/reconnect.md) |
| 8 진행 중 | 원본 UI의 협동 알파, 전사·사냥꾼·마법사 일부 행동, 하수도 1–5층과 Goo, 시드 581·1105 생존 완주, 581 감옥 6층 진입과 재개 | [현재 지원표](CONTENT-COMPATIBILITY.md), [플레이 안내](USER-GUIDE.md) |
| 9–10 | 전체 콘텐츠 통합 후 안정화, 실기기 검증, 이후 플랫폼 | [단계 계획](PLANS.md) |

진행 상태는 [PROGRESS.md](PROGRESS.md), 실제 실행 결과는 [TEST_REPORT.md](TEST_REPORT.md), 미지원 조합은 [KNOWN_ISSUES.md](KNOWN_ISSUES.md)에 기록한다. 임시 게임 규칙은 [DECISIONS.md](DECISIONS.md)에 남긴다. 다음 세션은 [PLANS.md](PLANS.md)의 첫 미완료 항목에서 시작한다.
