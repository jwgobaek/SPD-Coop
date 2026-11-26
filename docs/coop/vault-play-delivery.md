# 금고 정상 입구 및 두 Android 좌석 검증판

| 항목 | 확인값 |
|---|---|
| 시작 원격 | `f4232290d8050d2c938bd927d95c8003f7e27f3f`, `coop/phase8` |
| APK 빌드 소스 | `4c2054eabef9c0525efd5bdbc964a6ee00cb1366` |
| 패키지 | `org.spdcoop.dungeon.indev` |
| 버전 | 4.0.2-INDEV-vault-play, versionCode 922 |
| 크기 | 35,329,774바이트 |
| SHA-256 | `a37c238d8adab0dd0186b9ee2562937a661e2fcf2a7903ab053da19cb45f2822` |
| 서명 | v1·v2 PASS, 기존 인증서 SHA-256 `0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53` |

## 이전 APK의 두 Android 좌석

각각 부팅 ID가 다른 MuMu Android 15 Device-2와 Device-2-1을 사용했다. 두 Android가 같은 가상 IP 10.0.2.15를 보고했다. 기본 인스턴스의 로컬 ADB는 127.0.0.1:5559이며 16448은 동일 인스턴스의 주소였다. 보조 인스턴스에는 127.0.0.1:16480을 사용했다. 같은 Windows PC의 임시 루프백 ADB 중계로 게임 연결을 시험했다.

기본 인스턴스에는 기존 ecccda0 APK가 설치돼 있었다. 보조 인스턴스의 41c3efd를 Android 내부 Chromium에서 받은 APK로 데이터 보존 업데이트했다. 두 base.apk 모두 기존 배포 해시 `2ac0e38d7b2867a372540a71f625e2783bb8c9aff019e22f068e37b15e3ad47d`와 일치했다. 두 설치본의 최초 설치 시각도 보존됐다.

사용자 저장과 다른 방 두 개를 생성했다. 마법사·사냥꾼 방에서 로비, 준비, 양쪽의 1층 진입, 이동·대기·동료 표시, 게스트 강제 종료·재실행·재인증, 호스트의 저장·방 닫기·이어하기와 양쪽의 새 행동을 확인했다. 복원 후 같은 방에서 Turn 12, State Version 17, pending 0, PERSISTENT가 일치했다. 마법사는 비밀 문을 찾아 실제 쥐에게 지팡이를 두 번 발사했다. 충전은 4→3→2가 됐으며 죽은 쥐의 고기를 주웠다.

마법사·전사 방에서 게스트는 실제 피해를 입었고 인벤토리에서 원본 투척용 돌을 선택해 쥐를 겨냥했다. 투척 후 게스트의 돌이 3→2가 됐다. 길게 누르기로 퀵슬롯 선택기가 열렸고 드래그 후 카메라가 이동했다. 게스트 Android 디스플레이를 가로로 회전하고 앱을 백그라운드에서 복귀시켰다. 방장이 두 좌석의 상태 확인을 기다리는 동안 게스트가 같은 좌석에 재접속했다. 세로 방향과 원래 자동 회전 설정을 복원하고 방장의 정상 저장·닫기를 확인했다.

## 검증 환경

Windows 파일을 쓰지 않고 ADB 표준 출력과 메모리 스트림을 Cloud로 받아 검사했다. 초기 관찰에서 Windows CPU 24%, 가용 메모리 3,308 / 16,085 MiB였다. 중간 표본의 CPU는 1–30%, 가용 메모리는 3,146–3,246 MiB였다. 기본·보조 인스턴스는 사용자가 이 세션에 미리 실행한 것이다. 실제 휴대폰 무선 LAN·핫스팟·핀치와 실기기 성능은 NOT RUN이다. Cloud KO·EN 화면은 생성된 금고 공격 예고의 배치형 Desktop 실행이다.

[게임 및 성장 결과](vault-play-results.md) · [최신 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) · [최신 통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)
