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

## 신규 APK의 두 Android 좌석 — PASS

두 Android의 Chromium에서 Google 계정 로그인 없이 기존 Drive APK를 내려받았다. 설치 전 SHA-256과 35,329,774바이트를 각 Android에서 확인했다. 같은 패키지에 업데이트 설치한 뒤 각각의 base.apk가 위 해시와 일치했다. 두 설치 모두 versionCode 922, 4.0.2-INDEV-vault-play이며 최초 설치 시각도 보존됐다. 사용자의 기존 저장은 목록에서 세대 19로 확인했다. 전투는 이 세션의 별도 시험방에서 진행했다.

* 새 방에서 마법사·사냥꾼이 직업을 선택하고 준비했다. 양쪽 1층 진입 후 마법사가 한 칸 이동하자 게스트 화면의 동료 위치와 행동권이 바뀌었다.
* 이전 APK로 만든 마법사·전사 시험방을 양쪽에서 재개했다. 게스트가 인벤토리에서 돌을 골라 쥐를 조준·투척했고, 가방의 돌은 2개에서 1개가 됐다. 게스트 앱 프로세스를 강제 종료하고 다시 실행해 기존 자격으로 같은 방과 좌석 1에 재인증했다. 직후 두 진단에서 같은 방, Turn 69, State Version 78, pending 0, PERSISTENT가 일치했다.
* 호스트가 저장하고 방을 닫은 뒤 세대 80의 해당 방을 다시 열었다. 두 좌석이 플레이를 이어갔다. 호스트가 게스트 가까이 이동한 뒤 실제 쥐를 퀵슬롯 지팡이로 조준·발사해 충전이 4→3이 됐다. 전투 전후 게스트 HP는 18→11이었다. 마지막에 호스트가 시험방을 정상 저장하고 닫았다.
* 새 APK 발행 이후 두 인스턴스의 Android 이벤트 버퍼에서 이 패키지의 crash·ANR 기록은 각각 0건이었다. 실제 Android의 금고·보스·승리 투표·성장 능력, 응답 유실 재전송, 물리적 Wi-Fi·핫스팟·멀티터치 핀치, 장시간 실기기 성능은 NOT RUN이다.

## 종료 시 장치 상태

Windows CPU 37%, 가용 메모리 3,129 / 16,085 MiB를 마지막에 관측했다. 두 화면과 ADB가 응답했다. 심각한 프리징은 관찰되지 않았다. 양쪽 Chromium 다운로드의 정확한 경로와 체크섬을 대조해 이번 설치 파일 두 개만 제거했다. 양쪽 Android 다운로드 위치를 다시 조회했다. 호스트 forward 및 게스트 reverse를 선택적으로 해제하고 이번 루프백 리스너 0개를 확인했다. 이번 작업에서 연결한 게스트 ADB 주소만 해제했다. 기존 호스트 ADB와 Remote Desktop Commander는 응답했다. MuMuManager가 #2와 #3의 Android 및 프로세스 실행을 모두 보고했고 마지막 MuMu 기기 프로세스 수는 2였다.

Windows 프로젝트 파일 쓰기는 0건이었다. 화면은 메모리 스트림으로 전송했다. 정리 확인은 이번 세션의 명령·다운로드 경로·포트·프로세스를 대상으로 했다. 두 인스턴스를 계속 실행하는 승인은 이 세션에 적용했다.

## Google Drive 최종 발행 — UPLOADED_AND_HASH_VERIFIED

지정 계정의 기존 01_Latest에 APK 1회, 통합 ZIP 1회 전송했다. 두 기존 ID의 부모 폴더와 anyone/reader, allowFileDiscovery=false 메타데이터를 재조회했다. 계정 인증이나 쿠키를 전달하지 않은 별도 다운로드로 전체 파일을 다시 읽어 아래 크기와 SHA-256을 대조했다.

| 파일 | 크기(바이트) | 실제 다운로드 SHA-256 |
|---|---:|---|
| [SPD-Coop-latest.apk](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) | 35,329,774 | `a37c238d8adab0dd0186b9ee2562937a661e2fcf2a7903ab053da19cb45f2822` |
| [SPD-Coop-latest-backup.zip](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) | 73,287,221 | `154fa1e766be43e9dffaf7919ff1d24c4516a3a58e17dd9a7c008cdc6b02aa37` |

통합 ZIP의 소스 2,301개가 APK 빌드 커밋 4c2054e와 일치했고 체크섬 2,586개를 검증했다. 문서 및 검증 자료의 기준 커밋은 bd91180이다. Android 증거는 이번 시험방의 표준 출력 캡처와 요약이다. 패키징 시 APK 중첩, 서명 키와 사용자 세이브를 제외했다.

리비전 조회에서 직전 APK 35,329,331바이트와 직전 ZIP 73,819,931바이트가 이전 HEAD ID와 각각 일치했다. 두 직전 리비전 모두 keepForever=false였다. 연결 기능에서 리비전 고정을 수행할 수 없었다. 장기 보존과 실제 롤백은 미검증이다.
