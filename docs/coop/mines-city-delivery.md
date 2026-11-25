# 광산·드워프 왕 개발판 · 2026-11-25 UTC

| 항목 | 확인한 값 |
|---|---|
| 전체 JUnit | 기준 df645fc의 185/185를 다시 실행했고 최종 APK 소스 54742d9에서 전체 199/199가 통과했다. JUnit XML 55개에서 실패·오류·건너뜀이 각각 0이었다. |
| 시작 Git 커밋 | df645fc08d0b33dbd5e2d7a847f075dc26d4b020 |
| APK 빌드 커밋 | 54742d960fbc4d975e1285dc5cd5567533c2c103 |
| Android 패키지 | org.spdcoop.dungeon.indev · 4.0.2-INDEV-dwarf-king · versionCode 922 |
| APK | 35,308,093바이트 · SHA-256 `a2046832ea3a1897af21a20ede4de20664329333fca4c4b3b5e8350ed4546aa8` |
| 개발 인증서 | SHA-256 `0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53` · v1·v2 검증 통과 |
| 게임 범위 | 본편 1~20층 중 검증된 생성 조합, 같은 깊이의 대장장이 광산 2종 · 저장 스키마 1~11 · SYNC22 · LAN v1 |
| Desktop | 빌드 커밋과 같은 게임 코드에서 한국어·영어 각 7장. 광산 두 곳, 도시 상점, 대장장이 주문·장비 선택, 드워프 왕의 보호막·왕관을 배치형 시험으로 촬영했다. |

보스전의 시험용 영웅 조건, 정상 시작 8개 시드, 34개 독립 호스트 강제 종료 경계와 성능 표본을 공개한다. [광산과 드워프 도시 시험 기록](mines-city-results.md)

## Android 업데이트와 정리

GB2의 MuMu Android 15 기본 Device-2 한 개에서 비로그인 Chromium으로 같은 Drive 파일을 내려받았다. 동굴판에서 드워프 왕판으로 업데이트한 뒤에도 최초 설치 시각과 Android 서명 기록이 유지됐다. 내려받은 APK와 실제 설치된 base.apk 모두 위 빌드의 크기·SHA-256과 일치했다. 기존 전사·마법사 1층 9세대 방을 원래 방장 자격으로 열었고 두 영웅과 원본 HUD가 나타났다. 인벤토리와 장비 설명, 퀵슬롯 길게 누르기·취소, 방 저장·닫기 및 앱 재실행이 통과했다. [타이틀](mines-city/android/title.png) · [복원된 방](mines-city/android/host-restored.png) · [인벤토리](mines-city/android/inventory.png) · [퀵슬롯](mines-city/android/quickslot.png)

시험 프로세스의 크래시 버퍼에서 치명 오류가 0건이었고 마지막 Android 이벤트 1,500행에서 해당 패키지의 ANR 기록은 0건이었다. 두 번째 좌석의 Android 재인증, Android 광산·도시·보스전, 실제 무선 LAN·핫스팟·발열은 NOT RUN이다. 이번 Android 다운로드 파일을 제거했다. 최종 ADB forward/reverse 목록은 비어 있었고 MuMu #2만 실행 중이었다. 화면은 표준 출력으로 Cloud에 수집했다. Windows에서 프로젝트 파일이나 테스트 리스너를 만들지 않았으며 기존 사용자 폴더를 검색하지 않았다.
