# GB2 원격 Android 시험

## SYNC42 재개용 관측 검사

`python3 scripts/verify-final-deliverables.py --android-progress 후보.apk 명세.json 이전.tsv 새묶음.tsv`는 각 파일 옆의 captures 디렉터리에서 실제 증거 해시를 검증한다. 역할별 동일 부팅, 동일 APK, 시나리오·영수증·후속 행동 조건은 발행 검사기와 공유한다. 각 TSV에는 중복되지 않는 실제 행만 넣는다. 이미 합친 표는 한 번만 넘긴다.

표준 출력에 아직 필요한 역할과 시나리오가 나오며 미완료는 종료 코드 2다. 잘못된 증거는 오류로 종료한다. 현재 필수 160개 중 84개를 확인했다. 전체 인수 후 발행할 때는 통합 observations.tsv와 기존 --android-candidate 검사를 사용한다. [현재 Android 증거](docs/coop/rune-effects-android-progress.md)

## 2026-11-28 SYNC42 Android 및 SYNC44 Cloud

두 Android 부팅의 SYNC42 설치본을 확인했다. 사전 고정한 160행 중 68개에 실제 화면·영수증·다음 행동이 연결됐다. [Android 관측](docs/coop/rune-effects-android-progress.md) [Cloud 개발 및 회귀](docs/coop/earth-guardian-results.md)

## SYNC41 Android와 SYNC42 원본 콘텐츠

SYNC41에는 기존 92건에 미감정 주문서, 세 재료·유료 조합 및 수혈 26건이 추가됐다. 설치 전 고정한 118건에서 소유 좌석의 가방·공개 전송·원래 영수증과 재인증 뒤 후속 행동을 대조했다. 추가 수혈 6/6도 통과했다. main 로그에는 순환 버퍼로 인한 기록 범위 제한이 있었다. [후속 기능 검증](docs/coop/rune-effects-results.md) [Android 인수](docs/coop/seed-alchemy-android-acceptance.md)

## SYNC40 연금술 솥

기존 60개 시나리오에 방장·게스트 각각 16개를 추가했다. 같은 물리적 룬석 스택의 제작·바닥 귀속·재인증 후 수거와 이후 확정 영수증을 추적한다. 원래 영수증 재전송 및 고의로 오래된 버전·상대 핸들을 보내는 검사는 해당 Android 호스트에 직접 인증한 격리 시험 좌석으로 실행한다. 로그는 시험 중 앱 범위에서 메모리로 읽어 Cloud에 수집한다. [실측 결과](docs/coop/cauldron-android-acceptance.md) [필수 관측 명세](docs/coop/android-acceptance-sync40.json)

## SYNC39의 실제 인수

같은 두 Android 부팅에서 이전 필수 40개와 STASIS 추가 20개를 수행했다. 재인증 후 무료 수동 해제, 만료 후 제거, 양쪽 보관과 전체·델타 비공개를 검증했다. 화면 조작과 오래된 요청·영수증 재전송용 인증 TCP를 구분해 기록한다. [관측 명세](docs/coop/android-acceptance-sync39.json) · [증거와 실행 범위](docs/coop/original-items-android-acceptance.md)

## 실제 Android 변경 기능 인수

두 기기에서 기존 데이터 보존 업데이트 직후 설치본 SHA-256과 독립 부팅 ID를 확인한다. 역할별로 실제 입력, 효과, 승인 영수증, HEAD와 상태 버전을 기록하고 양쪽 앱 재인증 후 다음 행동까지 실행한다. 마법 부여 주문서는 후보 생성 전후의 취소, 적용 대상, 앱 재시작과 두 좌석의 선택 비공개를 각각 검사한다. 발행 검사기의 필수 목록은 [SYNC38 인수 명세](docs/coop/android-acceptance-sync38.json)에 있다.

동일 요청 재전송 및 오래된 버전은 새로 등록한 격리 시험방의 두 시험 좌석을 사용한다. 개인 방과 좌석 자격은 사용하지 않는다. OS 크래시 및 ANR과 함께 앱 UID의 ART·LinkageError·치명적 GLThread 기록, 호스트 응답, 대기 중 요청과 실제 영수증을 확인한다. FAIL·NOT RUN·영수증 누락이 있는 필수 항목은 ZIP 발행을 보류한다. [실측 결과](docs/coop/stasis-warp-android-acceptance.md)

