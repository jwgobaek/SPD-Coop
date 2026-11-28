# 최종 검증판 발행

## SYNC42 인수 재개 및 별도 Cloud 후보

Drive 누적은 APK 1회, ZIP 0회, 신규 0개다. 현재 대응 ZIP은 SYNC41 소스 7ba9bb3이며 SYNC42 전체 필수 증거와 승인 확보 후 6200b55 소스로 구성한다. SYNC46·47은 Cloud에 보존했다. [검증 범위와 실제 저장](docs/coop/powered-summon-results.md)

## 2026-11-28 SYNC42 Android 재개 및 SYNC45 Cloud

기존 파일을 비로그인으로 내려받아 APK 35,410,029바이트(SYNC42, 6200b55), ZIP 77,693,505바이트(SYNC41, 7ba9bb3)의 SHA-256을 확인했다. 이번 각 파일의 갱신은 0회다. 누적 APK 1회, ZIP 0회, 신규 파일 0개다. ZIP 발행 조건은 SYNC42의 160개 필수 관측 통과다. [Android 관측](docs/coop/rune-effects-android-progress.md) [Cloud 검증](docs/coop/regrowth-warding-results.md)

## 2026-11-28 SYNC42 Android 및 SYNC44 Cloud

기존 APK 1회, ZIP 0회, 신규 파일 0개다. Drive APK는 6200b55, 기존 ZIP은 7ba9bb3에 대응한다. 남은 필수 Android 인수가 완료된 뒤 6200b55의 대응 ZIP을 구성한다. [Android 관측](docs/coop/rune-effects-android-progress.md) [Cloud 개발 및 회귀](docs/coop/earth-guardian-results.md)

## SYNC41 Android와 SYNC42 원본 콘텐츠

SYNC41의 APK 승인 범위는 7ba9bb3의 지정 해시 한 개다. 대응 ZIP에는 실제 두 기기의 필수 118건, 같은 빌드 소스와 JUnit XML을 대조한다. [후속 기능 검증](docs/coop/rune-effects-results.md) [Android 인수](docs/coop/seed-alchemy-android-acceptance.md)

최종 Drive APK와 ZIP은 각각 한 번 갱신됐다. 두 비로그인 다운로드의 해시와 대응 소스 2,408개 파일을 확인했다. [SYNC41 발행 기록](docs/coop/seed-alchemy-delivery.md)

## SYNC40 후보와 재현 자료

승인 대상 0998031의 설치 해시와 실제 두 부팅의 92개 필수 관측을 대조했다. APK 대응 소스는 빌드 커밋의 `git archive`에서 추출한다. 후보 빌드의 JUnit XML과 이번에 같은 소스로 다시 실행한 전체 XML을 별도 디렉터리에 담는다. Android 관측별 화면·후속 영수증·HEAD 및 해시를 검사한 뒤 기존 ZIP에 발행한다. 후속 SYNC41의 소스와 APK는 Cloud에 따로 보관한다. [Android 인수](docs/coop/cauldron-android-acceptance.md)

APK와 ZIP을 각각 한 번 갱신하고 양쪽의 비로그인 다운로드 크기와 해시를 확인했다. [SYNC40 배포와 체크섬](docs/coop/cauldron-delivery.md)

## SYNC39 정상 발행 완료

기존 APK와 통합 ZIP을 각각 1회 갱신했다. 양쪽 설치본 및 비로그인 APK 해시는 d059320faf181611ea5b702e2aa82d4dcbf8d2cdae3dd88920fc3928c645cb0b, 크기는 35,398,518바이트다. ZIP은 73,548,513바이트, SHA-256 9dc3c4aad54bb13a2874f837765dc1921c7ce73000a35201ffee0834e8aaa4b1이다. ZIP의 2,389개 소스를 빌드 커밋 2fa1a0ba8793c8e0ffda87f5d6f21be13965a54e와 대조했다. 기존 링크의 anyone/reader, allowFileDiscovery=false 및 APK의 기존 편집자 권한을 확인했다. SYNC40은 별도 Cloud 후보로 관리한다. [인수 결과](docs/coop/original-items-android-acceptance.md)

