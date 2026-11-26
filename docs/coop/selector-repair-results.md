# 성서 물품 선택 크래시와 Android 인수

**Android 배포 오류는 열려 있다.** 배포판의 한국어 게스트에서 신성한 직감을 누르면 GLThread가 종료됐다. 같은 소스의 Desktop 게스트에서도 가방을 열기 직전에 `Group.add`의 `members`가 null인 예외를 재현했다. 원본 `WndOptions`는 선택을 전달하기 전에 자신을 닫는다. `9969d23`은 새 가방 창을 살아 있는 `CoopScene`에 붙인다. [원래 Android 기록](seal-arts-android.md)

| 파일 | 실제 빌드 커밋 | 바이트 | SHA-256 |
| --- | --- | ---: | --- |
| Drive와 두 MuMu 설치본 | `07b18899f48b22142a602bbf095cddcef621a033` | 35,369,779 | `4e580728fa22a73ee748f749e154edfe3c695e5aaeacbe9f1abe388269b8ef35` |
| Cloud에 보존한 수정 후보 | `9969d235a85c57019ba2963f873ab492367af164` | 37,306,668 | `66e1da255f50026f91db5cf89f9633842782a72e28b0f4191758cb0a428a1547` |

두 APK의 실행 소스는 이 검증 작업에서 변경하지 않았다. 두 파일의 패키지는 `org.spdcoop.dungeon.indev`, 버전 코드는 922다. 수정 후보의 v1·v2 서명자 SHA-256은 `0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53`이다. 양쪽 APK에서 추출한 DEX로 `CoopScene$3.add`가 `CoopScene.add`로 바뀐 것을 확인했다. 분리된 인장의 번역 이름도 수정 소스에 들어 있다.

ZIP 엔트리는 각각 575개다. 압축 페이로드 합은 35,280,593에서 35,280,815바이트로 222바이트 증가했다. 수정본의 로컬 ZIP 레코드 사이에 1,885,705바이트와 50,962바이트의 빈 구간이 있다. 증분 패키징에서 `classes2.dex`가 재배치된 레이아웃과 일치한다. 두 빈 구간과 압축 페이로드 변화의 합이 실제 크기 차이 1,936,889바이트다. 후보는 보존본 그대로 검사했다.

## 실제 입력과 저장

| 기능 | Desktop 한국어 / 영어 | TCP에서 대조한 효과 | 수정 APK Android 방장 / 게스트 |
| --- | --- | --- | --- |
| HolyIntuition | 양쪽 역할 PASS | 자기 갑옷의 저주 정보 공개, 강화 미공개, 자기 성서 차감 | NOT RUN / NOT RUN |
| RecallInscription | 양쪽 역할 PASS | 실제 감정 효과, 각인 추적기 종료, 자기 성서 차감 | NOT RUN / NOT RUN |
| Trinity BodyForm | 양쪽 역할 PASS | 자기 Swiftness 참조 선택, 원래 갑옷 형상 발동 | NOT RUN / NOT RUN |
| Trinity MindForm | 양쪽 역할 PASS | 자기 번개 지팡이 참조 선택, 원래 후속 발사 | NOT RUN / NOT RUN |
| Trinity SpiritForm | 양쪽 역할 PASS | 자기 신속의 반지 참조 선택, 후속 반지 효과 | NOT RUN / NOT RUN |

Desktop에서 실제 가방 → 성서 → 주문 → 대상 가방을 열었다. Escape로 취소하면 최초 권위 스냅샷을 유지했다. 별도 시도에서 창을 열어 둔 채 TCP 대기로 상태를 바꾼 뒤 오래된 항목을 눌렀다. 주문 제출과 성서 차감 모두 0회였다. 새 창에서 현재 물품을 고른 뒤 성공 영수증과 충전 차감을 확인했다. Android에서 종료됐던 방과 같은 시험 UUID의 원래 Cloud 생성 세대 사본에서도 수정된 한국어 게스트 입력과 재접속 후 행동이 통과했다. 이 사본의 진행 시점은 기기에서 크래시가 발생한 마지막 세대와 다르다.