## 2026-11-27 SYNC37 배포와 SYNC38 개발

게임 로그의 VerifyError가 스케줄러를 멈춘 경우 앱의 FATAL EXCEPTION이나 ANR이 없어도 호스트 응답·영수증·다음 행동을 대조한다. 별도 프로세스의 종료 신호는 패키지와 시각으로 식별한다. [실제 Android 인수](docs/coop/enchantment-android-acceptance.md)

## SYNC36 실제 Android와 기존 Drive 파일

09c9417 해시에 대한 사용자 한정 승인으로 기존 APK를 한 번 교체하고, Android 인수 뒤 같은 소스의 통합 ZIP을 한 번 교체했다. 기존 추가 편집자와 링크 공유 권한을 보존했다. 두 MuMu 내부 Chromium 다운로드와 데이터 보존 업데이트, 로그와 화면의 Cloud 수집을 사용했다. 다음 빌드의 사전 발행은 별도 승인 대상이다. [개발 및 시험 범위](docs/coop/enchantment-results.md) · [Android 인수](docs/coop/shared-growth-android-acceptance.md) · [배포 파일 대조](docs/coop/shared-growth-delivery.md)

## SYNC35 후보와 현재 두 설치본

GB2의 두 Android 15 부팅에서 실제 설치본은 모두 9969d23이었다. 5559와 16448은 같은 인스턴스였고 16480은 별도 인스턴스였다. 이번에 추가한 연결을 정리한 뒤 두 MuMu가 응답했고 기존 5559 연결을 확인했다. 새 후보의 비공개 전달 경로를 확보하지 못해 설치와 Android 인수는 미실행이다. 기존 9969d23 예외는 사용 완료됐다. 다음 설치는 정확한 후보 해시, 기존 데이터 보존 및 해당 발행에 필요한 승인을 확인한 뒤 진행한다. [두 설치본·후보 해시와 측정값](docs/coop/tier-growth-results.md)


## 9969d23의 일회성 전달 기록

사용자와 플랫폼의 승인을 받아 기존 APK를 한 번 갱신했다. 두 MuMu 내부 Chromium에서 비로그인으로 받고 다운로드 해시와 설치된 base.apk를 각각 확인했다. 두 다운로드의 경로·해시를 확인해 정리하고 양쪽 타이틀과 호스트 ADB를 유지했다. 이 예외는 사용 완료됐다. [실제 인수 및 GB2 종료 상태](docs/coop/selector-android-acceptance.md)

## 후보 전달과 Android 인수

시험 전 기존 MuMu의 실제 부팅 ID·설치본 해시·앱 전용 방과 좌석 목록을 확인한다. Cloud 후보는 기존 서명·패키지·SHA-256을 검사한다. 먼저 승인된 비공개 Android 전달 기능이 실제 제공되는지 확인한다. 이번 성서 수정 시험에서는 Desktop Commander의 대용량 바이너리 메모리 입력을 자동 승인 검토가 중단했다. 이 경로는 승인된 전달 방법 목록에 포함하지 않는다. 관리 APK 교체를 통한 대안은 해당 작업의 갱신 1회가 남고 검증 전 발행 예외가 적용될 때 사용할 수 있다. 이 작업에서는 그 교체도 자동 승인 검토에서 거절됐다. 같은 수정 후보는 새 프롬프트에서도 기존 전송 횟수를 이어받는다. 별도 비공개 전달 기능이 없으면 사용자가 기존 APK ID에 추가 1회 검증 전 발행을 명시적으로 승인하고 플랫폼 승인도 확보한 뒤 해당 바이트를 전달한다. [현재 두 기기와 관측 결과](docs/coop/trinity-discovery-results.md)

