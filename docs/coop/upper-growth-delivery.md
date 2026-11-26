# 상위 성장 APK와 통합 ZIP 발행

시작 원격 커밋은 9ae5eff26a5fe78dc27264a54790166b56b65ed4, APK 빌드 커밋은 d504fa7dfe962fd182d336dc493d09f9b5424925다. ZIP에는 해당 빌드 소스와 검증 문서 커밋 e83b0df6995c6de4490d2476f5b68f3c02f33e53의 기록을 담았다.

| 결과물 | 실제 비로그인 다운로드 크기 | SHA-256 | 세션 전송 |
|---|---:|---|---:|
| [SPD-Coop-latest.apk](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) | 35,345,914 | f6448837b2df7b5ae328782faa600c8f94df6ea24b15efadd6763545189e2a76 | 1 |
| [SPD-Coop-latest-backup.zip](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) | 74,581,688 | 5ecd9c44e54cb91b0a5fdcccac7c69096b8c3ea74250e2ee76b7d823af94d0ed | 1 |

두 기존 파일 ID와 상위 폴더 ID를 대조했다. 최종 일반 공유는 두 파일 모두 anyone/reader, allowFileDiscovery=false였다. 비로그인 클라이언트에서 두 전체 바이너리를 읽어 위 크기와 해시가 로컬과 일치함을 확인했다. 직전 리비전은 각 파일의 갱신 전 현재 리비전 ID와 일치하고 keepForever는 false다. 연결 기능에는 보존 플래그 변경 도구가 없었다. APK 직전 리비전의 인증된 조회는 지원하지 않는 MIME 형식 오류를 반환했다. 실제 롤백과 장기 보존은 미검증이다.

APK의 패키지는 org.spdcoop.dungeon.indev, 버전은 4.0.2-INDEV-upper-growth, versionCode는 922다. v1·v2 서명과 기존 인증서 SHA-256 0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53을 검사했다. 파일 575개를 대상으로 APK의 금지 파일명과 민감값 패턴을 확인했다. ZIP에서 소스 2,319개와 체크섬 2,630개, JUnit XML 71개, Android 캡처 24개를 확인했다. ZIP에 APK, 사용자 게임 저장본 또는 개인 키를 포함하지 않았다. [개발과 시험](upper-growth-results.md) · [Android 실측과 PC 정리](upper-growth-android.md)