20개 시나리오(두 역할 × 두 언어 × 다섯 경로)에서 선택창·시전 후·복원 후 다음 행동 화면 60장을 보존했다. 방장은 저장 후 방을 닫고 원래 두 좌석을 재인증했다. 게스트는 선택창이 열린 채 연결을 닫고 원래 좌석으로 돌아와 시전했으며, 다시 접속해 성서 충전과 다음 대기 영수증을 대조했다. 별도 한국어 게스트의 신성한 직감과 영어 게스트의 BodyForm은 OS 마우스의 빠른 두 번 누르기로도 확정 영수증이 하나였다. 같은 두 경로에서 열린 창을 세로 Desktop 창으로 전환한 후 원래 크기로 돌리고 취소와 재선택을 완료했다.

`CoopScene`의 성서·인장·주입·금고 보상, `WndSeatInventory`의 슬롯·탭 전환, `WndSeatTrade`와 `CoopHud`의 후속 창을 점검했다. 성서의 다섯 경로가 같은 콜백을 사용한다. 페이지 변경 시 게임 장면은 기존 Window를 닫고, 같은 페이지의 오래된 선택은 스냅샷 동일성 및 호스트의 소유권 검사로 거부된다. Android 백그라운드·회전·연타와 앱 종료 뒤 재인증은 수정본에서 NOT RUN이다.

## 복구와 혼합 안정성

기준 커밋에서 기존 JUnit 278/278을 재실행했다. 최종 전체 시험은 279/279 PASS였다. XML 74개에서 기존 클래스·메서드 이름 278개가 모두 일치했고 실패·누락·건너뜀이 각각 0개였다. 새 시험은 두 좌석과 다섯 선택 경로에서 타인의 성서와 사라진 물품 핸들을 거부한 후 자신의 유효한 물품으로 다시 실행한다. 발행 전 검사기의 별도 Python 시험 4개도 통과했다.

독립 TCP 호스트에서 HEAD 발행 후 성공 응답 직전에 JVM을 종료했다. 다시 띄운 호스트에서 두 좌석을 인증하고 같은 요청의 영수증을 대조했다. 신성한 직감, 각인 회수, Body 선택·발동, Mind 발동, Spirit 발동을 양쪽으로 재실행한 12건과, Mind·Spirit의 **선택 확정 자체**를 분리한 신규 4건이 통과했다. 기존 180개 경계 가운데 이번에 다시 실행한 수는 12개다.

이전 배포 작업에서 생성한 격리 저장 6개를 각각 새 JVM으로 읽었다. 실제 내부 스키마는 21이 5개, 19가 1개이며 모두 층 스키마 2였다. 각 사본의 두 티켓에 기록된 좌석·플레이어·epoch, 다음 WAIT와 재저장 스키마를 대조했다. 게임 저장 범위는 1~21, 층 스키마 1~15, SYNC32, LAN v1이다.

단일 JVM에서 성서·형상 선택·형상 발동을 포함한 16개 방을 열고 닫았다. 351개 요청이 확정됐고 별도로 거부 1회, 중복 영수증 16회, 게스트 재인증 32회를 기록했다. 지연 중앙값·p95·p99는 32.353·80.893·120.632ms였다. GC 후 힙은 1·8·16번째 방에서 6,123,432·7,000,368·7,045,584바이트였다. 마지막 관측에서 방 잔류 참조·Actor·LAN worker는 각각 0, 스레드 7, FD 20, 소켓 1이었다. 16개 방의 짧은 표본이다.

## GB2와 발행

Device-2의 127.0.0.1:5559와 16448은 같은 Android 15 부팅이었다. Device-2-1의 127.0.0.1:16480은 다른 부팅이었다. 호스트·게스트 모두 위 Drive 해시였다. 기존 시험방 6개를 보존하고, 다섯 경로마다 두 성직자가 있는 독립 시험 프로필을 추가했다. 시험 세대·개인 좌석·인증 자료는 공개 증거에서 제외했다.

