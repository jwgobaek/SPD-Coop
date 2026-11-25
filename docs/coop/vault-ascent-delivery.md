# 금고 정령과 지상 귀환판 배포

시작 소스는 `f239c06a638b20f2729579f5ee7a4c72f7aa911c`이고 APK 빌드 소스는 `ecccda0e62aac4b68e6ae1f33bcd5e98ef882fa9`이다. 두 관리 파일은 기존 부모 폴더의 동일한 ID를 사용한다. 전체 테스트, APK 패키지·서명 검사, GitHub 일반 푸시 및 원격 해시 대조를 마친 뒤 APK를 한 번 전송했다.

| APK 항목 | 실측 |
|---|---|
| 패키지 | org.spdcoop.dungeon.indev |
| 버전 / versionCode | 4.0.2-INDEV-vault-ascent / 922 |
| 크기 | 35,329,331바이트 |
| SHA-256 | 2ac0e38d7b2867a372540a71f625e2783bb8c9aff019e22f068e37b15e3ad47d |
| 기존 인증서 SHA-256 | 0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53 |
| 서명 | v1, v2 PASS |
| 전체 JUnit | 시작 213/213, APK 220/220 |
| 익명 다운로드 | Cloud에서 실제 다운로드한 APK 바이트·크기·해시 일치 |
| Android | Android 15 MuMu 내부 Chromium에서 비로그인 다운로드. Android에서 받은 파일의 크기·SHA-256을 확인하고 같은 versionCode의 기존 앱 위에 업데이트. 설치 후 base.apk와 Cloud APK SHA-256 일치. |

[최신 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) · [최신 통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) · [게임 검증 및 다음 작업](vault-ascent-results.md)

기존 첫 설치 시간이 유지됐다. 기존 전사·마법사의 1층 세대 17을 방장으로 열어 HUD, 인벤토리와 퀵슬롯 선택기를 확인했다. 저장 후 방을 닫고 앱을 강제 종료·재실행해 같은 슬롯 세대 19를 확인했다. 해당 시간대 패키지 크래시·ANR 기록은 각 0건이었다. Android 실전 공격·조준·정령·귀환·2인 승리 투표·휴대폰 Wi-Fi·핫스팟은 NOT RUN이다.

MuMu에는 이번 다운로드 설치 파일만 해시 대조 후 제거했다. Windows 프로젝트 파일을 쓰지 않고 메모리와 ADB 표준 출력만 사용했다. 종료 시 ADB forward/reverse 목록은 비어 있었다. 기본 Device-2가 실행 중이고 보조 Device-2-1은 종료 상태였다. Windows 전체 디스크 검색은 수행하지 않았다. 안드로이드 세이브나 개인 식별 정보는 공개 ZIP에서 제외했다.
