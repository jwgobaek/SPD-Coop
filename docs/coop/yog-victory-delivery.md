# Yog 일반 승리판 발행 및 Android 시험

| 항목 | 실제 확인값 |
|---|---|
| 빌드 소스 | f1e1bb2a668e4933e0a1782f40ce612fee7d51aa |
| 패키지·버전 | org.spdcoop.dungeon.indev · 4.0.2-INDEV-yog-victory · versionCode 922 |
| APK | 35,324,056바이트 |
| APK SHA-256 | 1a4beeb3b04ce0ac76337d73d3228571a5aadea717ada0543ecf5646295c8719 |
| 인증서 SHA-256 | 0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53 |
| 서명 | v1·v2 서명 확인. 이전 설치본에 같은 패키지로 업데이트 성공 |
| 전체 회귀 | 시작 소스 208/208 PASS, 빌드 소스 213/213 PASS, XML 60개. 실패·오류·건너뜀 0건 |
| APK 발행 전 GitHub | daa2ce6b62e5ac609b2f0183af2ad58fd8655468 |

Android 15 MuMu Device-2 한 개에서 비로그인 Chromium으로 지정된 Drive 파일을 직접 받았다. 기존 imp-halls 설치본의 SHA-256은 10298aa225ff0ea0b5a4c52696e2a628ddf2899b29d1eee6221d4b600514a8ca였다. 다운로드 파일과 교체 설치 후 base.apk의 크기·해시가 Cloud 서명 검사본과 일치했다. 첫 설치 시각이 유지됐다.

기존 저장 목록은 업데이트 전후 동일한 1층 방과 세대 13을 표시했다. 같은 화면 전체의 PNG 해시도 일치했다. 호스트 자격으로 방을 재개해 20/20 HP, 두 영웅, 좌석의 안개, 원본 HUD, 인벤토리, 투척용 돌 정보, 길게 누른 퀵슬롯 선택 창을 확인했다. 방을 저장하고 닫은 뒤 앱을 강제 종료·재실행했다. 세대 15로 표시되는 같은 방을 한 번 더 열어 확인했다. 시험 구간의 crash 및 events 버퍼에서 해당 패키지의 크래시와 ANR은 각각 0건이었다.

신규판의 2인 Android 인증, 실제 조준 발사, 25층 Yog와 26층 아뮬렛, 양쪽 승리 투표는 Android NOT RUN이다. 기존 실사용 세이브는 1층에서 동료를 기다리는 상태였다. 실기기 Wi-Fi·핫스팟·장시간 성능도 NOT RUN이다. 두 언어 Desktop의 최종층과 승리 UI, 독립 JVM·TCP는 별도로 실행했다. [게임·저장 검증 결과](yog-victory-results.md)

MuMu에 이번에 받은 APK 한 개를 해시 확인 후 삭제했고 파일 부재를 재확인했다. ADB 정방향·역방향 매핑은 비어 있었고 Windows 파일 생성이나 리스너 설치 없이 표준 출력으로 검사했다. 종료 시 기본 인스턴스 #2만 실행 중이고 #3은 종료 상태였다. Windows 파일 확인 범위는 이번 작업에서 실행한 명령과 경로다. 공개 ZIP에는 방 식별자가 포함된 저장 목록 이미지와 개인 세이브를 담지 않는다.

[지정 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) · [지정 통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)