후보 파일의 비공개 전달을 시도했지만 Remote Desktop Commander를 통한 대용량 메모리 입력은 자동 승인 검토가 거절했다. 일부 입력을 받은 시험 프로세스는 설치 전 종료했다. 기존 Drive APK ID 교체도 자동 승인 검토에서 같은 발행 작업의 APK 1회가 이미 소비됐고 수정 빌드의 Android 인수가 없다는 이유로 거절됐다. 이번 후속 작업의 실제 갱신은 APK 0회·ZIP 0회다. 이전 발행에서 각각 1회를 사용했다. 후보에서 측정한 Android 크래시·ANR 건수는 없다. 기기의 기존 빌드에서 확인한 구간에는 신규 크래시·ANR 이벤트가 없었다.

정리 직후 CPU 4%, 가용 RAM 2,670 MiB를 관측했다. Cloud 회귀 중 다시 확인한 최종 관측은 CPU 35%, 가용 RAM 2,332 / 16,085 MiB였고 두 MuMu 모두 start_finished였다. 이 시험의 forward·reverse와 새 ADB 연결을 정리하고 루프백 시험 리스너 0개를 확인했다. 기존 호스트 ADB와 Desktop Commander를 유지했다. Windows에 프로젝트 파일을 기록하지 않았고 Android에 후보 다운로드 파일을 만들지 않았다.

비로그인으로 받은 기존 APK는 위 배포본과 일치했다. 기존 통합 ZIP은 73,920,737바이트, SHA-256 `9218e329b5a66af74d75cb4c572fab768eed5fd48ac04dbfb4213dd3c13d1452`다. 기존 두 파일에서 링크 공개 권한 `anyone/reader`, `allowFileDiscovery=false`를 확인했다. 통합 ZIP의 소스 2,338개를 `07b1889`와 대조하고 체크섬 2,736개를 검사했다. 직전 리비전의 `keepForever=false`는 영구 보존 보장이 없다. [관리 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) · [관리 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)

## 성장과 정상 진행

투자 허용 코드를 원본 초기화 목록과 대조했다. 여섯 직업에서 1단계 16/24, 2단계 19/30, 3단계 3/48, 4단계 9/72로 총 47/174개 슬롯이다. 같은 특성이 다른 갑옷에서 반복되는 슬롯이 있다. 지원표에서 원본 존재, 호스트 실행, 투자점 시험 범위, 플레이어 UI, TCP, 저장·복구, Android를 별도 열로 기록했다. `NOT_RUN`은 해당 열의 근거를 확인하지 못했다는 뜻이다. 검투사의 CLEAVE·LETHAL_DEFENSE·ENHANCED_COMBO, 감지 두 종, 지팡이 보존 등에는 기존 시험 범위를 연결했다. [특성별 근거](seal-arts-talents.tsv)

Trinity는 Body 2/48, Mind 1/41, Spirit 1/24다. 과거의 좌석별 발견 이력만으로 가방 밖 대상을 선택하는 경로는 미구현이다. 일반 갑옷 18종에는 기존에 검증된 최소 효과가 있다. ElementalStrike는 무부여·Blazing·Chilling, 지팡이는 기존 다섯 종류가 지원 범위다. 이번 체크포인트는 수정 APK의 Android 인수가 남아 있어 배포 대상으로 새 특성·주문·지팡이를 추가하지 않았다. 일반 방은 가면·왕관 OFF, 경험치는 마지막 타격이다. [Trinity 후보](seal-arts-trinity.tsv) · [갑옷 하위 효과](seal-arts-results.md)

