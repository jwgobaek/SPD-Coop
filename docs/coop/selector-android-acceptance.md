# 성서 물품 선택 수정판 Android 인수

2026-11-27 07:55:13~08:16:33 KST에 GB2의 Android 15 MuMu 두 대에서 한국어 화면을 조작했다. 두 앱 모두 기존 패키지와 서명을 유지한 업데이트였고 최초 설치 시각은 보존됐다. 기존 격리 방 다섯 개에 원래 시험 좌석으로 접속했다. 전송은 Windows 루프백 ADB 중계였다.

| 실제 경로 | 방장 | 게스트 | 저장과 재인증 후 확인한 효과 |
|---|---|---|---|
| HolyIntuition | PASS | PASS | 자기 갑옷의 저주 여부 공개, 강화 정보 미공개, 성서 3→0 |
| RecallInscription | PASS | PASS | 자기 갑옷 감정, 성서 3→0 |
| Trinity BodyForm | PASS | PASS | Swiftness 선택, 성서 4→2 |
| Trinity MindForm | PASS | PASS | WandOfLightning 선택, 성서 4→1 |
| Trinity SpiritForm | PASS | PASS | RingOfHaste 선택, 성서 4→0 |

열 경우 모두 원래 메뉴와 가방을 열고 취소한 다음 다시 열어 자기 물품을 선택했다. 양쪽 앱 프로세스를 다시 시작해 저장된 좌석으로 인증하고 실제 다음 행동과 새 영수증을 확인했다. Trinity의 세 행은 형상 선택 주문에 대한 인수다. 정신 형상 대상의 빠른 두 번 누르기는 한 번만 소비했다. 신성한 직감 가방에서 홈으로 나갔다가 복귀하면 멀티플레이 로비로 돌아갔다. 재선택 전 영수증과 충전은 보존됐다. 화면 회전과 별도의 오래된 핸들 거부는 이번 Android 구간에서 NOT RUN이다.

양쪽 Android events 버퍼의 해당 패키지 am_crash·am_anr, crash 버퍼의 AndroidRuntime 패키지 사망 기록 및 GLThread 치명 예외는 모두 0건이었다. 관측표를 후보 바이너리와 함께 verify-final-deliverables.py --selector-candidate에 전달해 두 부팅 ID, 두 설치 해시, 열 행을 검증했다. [설치·시나리오 관측표](selector-android-observations.tsv) · [복원 효과와 영수증](selector-android-restored.tsv)

## 발행과 대응 소스

사용자가 승인한 예외 1회를 Drive 커넥터로 실행했다. 기존 APK의 ID와 anyone/reader 링크 권한, allowFileDiscovery=false를 유지했다. 양쪽 MuMu 내부 Chromium과 Cloud에서 비로그인 다운로드를 확인했다. APK 추가 갱신 1회, ZIP 갱신 0회, 신규 파일 0회다.

| 항목 | 대응 커밋 | 바이트 | 다운로드 SHA-256 |
|---|---|---:|---|
| APK | 9969d235a85c57019ba2963f873ab492367af164 | 37,306,668 | 66e1da255f50026f91db5cf89f9633842782a72e28b0f4191758cb0a428a1547 |
| 기존 통합 ZIP의 source | 07b18899f48b22142a602bbf095cddcef621a033 | 73,920,737 | 9218e329b5a66af74d75cb4c572fab768eed5fd48ac04dbfb4213dd3c13d1452 |

두 설치본의 base.apk가 표의 APK와 일치했다. 패키지는 org.spdcoop.dungeon.indev, versionCode는 922, 개발 인증서의 SHA-256은 0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53이다. 배포판은 스키마 1~21과 SYNC32를 사용한다. 기존 배포판에서 재현됐던 게스트 GLThread 크래시는 이 수정판에서 인수 기준을 통과했다. 이전 Desktop 20/20, 후보 JUnit 278/278은 별도 체크포인트다.

시험에 사용한 Android 다운로드 2개를 경로와 해시로 확인해 제거하고, 이번 양방향 중계와 게스트 ADB 연결을 해제했다. 호스트 ADB와 Desktop Commander를 유지했다. 두 MuMu 모두 타이틀에서 응답했다. 종료 관측은 CPU 30%, 가용 RAM 3,686/16,085 MiB였다. Windows에 프로젝트 파일을 쓰지 않았다.