## 변경 기능별 Android 발행 검사

SYNC38은 성서 및 주문서 역할별 38건과 게스트의 소환수 보관·무료 해제 2건을 검사한다. 실제 결과는 39 PASS, 1 FAIL이다. APK를 한 번 갱신했고 무료 해제 실패로 기존 ZIP을 유지했다. APK 발행 전 설치가 불가능할 때에는 해당 바이트에 대한 사용자 승인과 플랫폼 승인을 따른다. 통합 ZIP은 실제 기기에서 필수 시험을 마친 뒤 갱신한다. 각 후보의 빌드 커밋·APK 해시·바이트 수와 변경 기능별 필수 목록을 먼저 고정한다.

`python3 scripts/verify-final-deliverables.py --android-candidate CANDIDATE.apk docs/coop/android-acceptance-sync38.json observations.tsv`를 실행한다. 설치본, 부팅, 실제 영수증, 다음 행동, 예외·중단·대기 요청과 증거 파일 해시를 대조한다. 기존 `--selector-candidate`는 성서 선택 회귀에 함께 사용한다. APK 대응 소스는 정확한 빌드 커밋에서 추출하고 최신 검증 도구와 자료는 evidence 또는 documentation에 넣는다.

## 2026-11-27 SYNC37 배포와 SYNC38 개발

SYNC37 APK(27663fc)를 승인된 기존 ID에 1회 갱신했다. Android 주문서 인수 실패로 ZIP은 09c9417을 유지한다. 다음 APK에는 서명과 함께 실제 DEX selector 저장 지점을 검사하고 같은 바이트로 Android 인수를 수행한다. [실제 Android 인수](docs/coop/enchantment-android-acceptance.md)

## SYNC36 실제 Android와 기존 Drive 파일

09c9417 해시에 대한 사용자 한정 승인으로 기존 APK를 한 번 교체하고, Android 인수 뒤 같은 소스의 통합 ZIP을 한 번 교체했다. 기존 추가 편집자와 링크 공유 권한을 보존했다. 두 MuMu 내부 Chromium 다운로드와 데이터 보존 업데이트, 로그와 화면의 Cloud 수집을 사용했다. 다음 빌드의 사전 발행은 별도 승인 대상이다. [개발 및 시험 범위](docs/coop/enchantment-results.md) · [Android 인수](docs/coop/shared-growth-android-acceptance.md) · [배포 파일 대조](docs/coop/shared-growth-delivery.md)

## 현재 일치하는 배포 세트 · e690e53

기존 APK에 지정 후보를 1회 발행하고, 두 Android에서 인수한 뒤 동일 소스와 실측 자료를 담은 ZIP을 기존 ID에 1회 발행했다. 두 비로그인 다운로드의 전체 바이트와 SHA-256을 재검사했다. 발행 후 개발 브랜치의 SYNC36 APK는 Cloud에 보관한다. [파일 식별자와 체크섬](docs/coop/tier-growth-android-acceptance.md)

## 상위 성장 후보와 공개 파일

SYNC35 후보는 Cloud에서 보관한다. 공개 APK의 대응 소스는 9969d23, 공개 ZIP의 대응 소스는 07b1889다. 이전 일회성 발행 예외는 해당 수정판에만 적용됐다. [기능·회귀별 실측](docs/coop/tier-growth-results.md)

Cloud 보관 위치는 `/workspace/shared/downloads/SPD-Coop-e690e53-tier-growth/`다. 기존 검사기로 통합 ZIP의 source 파일 2,359개를 APK 소스 e690e533383a68978781842a949cd0696bfecb63과 대조하고 체크섬 2,504개를 검증했다. 후속 검증 및 문서 기준 커밋은 32f8d2aec5db8335771f00b2ff677cc0f55273ba다.

