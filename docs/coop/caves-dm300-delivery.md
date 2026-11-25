# 동굴 개발판 발행 확인 · 2026-11-25 UTC

| 항목 | 검증 결과 |
|---|---|
| 기준 소스 | 76ed0506c9589c17dcbdaaca7b78a886f3fb0e92 |
| APK 소스 | 23e6a84f0320649820721f974db9c1695165c064 |
| 통합 ZIP 문서·검증 커밋 | 2d5c4e3c002d303aab2dfe12ae4584d76548c6e9 |
| 전체 회귀 | 185 PASS, 0 FAIL, 0 SKIP. 시작 시 원격 기준 174/174 재실행. |
| APK | 35,297,164 bytes, SHA-256 23a724056c750e470169cf758daa7e43e0ab1b9e7e68c64c373f43d0d1002f08 |
| ZIP | 70,724,401 bytes, SHA-256 402f1a912bfea82d650c6bab6e0f67ffe012c20859f273edf2d2076f5d531a1e |
| 패키지 | org.spdcoop.dungeon.indev, 4.0.2-INDEV-caves-dm300, 922. 기존 인증서로 v1·v2 PASS. |
| ZIP 검사 | 빌드 커밋의 소스 2,165개 일치, 체크섬 2,419개 대조. JUnit XML, APK 검사, 실제 화면 23개, Android 결과와 시드 기록 포함. |
| Drive 업로드 | APK 1회, ZIP 1회. 각각 기존 ID·상위 폴더와 소유자를 확인했다. 두 파일 모두 소유자 연결로 내려받은 바이트의 크기·SHA-256이 일치했다. UPLOADED_AND_HASH_VERIFIED. |
| 리비전 | 두 파일에 각각 리비전 3개, 직전 리비전 ID 존재. 기존 Tengu 파일의 Cloud 보존 사본도 둘 다 이전 해시와 일치. 이전 리비전 영구 보존 또는 실제 롤백은 NOT RUN. |
| 공유 | 각 파일에 소유자 권한만 있고 shared=false. 연결 기능이 누구나 읽기 권한을 지원하지 않으며 소유자 브라우저 세션이 없어 권한 설정은 NOT RUN. 로그인 없는 실제 다운로드는 둘 다 FAIL. |
| Android | 발행 후 MuMu Chromium에서 지정 APK 링크가 Google 로그인을 요구했다. 기존 41c3efd의 실제 기동 PASS. Tengu 및 동굴판 데이터 보존 업데이트, 게임 실행, 크래시·ANR, 무선 LAN은 NOT RUN. |
| GB2 종료 확인 | 기본 MuMu 1개, ADB forward/reverse 목록 비어 있음, 지정 Android 다운로드 파일 없음, 기존 설치 앱으로 복귀. Windows에 프로젝트 파일·리스너를 만들지 않았다. 기존 파일에 대한 디스크 전체 검색은 수행하지 않았다. |

실제 전투·보상·이동·저장과 정상 시작 실패의 조건은 [동굴 시험 보고서](caves-dm300-results.md)에 기록했다. 다음 세션은 571의 GNOLL 광산과 570의 CRYSTAL 광산을 본편과 같은 depth에서 구분하여 새 JVM에서 복원하는 시험부터 시작한다.
