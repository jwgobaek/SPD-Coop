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

## Android 데이터 보존 업데이트

GB2 MuMu Android 15의 기본 Device-2 한 개에서 로그인 없는 Chromium으로 기존 Drive 파일을 받았다. Windows로 전송하는 과정 없이 Android Downloads의 APK를 Android 패키지 관리자에 전달했다. 기존 드워프 왕판에서 교체 설치한 결과 최초 설치 시각이 그대로였고 실제 설치본과 다운로드 APK의 SHA-256이 모두 배포본과 같았다.

기존 1층 11세대 전사·마법사 방을 방장 좌석으로 열어 두 영웅과 원본 HUD를 확인했다. 터치로 가방과 투석 정보, 퀵슬롯 선택·취소를 열고 방을 저장하여 닫았다. 앱을 종료한 뒤 재실행했다. 실제 Android에서 두 번째 좌석, 조준 및 공격, 금고·악마의 전당, 보스전, 휴대폰 Wi-Fi·핫스팟은 NOT RUN이다. [Android 시작 화면](imp-halls/android/title.png) · [기존 방 복원](imp-halls/android/host-restored.png) · [인벤토리](imp-halls/android/inventory.png) · [퀵슬롯](imp-halls/android/quickslot.png)

시험 프로세스 크래시 버퍼에서 치명 오류 0건, 최근 Android 이벤트 1,500행에서 이 패키지의 ANR 0건을 확인했다. 이번 다운로드를 제거한 후 ADB forward/reverse는 각각 0개였고 #2만 실행 중이었다. 화면과 진단은 Windows의 메모리로 전달했다. 이번 작업이 만든 Windows 프로젝트 파일, 매핑 및 테스트 리스너는 없었다. 기존 개인 폴더의 파일은 조사 대상에서 제외했다.

재실행 뒤 협동 저장 목록에서 같은 방의 1층 13세대를 확인했다. Android 증거에는 세션 인증 키나 사용자 저장 파일을 포함하지 않았다.
