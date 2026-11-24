# 4.0.2 기반 개발 기준판

- 공식 태그 고정, Android 개발 패키지 org.spdcoop.dungeon.indev와 표시명 Dungeon Co-op 적용.
- Wrapper 배포 SHA-256 고정, Android 설정·APK 검사·CI Artifact 워크플로 추가.
- 엔진 의존 위치와 싱글 회귀 테스트 설계 기록. 원본 Java 게임 로직과 에셋 변경 없음.

## Phase 2

- JUnit 4와 libGDX Headless 테스트 백엔드를 테스트 범위에 추가했다. DungeonGenerationTest가 실제 전사 생성, 시드 402 하수도 지형, 층 진입과 저장 후 지형 복원을 검증한다.

- Phase 2에서 선택적 HUD/입력 연결, Hero 이동 애니메이션, Goo 효과와 Heap 낙하 애니메이션을 headless로 구동 가능하게 했다. 다중 시드, 행동 비용, 몬스터 AI, 실제 스케줄러, 아이템·저장 복원, 보스 및 승패 회귀를 포함해 9건을 실행했다.
- GitHub 개발 브랜치와 초안 PR #1을 생성했다. Cloud와 Actions에서 단일 검증 스크립트를 사용한다.
