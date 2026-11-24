# 미해결 문제

- 사용자 소유 GitHub 대상 미연결. gh api user가 HTTP 401 Bad credentials를 반환했다. iPad에서 해당 저장소를 Codex에 연결하고 콘텐츠·워크플로 변경 및 Actions 실행 권한을 허용해야 한다. 로컬 브랜치와 파일의 세션 간 보존은 확인되지 않았다.
- :core:test에는 테스트 소스가 없다. Headless 및 독립 소켓 테스트는 Phase 2 이후 구축한다. 플레이, 기기 설치 및 네트워크 동작은 미검증이다.
- 원본 Gradle은 deprecated API를 사용한다. 일부 .so의 디버그 심볼 제거 경고, apksigner의 META-INF 항목 관련 경고가 있다. 검사 원문은 docs/coop/*apk.txt에 있다.
- 디버그 키의 runner 간 지속성은 보장되지 않는다. 독립 앱 이름과 ID는 개발용이며 브랜드·리소스별 권리(특히 폰트·음악) 검토는 공개 배포 전 남아 있다. LICENSE.txt 및 원저작자 크레딧, 번역은 보존했다.
- 단일 영웅 의존 색인은 정적 어휘 기반이다. 반사형 Bundle 복원, 비동기 콜백, 보스별 특수 효과의 완전성은 동적 회귀 검증이 필요하다.
