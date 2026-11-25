# SPD-Coop 개발 기준

공식 v4.0.2를 기반으로 같은 로컬 네트워크에서 두 명이 원본 던전을 함께 플레이하는 Android 게임을 개발한다. 승인된 설계, 원본 보존 원칙과 초기 지원 플랫폼은 [GOALS.md](GOALS.md), 작업 순서는 [AGENTS.md](AGENTS.md)를 따른다.

| 단계 | 현재 기준 | 확인 자료 |
| --- | --- | --- |
| 0–2 | 원본 소스·빌드와 JVM 싱글플레이 회귀 | [빌드](BUILD.md), [검증](TEST_REPORT.md) |
| 3–5 | 두 영웅, 단일 Actor 스케줄러, 호스트 권한, 좌석별 스냅샷·영수증 | [구조](ARCHITECTURE.md), [상태 동기화](docs/coop/state-sync.md) |
| 6–7 | 독립 시야·탐험, 소유권, 부활, 원자적 저장, 기존 좌석 재접속 | [저장 형식](docs/coop/save-format.md), [재접속](docs/coop/reconnect.md) |
| 8 진행 중 | 원본 UI의 협동 알파, 전사·사냥꾼·마법사 일부 행동, 하수도 1–5층과 Goo, 정상 시작 시드 581·1105 | [현재 지원표](CONTENT-COMPATIBILITY.md), [플레이 안내](USER-GUIDE.md) |
| 9–10 | 전체 콘텐츠 통합 후 안정화, 실기기 검증, 이후 플랫폼 | [단계 계획](PLANS.md) |

진행 상태는 [PROGRESS.md](PROGRESS.md), 실제 실행 결과는 [TEST_REPORT.md](TEST_REPORT.md), 미지원 조합은 [KNOWN_ISSUES.md](KNOWN_ISSUES.md)에 기록한다. 임시 게임 규칙은 [DECISIONS.md](DECISIONS.md)에 남긴다. 다음 세션은 [PLANS.md](PLANS.md)의 첫 미완료 항목에서 시작한다.
