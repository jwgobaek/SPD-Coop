# SYNC39 Android 인수 — 2026-11-27

**필수 60/60 PASS.** 기존 40개(성서 선택 10, 마법 부여 주문서 28, 게스트 STASIS 2)를 모두 새로 실행했고 STASIS 20개를 추가했다. SYNC38의 39/40 실패 기록은 [이전 인수](stasis-warp-android-acceptance.md)에 남아 있다.

| 항목 | 실제 관측 |
|---|---|
| APK | `2fa1a0ba8793c8e0ffda87f5d6f21be13965a54e`, 35,398,518바이트, SHA-256 `d059320faf181611ea5b702e2aa82d4dcbf8d2cdae3dd88920fc3928c645cb0b` |
| 설치 | GB2의 Android 15 두 대에서 Chromium 다운로드와 데이터 보존 업데이트. 양쪽 base.apk가 위 해시와 일치 |
| 부팅 ID | 방장 `7867132f-4ba4-4a71-abe0-b096e52cdc07`, 게스트 `8ce63420-1712-4ee2-a895-b5568acebf55` |
| 성서 선택 | 방장·게스트 각각 HolyIntuition, RecallInscription, BodyForm, MindForm, SpiritForm. 취소, 재선택, 효과, 재인증 및 후속 행동 |
| 마법 부여 주문서 | 역할별 14개. 습득, 미감정·감정 읽기, 두 취소 시점, 활·가방 무기·착용 무기·착용 갑옷·인장 갑옷, 재실행 후 세 후보, 후보 비공개, 오래된 선택, 중복 영수증 |
| STASIS | 역할별 11개. 보관, 수동 해제, 재인증 후 메뉴, 해제 표시 제거, 상대 좌석 비공개, 두 소환수의 독립 해제, 착지 불가 거부, 자동 만료 재시도, 만료 표시 제거, 중복 해제, 보관 중 층 이동 및 복원 |

두 성직자의 실제 LightAlly ID는 23·26, HP는 각각 80/80이었다. 기존 좌석으로 두 앱을 다시 연결하고 한국어 성서 메뉴에서 무료 해제를 확정했다. 보관 시 성서 3→1, 해제 시 1→1이었다. 원래 소환수 ID와 HP, PowerBuff 24·27, 시전자 1·16 및 양측 LifeLink를 대조했다. 후속 행동이 확정됐고 대기 요청은 0이었다.

별도 시험에서는 해제 순서를 뒤집었다. 양쪽 역할의 보관·해제 전후 전체 및 델타 스냅샷 8쌍에서 소유자에게만 stasisheld 추가·제거가 전달됐다. 빈칸이 없을 때 양쪽 수동 요청은 REJECT_INVALID였다. 자동 만료는 1틱씩 재시도했고 착지가 가능해진 뒤 동일 Actor가 돌아왔다. 각 역할의 별도 층 이동 시험에서는 두 좌석이 동의한 뒤 2층에서 보관 상태로 재인증했다.

16개 격리 시험 프로필에서 정상 입력은 게임 화면으로 실행했다. 오래된 요청·중복·스냅샷 비교에는 같은 Android 호스트의 인증 TCP 경로를 썼다. 시나리오별 원래 영수증, 상태 버전, HEAD, 후속 영수증, 화면, 캡처 SHA-256은 통합 ZIP의 `evidence/android-sync39`에 있다. 개인 저장과 좌석 인증 자료는 제외했다.

조회한 양쪽 Android 로그에서 VerifyError, LinkageError, 치명적 GLThread 예외, 프로세스 크래시 및 ANR은 모두 0건이었다. main 로그의 앞부분은 순환 버퍼에서 교체됐다. crash 및 system/events 버퍼, 각 시나리오의 실제 응답과 후속 행동을 함께 확인했다. 시험은 Windows 루프백 ADB 중계와 격리된 배치형 방에서 수행했다. 실제 휴대폰 Wi-Fi·핫스팟 시험은 NOT RUN이다.

[필수 관측 명세](android-acceptance-sync39.json) · [공개 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) · [대응 소스 및 Android 증거 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)

비로그인 다운로드는 APK 35,398,518바이트, ZIP 73,548,513바이트였다. ZIP SHA-256은 `9dc3c4aad54bb13a2874f837765dc1921c7ce73000a35201ffee0834e8aaa4b1`이다. 두 MuMu가 타이틀 화면에 돌아왔고 정리 시 CPU 10%, 가용 RAM 1,685/16,085 MiB였다. 시험 종료 후 다시 조회한 GB2는 CPU 18%, 가용 RAM 1,800/16,085 MiB, MuMuNxDevice 두 프로세스(2552·26728)였다. 양쪽 시험 다운로드 및 이번 임시 ADB 중계를 정리했다.