다운로드와 설치된 base.apk를 각각 검사하고 설치 전후 파일 목록과 최초 설치 시각을 대조한다. 원래 크래시 방 또는 완전히 분리한 검증 방에서 메뉴 → 물품 창 → 취소 → 재선택 → 실제 시전 → 저장 → 양쪽 재인증 → 다음 행동을 점검한다. 역할·언어·빌드별로 증거를 기록하고 동일 구간의 AndroidRuntime과 ANR을 확인한다. 시험 종료 시 이번에 받은 정확한 다운로드와 전용 매핑만 정리한다.


최종 보스와 아뮬렛 화면의 한국어·영어 Desktop 증거는 독립 자료다. 새 빌드의 Android 결과는 설치된 base.apk 해시, 기존 저장 목록과 재개 여부를 기준으로 기록한다. [최종 보스·승리 검증 기록](docs/coop/yog-victory-results.md)

## 광산·도시 체크포인트

Drive 미리보기에서 다운로드 버튼이 보이지 않을 때도 동일한 승인 파일 ID에 대한 공식 drive.usercontent.google.com/download 경로를 MuMu 안에서 확인한다. 설치 파일의 크기와 SHA-256을 확인한 뒤 Android 안에서 업데이트하고 이번 다운로드만 정리한다. [실행 기록](docs/coop/mines-city-results.md)

Codex가 승인된 Remote Desktop Commander를 통해 필요한 변경 회귀를 직접 수행한다. Cloud에서 Git·Gradle·JUnit·장시간 시험·서명 검사·Drive 발행을 처리한다. GB2는 기존 MuMu와 로컬 ADB로 짧게 시험한다. 별도 Work 작업이나 사용자의 수동 설치 절차가 필요하지 않다.

1. 지정된 GB2의 연결, 메모리 여유, 기존 인스턴스, 로컬 ADB 연결·매핑과 부팅 식별자를 확인한다. 기본 Device-2만 사용한다. 동일 부팅을 보고하는 포트는 한 기기다. 두 번째 인스턴스에는 별도 승인과 자원 확인이 필요하다. 자원 부족 징후가 나타나면 작업을 멈춘다.
2. 설치 패키지·버전·서명과 base.apk 해시를 확인한다. 해시가 같으면 다운로드를 건너뛴다. 호환되는 업데이트는 기존 설치·저장 데이터를 보존한다. 전용 Android 데이터에 대한 파괴적 시험은 사용자 저장본과 격리한다.
3. Android 내부 브라우저로 지정된 최신 APK를 로그인 없이 내려받아 바이트 수와 SHA-256을 비교한다. 익명 다운로드, 호환 서명 또는 설치 권한이 막히면 사유를 기록한다. Windows에 APK·소스·스크립트·스크린샷·로그를 쓰거나 공유 폴더를 경유하지 않는다. 안드로이드 내 설치 파일의 크기와 SHA-256을 대조하고 정상 설치 UI를 사용한다. 설치 후 버전·base.apk 해시를 다시 확인한다.
4. 표준 출력과 메모리 스트림으로 결과를 Cloud에 수집한다. 시험과 관련된 로그만 수집하며 인증값과 개인 세이브를 제외한다. 디스플레이·성능·LAN 결과는 실제 사용 환경을 표시한다.
5. 이번에 만든 매핑·리스너·프로세스·Android 다운로드만 경로 또는 ID로 확인해 정리한다. 사용자 폴더 전체를 검색하거나 기존 파일을 삭제하지 않는다. 기본 인스턴스와 원격 연결, 설치 앱 및 저장 데이터는 유지한다. 종료 보고에는 실제로 확인한 범위를 적는다.

최종 Drive 발행은 지정된 기존 두 파일의 내용만 세션당 각 한 번까지 갱신한다. 먼저 현재 revision의 복구 가능성과 원격 소스 커밋을 확인한다. 비공개 경로가 없을 때에는 해당 작업의 남은 횟수와 검증 전 발행 예외를 먼저 확인한다. 신규 Android 실측에는 신규 APK 설치 해시가 필요하다. [백업 정책](BACKUP-AND-DELIVERABLES.md)

최신 APK와 통합 ZIP 두 파일만 기존 ID에 링크 뷰어와 다운로드를 허용한다. 비로그인 다운로드의 실제 파일 크기와 SHA-256을 별도로 확인한다. [발행 절차](BACKUP-AND-DELIVERABLES.md)
