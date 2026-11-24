# 2026-11-24 — Phase 0 / Phase 1

시작 시 /workspace에는 연결된 Git checkout, 프로젝트 문서, Android SDK, javac, 시스템 Gradle이 없었다. Java 21 실행기와 gh는 있었다. 기존 디렉터리를 보존하고 spd-coop에 공식 태그를 shallow clone했다. 공식 ls-remote 결과와 로컬 tag^{commit}, HEAD가 모두 57a4e06a4caf162446d1c28caa7983f0493fecf0이다. 개발 브랜치는 coop/baseline이다.

- Phase 0 클라우드 기준 충족. 소스 변경 전 원본 APK와 독립 ID의 개발 기준 APK를 각각 생성하고 검사했다. CI 정의와 빌드 절차 작성 완료. GitHub 저장과 실제 Actions 실행은 권한 연결 대기.
- Phase 1 분석 기준 충족. ARCHITECTURE.md에 주요 실행 경로, 전역 상태, 실제 수정 지점과 Headless 제약을 정리했다. docs/coop/dependencies.tsv는 전체 Java 소스 어휘 색인이다. 게임 Java 클래스나 메서드 수정 없음.
- 다음 단계는 PLANS.md의 원본 JVM 회귀 데이터 수집이다. 이 기준 APK에서 제공하는 게임 모드는 원본 싱글플레이다.