| Cloud 파일 | 바이트 | SHA-256 |
|---|---:|---|
| SPD-Coop-latest.apk | 37,321,868 | `cc8a6f7ab203a01e7beacfc7e2fcb00ad487175be537cd6cfa25689e96e9aa4b` |
| SPD-Coop-latest-backup.zip | 72,871,855 | `ffcd483cec93144c2081537c3e0956ae9f49d03978a8d2e2a2691aa56768b6f6` |

이번 Drive 갱신은 APK·ZIP 각각 0회다. 위 두 해시는 Cloud 후보의 해시다. 기존 공개 파일을 비로그인 다운로드해 확인한 크기와 해시는 기능 보고서에 기록했다.


## 현재 Drive의 APK와 ZIP

기존 APK ID에는 9969d23의 Android 인수 완료 수정판이 있다. 통합 ZIP의 source는 07b1889다. 승인된 APK 예외 1회를 사용했고 ZIP 전송은 0회였다. 개발 APK와 증거는 Cloud에 보존했다. [파일 ID, SHA-256과 실제 다운로드 확인](docs/coop/selector-android-acceptance.md)

## 후보 검증과 발행 순서

후보 APK의 빌드 커밋, 크기, SHA-256, 패키지, 기존 서명과 JUnit XML을 Cloud에서 고정한다. 승인된 비공개 전달 경로가 있으면 두 Android의 기존 자료를 보존해 업데이트한 뒤 **각 base.apk의 해시가 후보와 일치하는 상태**에서 신규 기능과 기존 핵심 UI를 조작한다. 오류가 나오면 수정한 바이트를 새 후보로 취급하고 영향을 받는 Android 시험을 반복한다.

검증한 후보를 기존 Drive APK ID에 한 번 갱신하고 비로그인으로 내려받아 대조한다. Android 시험 결과까지 확정한 뒤 동일 APK 커밋의 소스와 증거로 통합 ZIP을 만들어 기존 ID에 한 번 갱신한다. 발행 전 두 파일의 최신 리비전과 이번 작업의 전송 횟수를 확인한다. 이전 작업의 완료 기록과 이번 작업의 전송을 구분하되 같은 발행 작업의 재시도는 별도 회차로 세지 않는다.

비공개 전달이 불가능한 경우 사용자가 허용한 검증 전 발행 예외는 **해당 발행 작업에 APK 갱신 기회가 남아 있을 때** 적용한다. 사전 검증의 제한을 기록하고, 서명과 수정 회귀를 확인한 정확한 후보를 발행한 뒤 Android에서 검사한다. 이 예외로 먼저 발행한 APK가 실패하면 같은 작업의 두 번째 APK 교체는 할 수 없다. 이번 수정 후보의 관리 파일 교체는 자동 승인 검토가 이미 소비한 전송과 미완료 Android 인수를 이유로 거절했다. 이미 갱신을 소비한 동일 후보를 검증 전에 다시 교체하려면 대상 파일 ID, 후보 해시, 기존 크래시, 실제 Desktop·JUnit 결과, Android 미실행 상태, 추가 갱신 횟수를 명시해 사용자 예외 승인과 플랫폼 승인을 모두 확보한다. 새 프롬프트에서도 기존 횟수를 계승한다. [당시 자동 승인 결과](docs/coop/selector-repair-results.md) · [현재 파일 대조와 인수 상태](docs/coop/trinity-discovery-results.md)

성서 오류의 사전 검증에는 Cloud에서 아래 검사를 사용한다.

```sh
python3 scripts/verify-final-deliverables.py --selector-candidate CANDIDATE.apk ANDROID_OBSERVATIONS.tsv
```

비공개 관측 파일의 탭 구분 헤더는 `role boot_id base_apk_sha256 selector result crashes anrs` 순서다. 부팅 ID는 시험 Android 자체에서 읽고 SHA-256은 설치된 base.apk에서 읽는다. `role`은 host 또는 guest, `selector`는 HolyIntuition, RecallInscription, BodyForm, MindForm, SpiritForm 중 하나다. 실제 조작·취소·재선택·시전·충전·저장·재인증·다음 행동을 확인한 후에만 해당 행에 PASS를 기록한다. 같은 시험 구간의 앱 크래시와 ANR을 센다. 명령은 후보 파일을 해시하고 관측표의 두 부팅, 설치본 및 열 개 결과를 대조한다. 화면이나 로그의 판독은 직접 수행한다. [기기 절차](ANDROID-REMOTE-TESTING.md)