시드 572의 실제 행동 343 저장(스키마 12)을 읽었다. 키 정렬, UTF-8, 공백 없는 JSON의 SHA-256은 `dad9e51590dfe7420760ce8a11779e5467f2185efcaf8760a2b7d595c8dc7e01`이다. 각 안내기는 해당 좌석의 공개 상태만 보고 다시 출발했다. 160요청 예산에서 safeexit와 handoff는 각각 159행동·150점·방장 HP 0/게스트 13이었다. 새 crossonce 방식은 자신이 밟은 문과 직전 접근 칸을 기억하고, 안전한 칸에서는 즉시 같은 문으로 돌아가는 이동에 낮은 우선순위를 줬다. 동료가 보이면 추격했고 위협 중 저체력일 때 바닥 전리품을 보류했다.

같은 시작 저장에서 crossonce는 159행동·150점·HP 13/6으로 두 좌석이 살아남았다. 관찰한 링 방(51,11)의 같은 출입구 870으로 방장과 게스트가 각각 두 번째·일곱 번째 확정 행동에 들어갔다. 방장은 116번째에 같은 문으로 나왔다. 게스트의 다른 출구 퇴장, 양쪽의 방 통과, 토큰 문·퍼즐, 4,000점은 달성하지 못했다. 마지막 안전 상태와 종료 시점의 엔진 저장은 비공개 Cloud에 분리해 보존했다. 종료 상태의 정규화 SHA-256은 `b9e02639a23d1f582a362b2e2a3aaff310000ba987ce0bb635400aae10501eb8`이다. 전체 JUnit의 기존 정상 시작 TCP 경로에서 시드 581은 감옥 진입까지 1,155행동·동의 5회·HP 7/30, 시드 1105는 Goo 전리품 2개·1,018행동·HP 20/30을 확인했다. 두 시드의 정상 Tengu 처치, 신규 경험치 비교 및 별도 12시드 배치형 승리·Ascension 장거리 재주행은 NOT RUN이다.

다음 시작점은 보존한 `66e1da25…` 후보를 실제 설치할 수 있는 승인된 전달 경로다. 두 base.apk를 확인한 후 다섯 시험방 각각에서 방장·게스트의 취소·재선택·시전·충전·저장·재인증·다음 행동을 기록한다. 첫 기능 확장 대상은 아직 발생 이력을 소유자에게 귀속할 수 있는 감정·장비 발견 경로와 기존 저장의 이행이다. 기존 공통 지식만 있는 저장에서는 과거 발견자를 추측할 수 없다.

## 재현 도구

* Desktop의 `CoopRenderSmoke SCREENSHOTS selector-intuition`으로 방장 입력을 실행한다. 마지막 인자는 `selector-recall`, `selector-body`, `selector-mind`, `selector-spirit`로 바꿀 수 있다. 언어는 JVM의 `-Duser.language=ko` 또는 `en`으로 설정한다. 게스트 시험에는 `AndroidStagedGrowth TRIAL FRESH_QA_OUTPUT selector-이름`으로 만든 프로필의 **새 비공개 작업 사본** 경로를 `-Dspd.selector.guestProfile=경로`로 지정한다. 사본에는 해당 방의 seat0와 seat1만 둔다. 실제 앱 저장을 입력으로 사용하지 않는다.
* TCP 응답 유실 시험은 `python3 scripts/verify-dart-growth.py CORE_TEST_CLASSPATH --actions intuition recallid trinityselect trinitymindselect trinityspiritselect trinity trinitymind trinityspirit`로 두 역할을 실행한다. 혼합 표본은 기존 `MixedRoomStability 16 22`에 `-Dstability.selectors=true`를 지정한다.
* 금고는 `VaultPublicRoute 572 160`, `-Dvault.guide.team=true`, `-Dvault.guide.regroup=crossonce`, `-Dvault.guide.resume=이전_저장`으로 재개한다. `-Dvault.guide.endCheckpoint=새_파일`로 종료 상태를 비공개로 저장할 수 있다. 후속 실행은 그 저장과 정규화 해시를 먼저 대조한다.
