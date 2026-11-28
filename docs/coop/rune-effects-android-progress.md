# SYNC42 Android 인수 중간 기록

필수 68/160개에 새 Android 관측을 연결했다. 기존 필수 항목은 50/118, SYNC42 추가 항목은 18/42였다. 실제 두 Android 부팅에서 설치본·화면·후속 행동·HEAD와 원래 좌석의 영수증을 대조했다. 나머지 92개는 인수 증거가 미완료다. 발행 검사기는 누락된 시나리오를 열거하며 실패했다. ZIP 발행 조건은 충족하지 않았다. [사전 고정 명세](android-acceptance-sync42.json)

| 범위 | 실제 증거가 있는 항목 / 필수 |
| --- | ---: |
| 성서 선택 | 10/10 |
| 마법 부여 주문서 | 10/28 |
| STASIS | 8/22 |
| 기존 솥 | 22/32 |
| 기존 미감정·씨앗·유료·수혈 | 0/26 |
| SYNC42 추가 | 18/42 |

## Android에서 관측한 효과

각 좌석에서 성서 물품 선택의 취소·재선택과 확정 뒤 앱 재실행 및 다음 행동을 확인했다. 방장은 미감정 마법 부여 주문서를, 게스트는 공유 식별 이후의 주문서를 읽었다. 자기 활의 세 후보를 띄운 채 재실행하고 동일 후보를 확정했다. 타 좌석 화면에는 다른 영웅의 후보창이 표시되지 않았다.

서로 다른 성직자가 소환한 두 LightAlly의 Actor ID는 23과 26이었다. 각 HP는 80이었다. LifeLink 후 양쪽 성서의 충전은 3이었다. 보관하면 각 1이 되고 양쪽 앱을 다시 연 뒤 게스트부터 풀어도 다른 소환수는 계속 보관됐다. 두 소환수 모두 원래 ID·HP·시전자 연결로 돌아왔고 각 성서의 충전은 1이었다.

신규 양쪽 역할의 개별 관측은 아래 목록에 기록했다. 기존 Android 118/118 및 별도 수혈 6/6은 SYNC41에서 수행했다. 이번 SYNC42의 미감정 상세창 회귀와 추가 수혈은 미실행이다.

- stone-fear
- stone-worn-augmentation
- potion-mindvision
- corruption-conversion
- exotic-mindvision-four
- exotic-terror-six
- read-mapping
- recall-mapping
- previous-sync41-room

완료 행마다 실제 다음 행동의 영수증이 있고 해당 시험 좌석의 보존 요청 수는 0이었다. 수집한 로그에서 게임 프로세스의 VerifyError, LinkageError, 치명적 GLThread 예외, 크래시 및 ANR을 관측하지 않았다. 초기 native fatal signal 네 건은 PID와 cmdline을 대조한 결과 uiautomator에서 발생했다. main 순환 버퍼는 전 구간을 연속 보존하지 못했다. 시험은 GB2의 Windows 루프백 ADB 중계와 Android 15 MuMu 두 대에서 수행했다.

## 발행 및 재개

APK는 6200b55b3d2e6c9a23a076a124b4f19168db3829, 35,410,029바이트, SHA-256 d46f64e1523b9d7ec667035f793b20472dc48ab98defd08f0d323a9217de6f89다. 두 MuMu 내부 Chromium의 비로그인 다운로드로 기존 데이터를 보존해 설치했다. 방장 부팅 7867132f-4ba4-4a71-abe0-b096e52cdc07와 게스트 부팅 8ce63420-1712-4ee2-a895-b5568acebf55에서 각각 같은 base.apk 해시를 측정했다. 개발 인증서, 패키지 org.spdcoop.dungeon.indev, versionCode 922, classes2.dex 형식 지정 배열 저장을 확인했다.

한정 승인으로 기존 Drive APK 내용을 한 번 교체했다. ZIP 갱신 0회, 신규 파일 0개다. 비로그인 다운로드한 기존 ZIP은 7ba9bb3bfd0d876446d55c400a7fea32fb691266 소스, 77,693,505바이트, SHA-256 1b1130b3fca9ac0c3e007158f1302df6a7bf4275424cbb4e3e2068301174b643이다. 기존 ID와 권한을 유지했다.

종료 시 GB2 CPU 41%, 가용 RAM 1358/16085 MiB였다. 가용 RAM은 시험 시작 시 1,777 MiB에서 감소했다. 추가 시험 부하를 중단하고 두 MuMu가 타이틀에서 응답하는 것을 확인했다. 이번 작업에서 내려받은 Android 파일 두 개와 임시 연결을 정리했다.

Cloud 후보 보존 디렉터리의 android-20261128-partial에 observations.tsv, required-status.tsv, captures, gate.txt, SHA256SUMS.txt를 보존했다. 다음 시작점은 기존 두 설치본의 서명을 유지한 채 미감정 자장가·재충전 전후의 가방 상세와 공개 투영을 양쪽에서 다시 여는 것이다. 이어서 오래된 요청 거부·영수증 재전송, 세 재료 및 수혈을 재실행한다. 시험 저장과 인증 정보는 공개 저장소에 넣지 않았다. [후속 개발판](earth-guardian-results.md)