최종 ZIP이 생기면 기존 `python3 scripts/verify-final-deliverables.py DIRECTORY FULL_APK_COMMIT` 검사로 관리 APK, ZIP의 체크섬과 실제 소스를 대조한다.


인장·갑옷판의 실제 배포 소스, Android에서 찾은 오류, 후속 수정 소스와 두 결과물의 검사값은 [이번 발행 기록](docs/coop/seal-arts-delivery.md)에 있다.

Yog 일반 승리판도 기존 두 관리 파일의 ID를 유지한다. 통합 ZIP 안에 APK 빌드 소스와 후속 검증 커밋의 테스트 및 보고서를 별도 경로로 수록한다. [최종 보스·승리 검증 기록](docs/coop/yog-victory-results.md)

## 원격 기기의 파일 관리

Windows에는 프로젝트 파일을 남기지 않는다. 실제 Android 자료는 Cloud로 스트리밍해 최종 통합 ZIP에 포함한다. [GB2 시험 절차](ANDROID-REMOTE-TESTING.md)

GitHub의 coop/phase8에는 검증된 소스 체크포인트를 기록한다. Google Drive는 세션 마지막에 최종 검증판을 다운로드하는 용도로 사용한다. 이전 방식으로 보존한 40개 버전과 그 자료는 과거 기록이다.

## 대상 및 횟수

지정된 소유 계정의 기존 My Drive/SPD-Coop/01_Latest에서 두 이름만 관리한다.

* [SPD-Coop-latest.apk](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view)
* [SPD-Coop-latest-backup.zip](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)

한 세션의 전송 상한은 APK 한 개와 통합 ZIP 한 개다. 검증된 신규 APK가 없으면 업로드는 0개다. 중간 빌드는 Cloud에서만 보관한다. 기존 파일의 삭제·이동, 소급 백업, 아카이브 복사 및 별도 자료 업로드는 사용자의 별도 요청이 필요하다.

## 빌드와 발행 순서

1. 전체 JUnit, 패키지, 기존 서명과 APK 빌드 커밋을 확인한다. 개발 브랜치에 일반 푸시하고 원격 커밋을 대조한다.
2. 두 관리 파일의 소유 계정·폴더·ID와 현재 리비전, 남은 갱신 횟수를 확인한다. 이미 같은 파일이면 링크를 재사용한다.
3. 위 후보 검증 절차에 따라 실제 Android 인수를 수행한다. 설치본과 같은 SHA-256의 APK를 기존 ID에 발행하고 비로그인 다운로드의 바이트 수와 SHA-256을 대조한다. 검증 전 발행 예외를 사용했다면 해당 상태와 후속 기기 판정을 기록한다.
4. Android 결과가 확정되면 소스·에셋·Gradle Wrapper JAR·라이선스·크레딧·전체 JUnit XML·빌드·서명·화면·체크섬·BUILD-INFO와 남은 작업을 통합 ZIP에 담는다. 서명 키, 개인 저장, 인증값, APK 및 과거 ZIP을 제외한다.
5. ZIP을 검사해 기존 ID에 한 번 발행한다. 비로그인 다운로드의 크기와 SHA-256, 두 파일의 접근 권한과 실제 전송 횟수를 보고한다. 전송이 막히면 Cloud 보존 경로와 검증한 마지막 해시를 기록한다.

통합 ZIP의 source/는 APK 빌드 커밋의 Git 트리에서 구성하고, 검증 문서와 검증 당시 자료는 documentation/ 및 evidence/에 둔다. Python 3.11 이상에서 `python3 scripts/verify-final-deliverables.py <두 파일이 있는 디렉터리> <APK의 전체 커밋>`을 실행하면 모든 구성 파일, 독립 APK의 SHA-256 및 빌드 커밋과의 소스 일치를 게시 전에 검사할 수 있다.
