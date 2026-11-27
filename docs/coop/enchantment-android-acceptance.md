# SYNC37 Android 인수 결과

**FAIL.** 두 Android 15 설치본에서 기존 성서 선택 회귀는 통과했다. 마법 부여 주문서를 읽으면 호스트가 응답을 멈췄다. 게스트도 확정 영수증을 받지 못했다. 배포 APK만 한 번 교체했고 대응 ZIP 교체 조건은 충족되지 않았다.

| 확인한 파일 | 빌드 | 바이트 | SHA-256 | 이번 Drive 갱신 |
|---|---|---:|---|---:|
| APK | 27663fc, SYNC37 | 37,350,733 | 75db0d5b86d7c62ba94d638129d3faa8c1152c6ad9b9613fe81b3e90cadd24bd | 1 |
| ZIP | 09c9417, SYNC36 | 74,999,032 | a12d81368c49c2ba17fdc1e3bb09c8fed5f21997378e2caa726f93e3a6e771c4 | 0 |

기존 파일 ID로 비로그인 다운로드한 두 파일을 확인했다. 기존 링크 권한은 anyone/reader, allowFileDiscovery=false다. APK의 기존 개별 편집자 권한을 포함해 공유 설정은 유지했다. 새 Drive 파일 생성은 0회다. 패키지는 org.spdcoop.dungeon.indev, versionCode는 922, 인증서 SHA-256은 0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53이다. [APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) · [ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)

두 MuMu의 내부 Chromium에서 내려받아 데이터를 보존해 설치했다. 서로 다른 부팅 ID와 두 base.apk의 해시는 관측표에 기록했다. 기존 개발 인증서와 최초 설치일을 확인했다. 격리 시험방에서 양쪽 역할이 메뉴를 열고 취소·재선택한 후 실제 주문을 확정했다. 양쪽 앱 재시작, 기존 좌석 재인증, 다음 행동까지 확인했다. [기기별 관측 10행](enchantment-android-observations.tsv)

| 주문 또는 형태 | 방장 | 게스트 | 성서 충전 |
|---|---|---|---|
| HolyIntuition | PASS | PASS | 3 → 0 |
| RecallInscription | PASS | PASS | 3 → 0 |
| Trinity BodyForm | PASS | PASS | 4 → 2 |
| Trinity MindForm | PASS | PASS | 4 → 1 |
| Trinity SpiritForm | PASS | PASS | 4 → 0 |

마법 부여 주문서에서는 격리 방의 실제 소지품 창에서 읽기를 눌렀다. 호스트·게스트 재접속 및 재시도 후에도 대상·후보 선택을 완료할 수 없었다. 기기의 ART가 classes2.dex의 CoopEnchantScroll.select를 검증할 때 0x63의 필드 저장에서 Object[]를 Bundlable[]로 사용할 수 없다는 VerifyError를 냈다. 해당 클래스를 처음 로드하는 읽기 단계에서 중단되므로 세 후보의 표시·확정·취소 및 대기 중 선택 복원은 NOT RUN이다. 한국어·영어 Desktop 및 해당 후보의 JUnit 364개에서는 이 오류가 드러나지 않았다.

시험 구간에 게임 패키지의 치명적 AndroidRuntime/GLThread 기록과 am_anr은 각각 0건이었다. 다운로드 조회용 Android 셸 프로세스에서 남긴 네이티브 종료 신호는 프로세스 이름과 시각으로 식별했다. 게임 호스트의 VerifyError와 응답 유실은 별도로 FAIL이다. 성서 인수 TSV 검사기는 후보 해시·두 부팅·10행을 통과했다.

EAGLE_EYE 3점이 배치된 게스트 사냥꾼이 실제 SpiritHawk를 소환했다. 갑옷 50 → 15와 두 화면의 매를 확인했다. 특성 투자 입력과 장애물 뒤 시야 반경, 몸 바꾸기, 신규 성직자 주문, 성지·광벽 겹침, Kinetic, 분해 지팡이, Trinity 화염 발사 및 광전사 부활의 SYNC37 기기 인수는 NOT RUN이다. 가방 속 망토·성서, 소환수 교환, 망령 귀속, Blocking 보호막도 이번 기기 인수에 포함되지 않았다.

종료 시 양쪽 앱을 타이틀로 되돌렸고 MuMuNxDevice 두 프로세스가 모두 응답했다. CPU 4%, 가용 RAM 2,725/16,085 MiB였다. 이번 시험용 Android 다운로드 두 건과 임시 ADB 매핑 및 보조 연결을 정리했다. Windows에는 프로젝트 파일을 작성하지 않았다. 이번 네트워크 경로는 Windows 루프백 ADB 중계다. 물리 휴대폰 Wi-Fi와 핫스팟은 NOT RUN이다.
