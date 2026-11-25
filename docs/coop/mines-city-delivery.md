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

## 최종 배포

jwgobaek2022@gmail.com의 기존 SPD-Coop/01_Latest 두 파일 내용을 각각 한 차례 갱신했다. 두 파일의 ID와 부모 폴더가 같았으며 링크 공유 권한은 anyone/reader, 검색 노출은 비활성으로 유지됐다. 공개 로그인 정보 없이 각각 다시 다운로드해 로컬 크기와 SHA-256을 대조했다.

| 파일 | 바이트 | SHA-256 | Drive 검증 |
|---|---:|---|---|
| [SPD-Coop-latest.apk](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) | 35,308,093 | `a2046832ea3a1897af21a20ede4de20664329333fca4c4b3b5e8350ed4546aa8` | UPLOADED_AND_HASH_VERIFIED |
| [SPD-Coop-latest-backup.zip](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) | 72,962,573 | `2b6a1375eb3d492355bcf854624dc2dd18a9afd038c4bf543bda4835a21123f2` | UPLOADED_AND_HASH_VERIFIED |

통합 ZIP의 검증·문서 소스는 3cc59b18c3371a7ae9f9c30c7ea1f44ad54b1ef6이다. APK에 대응하는 원본 소스 2,216개와 매니페스트 체크섬 2,497개를 대조했다. ZIP에 원본 에셋, 라이선스, Gradle Wrapper, JUnit XML, 빌드·서명 검사, 영문·국문 Desktop 및 실제 Android 화면을 포함했다. 두 Drive 파일 각각의 리비전 수가 3에서 4로 늘었고 직전 리비전이 발행 전 HEAD와 일치했다. 이전 리비전의 장기 보존 및 실제 롤백은 NOT RUN이다.

다음 구현 시작점은 시드 572의 18층 Imp 금고다. 탈출 수정에서 두 좌석의 물리적 장비 ID와 골드·에너지를 한 번만 복구하는 확정 경계를 먼저 검증한다. 이후 21층의 DemonSpawner·RipperDemon, Eye, Succubus와 20↔21층 이동을 연결한다.
