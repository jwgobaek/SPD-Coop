# Imp 금고·악마의 전당 검증판

2026-11-25 UTC. 시작 커밋은 `960e4bc9ad4b94fdb4de7959c41c17827066ff39`, APK 소스는 `5f00b27baff66210902e109cba23e2c9b70016bc`이다.

| Android 패키지 검사 | 확인한 값 |
|---|---|
| 패키지 | `org.spdcoop.dungeon.indev` |
| 버전 | `4.0.2-INDEV-imp-halls` · versionCode 922 |
| APK | 35,316,633바이트 · SHA-256 `10298aa225ff0ea0b5a4c52696e2a628ddf2899b29d1eee6221d4b600514a8ca` |
| 서명 | v1 및 v2 PASS · 이전과 같은 개발 인증서 SHA-256 `0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53` |

시드 572의 금고 보관·반환과 실제 별도 JVM 영수증 복구, 정상 성장 표본, 악마의 전당 21~24층 왕복 결과를 수록했다. 공식 25층 보스 전투, 26층 아뮬렛 획득, 승리 종료, Ascension은 다음 개발 범위다. [시험 환경과 측정값](imp-halls-results.md) · [한국어 금고](imp-halls/ko/imp-vault.png) · [영어 21층](imp-halls/en/halls-21.png)

## 전체 회귀

기준 960e4bc를 변경하지 않고 재실행한 결과는 198/199였다. DM-300 보상 시험에서 원본 드롭 셀에 두 영웅이 겹칠 수 있는 배치를 수정했다. 최종 APK의 깨끗한 작업 트리에서 전체 JUnit 208/208이 통과했다. XML 58개에서 실패, 오류, 건너뜀이 모두 0이었다. 기존 두 시드의 하수도 실제 TCP 회귀와 신규 사후 영수증 복구 시험을 포함한다.
