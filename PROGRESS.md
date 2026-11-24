# 2026-11-24 — Phase 0 / Phase 1

시작 시 /workspace에는 연결된 Git checkout, 프로젝트 문서, Android SDK, javac, 시스템 Gradle이 없었다. Java 21 실행기와 gh는 있었다. 기존 디렉터리를 보존하고 spd-coop에 공식 태그를 shallow clone했다. 공식 ls-remote 결과와 로컬 tag^{commit}, HEAD가 모두 57a4e06a4caf162446d1c28caa7983f0493fecf0이다. 개발 브랜치는 coop/baseline이다.

- Phase 0 클라우드 기준 충족. 소스 변경 전 원본 APK와 독립 ID의 개발 기준 APK를 각각 생성하고 검사했다. CI 정의와 빌드 절차 작성 완료. GitHub 저장과 실제 Actions 실행은 권한 연결 대기.
- Phase 1 분석 기준 충족. ARCHITECTURE.md에 주요 실행 경로, 전역 상태, 실제 수정 지점과 Headless 제약을 정리했다. docs/coop/dependencies.tsv는 전체 Java 소스 어휘 색인이다. 게임 Java 클래스나 메서드 수정 없음.
- 다음 단계는 PLANS.md의 원본 JVM 회귀 데이터 수집이다. 이 기준 APK에서 제공하는 게임 모드는 원본 싱글플레이다.

## Phase 2 첫 Headless 회귀

사용자 저장소 jwgobaek/SPD-Coop를 remote coop으로 연결했다. master가 공식 기준 커밋과 일치하며 gh api user로 jwgobaek 인증에 성공했다. coop/baseline push는 403으로 거부되어 로컬 coop/phase2에서 계속했다. 기준 태그의 별도 checkout과 작업 checkout의 JVM에서 전사 시드 402의 첫 하수도 층을 실제 생성했다. 원본 지형 해시 비교, 층 진입, 저장된 층 지형 복원 테스트를 통과했다. 다음은 몹·아이템/장비와 전체 영웅 상태 비교 및 행동 완료 신호의 회귀 테스트다.
