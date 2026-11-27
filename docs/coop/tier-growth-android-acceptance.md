# SYNC35 Android 인수

2026-11-27 GB2의 Android 15 MuMu 두 대에 e690e533383a68978781842a949cd0696bfecb63 후보를 각 Android 내부 Chromium에서 비로그인으로 내려받고 기존 데이터 위에 설치했다. 두 설치본과 Drive 비로그인 다운로드는 모두 37,321,868바이트이며 SHA-256은 cc8a6f7ab203a01e7beacfc7e2fcb00ad487175be537cd6cfa25689e96e9aa4b다. 패키지는 org.spdcoop.dungeon.indev, versionCode는 922다. 기존 개발 인증서 SHA-256은 0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53이다.

| 격리 시험방 | 방장 | 게스트 | 실제 확인 결과 |
|---|---|---|---|
| HolyIntuition | PASS | PASS | 자기 방어구의 저주 여부 공개, 양쪽 충전 차감 |
| RecallInscription | PASS | PASS | 각인이 가리키는 감정 주문서의 실제 효과로 자기 물품 감정 |
| Trinity BodyForm | PASS | PASS | 선택한 자기 장비의 Blocking 기억, 충전 4→2 |
| Trinity MindForm | PASS | PASS | 자기 마력탄 지팡이 형상 선택, 충전 4→1 |
| Trinity SpiritForm | PASS | PASS | 자기 신속의 반지 형상 선택, 충전 4→0 |

각 셀에서 원본 메뉴를 열고 선택을 취소한 뒤 다시 열어 자기 물품을 선택했다. 다섯 시험방 모두 양쪽 앱을 재실행해 기존 좌석을 재인증하고 다음 행동을 확정했다. 별도 과거 발견 시험방에서는 두 좌석 모두 지팡이가 가방에 없는 상태에서 마력탄 기억을 선택했다. 위 Trinity 시험은 형태 설정과 저장까지 확인했다. 설정한 형상으로 후속 갑옷 능력을 발동해 보호막이나 피해를 발생시키는 입력은 이번 Android 표본에 포함하지 않았다.

- 과거 SYNC32에서 작성된 스키마 21 성서 시험방을 SYNC35가 복원했다. 재인증과 다음 행동 후 호스트 HEAD는 19세대, Turn 12였다.
- 광전사 시험방에서 폭발 함정을 실제로 밟아 HP 1→0, ALIVE, 보호막 24, 최초 치명 피해 직전 HP 1을 기록했다. 양쪽 재인증 후 다음 행동에서 8세대, 보호막 23, HP 0, ALIVE를 읽었다. 보호막 완전 소멸과 부활의 Android 경계는 미실행이다.
- 사제의 정화와 성창, 게스트의 독 다트 투척, Kinetic 원소 타격, 재충전 주문서 읽기와 해당 각인 회수를 실행했다. Kinetic 시전 후 갑옷 충전은 25.2였다. 실명 다트 Android 투척은 미실행이다.
- 원본 생성기로 마련한 2층 비축 식량을 Android에서 단일 도적이 수거하고 먹었다. 생성과 상자 열기는 Cloud에서 시험방을 준비할 때 수행했다. Android에서 정상 시작으로 해당 층을 생성하는 시험은 미실행이다.
- 전사가 분노 자극, 급조한 투척물, 굳건한 태세에 각각 1점을 투자하고, 대기로 원본 HoldFast 버프를 얻었다. 동료의 세 투자 잔액과 버프는 독립적이었다.
- 두 화면에서 가로 방향으로 전환한 뒤 원래 설정을 복원했다. 앱이 계속 응답했고 복귀 후 두 좌석에서 실제 행동을 확정했다. 오래된 선택의 별도 거부 및 SYNC 불일치 화면의 실제 Android 시험은 미실행이다.
- 게임 시험 구간의 Android events 버퍼는 방장 5,285행, 게스트 4,266행이었다. 해당 앱의 am_crash, am_anr, fatal AndroidRuntime 및 치명적 GLThread 예외는 두 기기 모두 0건이었다. 개별 기기의 시험 시작과 마지막 로그 관측 epoch는 1795745151–1795749364, 1795745153–1795749365이다.
- Windows 루프백 ADB 중계와 별도 시험 자격을 사용했다. 실제 휴대폰 Wi-Fi, 핫스팟, 멀티터치 핀치 및 정상 시작 캠페인은 이번 Android 인수 범위 밖이다.

통합 ZIP의 evidence/android-sync35에는 두 부팅 ID와 10개 관측 행, 원본 창의 실제 입력 화면, 식별 정보를 제거한 세대·충전·버프 기록이 있다. 각 선택 시나리오의 가로 그림은 첫 선택창, 취소, 재선택, 확정, 양쪽 재인증 후 다음 행동 순서다. source 디렉터리는 APK 빌드 커밋과 바이트 단위로 대조했다. 이후 개발 소스와 개인 세이브·티켓은 배포 자료에서 제외했다. [발행 검사기용 10개 실측 행](tier-growth-android-observations.tsv)

## 배포 파일 재검사

| 관리 파일 | 바이트 | 비로그인 다운로드 SHA-256 | 이번 갱신 |
|---|---:|---|---:|
| [APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) | 37,321,868 | `cc8a6f7ab203a01e7beacfc7e2fcb00ad487175be537cd6cfa25689e96e9aa4b` | 1 |
| [통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) | 74,236,263 | `112f1862dd3a9c25c60e87756093b49598f93962d6552d5debcfff2f16fd8ed1` | 1 |

플랫폼을 통한 두 기존 파일의 갱신이 성공했다. 기존 anyone/reader 및 allowFileDiscovery=false 권한을 확인했다. ZIP의 소스 파일 2,359개는 e690e53과 일치한다. 검사기는 2,533개 체크섬을 검증했다. 종료 후 두 MuMu가 타이틀에서 응답했고, 이번에 추가한 ADB 매핑과 Android 다운로드를 정리했다. 이후 GB2에서 CPU 8%, 가용 RAM 2,753/16,085 MiB, 응답 중인 MuMuNxDevice 두 개를 관측했다.

후속 Cloud 전수 회귀 도중 04:31 UTC에 GB2를 읽기 전용으로 다시 조회했다. CPU 30%, 가용 RAM 2,654/16,085 MiB였고 MuMu 프로세스 두 개 모두 응답했다.
