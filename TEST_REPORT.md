# 2026-11-24 클라우드 실행 보고

|상태|실행한 검사|관측 결과|
|---|---|---|
|PASS|git ls-remote --exit-code --tags 및 git rev-parse HEAD 'v4.0.2^{commit}'|공식 태그와 로컬 커밋 일치. 전체 SHA는 PROGRESS.md에 있다.|
|FAIL|첫 ./gradlew --no-daemon :android:assembleDebug|Java가 Gradle 배포 URL에 직접 연결해 Connection refused. spd-baseline.log.|
|PASS|JDK 설치, sdkmanager, 프록시 적용 후 같은 원본 명령|BUILD SUCCESSFUL, 46 tasks executed. spd-build.log.|
|PASS|scripts/setup-android.sh|CLI 압축 파일 체크섬 검증 및 SDK 구성 확인. SDK XML 버전 경고 발생.|
|FAIL|기본 샌드박스의 ./gradlew --offline --no-daemon :core:test :android:assembleDebug|Gradle 잠금 소켓용 wildcard IP를 열 수 없음. spd-fork.log.|
|PASS|승인된 네트워크 환경에서 같은 offline 명령|독립 Android ID로 빌드 성공. 10 tasks executed, 36 up-to-date. spd-verified.log.|
|PASS|두 APK에 scripts/inspect-apk.sh 실행|aapt 패키지, min/target SDK, 네 ABI 확인. apksigner verify 성공(v1·v2), 크기와 SHA-256 기록.|
|PASS|python3 scripts/audit-engine.py|공식 커밋에서 모든 추적 Java 파일 조회. 직접 표기 325개 파일, 2,159회.|
|NOT RUN|:core:test의 실제 테스트 케이스|Gradle 태스크 결과 NO-SOURCE. 단위·Headless·소켓·스트레스·싱글플레이 회귀 미실행.|
|NOT RUN|GitHub Actions 및 Android 기기|사용자 저장소 미연결. 실기기 없음.|

## 재검토 자료

원본·개발판 패키지와 인증서 및 파일 체크섬은 docs/coop/upstream-apk.txt, docs/coop/fork-apk.txt에 있다. 빌드 시도별 전체 Gradle 출력은 docs/coop/logs/에 보존했다.

주요 추가 실행 명령은 git clone --depth 1 --single-branch --branch v4.0.2, git switch -c coop/baseline, java -version, sdkmanager --sdk_root=… 'platforms;android-36' 'build-tools;36.0.0', gh api user, git status --short, git diff --check다. 실제 사용한 JDK/SDK 설정과 재현 명령은 BUILD.md를 따른다.

## Phase 2 후속 실행 — 2026-11-24

- PASS — gh api user, gh api repos/jwgobaek/SPD-Coop, git fetch --no-tags coop master 및 merge-base --is-ancestor. 사용자 jwgobaek, master 57a4e06.
- FAIL — git push -u coop coop/baseline. HTTP 403 Permission denied. Actions 실행 목록은 조회 가능했고 0건이었다.
- FAIL → PASS — :core:test 첫 실행에서 Game.version 미설정으로 DeviceCompat.isDebug 초기화 실패. 테스트 부트스트랩에 실제 버전을 설정했다.
- PASS — 별도 v4.0.2 worktree에 테스트 의존성과 러너만 적용하여 ./gradlew --offline :core:test 실행. 지형 배열의 Arrays.hashCode가 -585159512였다. 개발 checkout에서 ./gradlew --offline :core:cleanTest :core:test :android:assembleDebug 실행. 다른 JVM에서 같은 값과 실제 저장·복원 지형 배열을 확인했다. JUnit XML 1건, 실패/오류/스킵 0. 원본 Java 런타임 클래스 변경 없음.
- NOT RUN — GitHub Actions, 실기기, 전투·턴·보스·협동 회귀. 이번 테스트가 검증한 복원 대상은 첫 층의 지형이다.

## Phase 2 엔진 회귀 및 원격 검증 — 2026-11-24

시작점 2f81e97. gh api user, 레포 메타데이터, git ls-remote --heads coop 확인 후 git push --atomic -u coop coop/baseline coop/phase2 성공. gh pr create --draft로 PR #1 생성. 이후 개발 브랜치에 일반 push. 기준 master는 57a4e06 유지.

|상태|증거와 범위|
|---|---|
|PASS|기존 :core:cleanTest :core:test부터 재실행. 전사 402 지형 해시 및 저장 지형 비교 통과.|
|FAIL → PASS|실제 이동·공격에서 HeroSprite 캐스팅, HUD 및 입력 selector 부재를 확인. 표시 객체가 존재할 때만 표시 호출을 수행하도록 조정. 원본 Hero.handle/act/onAttackComplete, Food.execute 및 시간 계산을 실행했다.|
|FAIL → PASS|시드만 고정한 다중 시드 지문에서 guidebook 위치 차이 발견. EntranceRoom.placeEarlyGuidePages는 별도 무시드 RNG를 사용한다. 마법사 2402·도적 3402의 튜토리얼 완료 상태를 고정하고 공식 태그의 독립 JVM에서 각 3회 같은 전체 지문을 확인했다. 지형·출입구·HP·몹 종류/좌표·바닥 아이템 종류/좌표/수량/강화·초기 장비를 개발판과 비교했다.|
|PASS|행동/저장 — 가속 이동 비용, 공격 완료 전후 피해·시간, 식량 소모, 강화 무기·장비 상태, HP, 좌석 도입 전 영웅 ID, 적 ID/HP/위치, 탐험 배열, 두 층 왕복과 복원 후 다음 이동.|
|PASS|Actor.process 스레드로 두 턴 휴식. 버프 만료 시점, 경과 시간 리베이스 불변식, 스레드 종료 검증. 쥐 AI의 표적 선택, 연속 네 번의 공격 비용 및 영웅 피해 검증.|
|PASS|Goo 피해·격노·층 봉쇄/해제·열쇠, 축복 앙크 1회 소모와 원본 HP 회복, 최종 사망·랭킹·저장 슬롯 제거, Dungeon.win의 승리 기록.|
|PASS|코드 커밋 f96289cd1439e0fc4560ebc256d1293ebac4343f에서 scripts/verify-android-build.sh 실행. 9 JUnit cases, 실패·오류·스킵 0. 디버그 APK 36,841,768 bytes, SHA-256 a9b558541d5431bd94972ea9b1d7ae226a59d5c242a8c7d99188cb1ef0c5d299, aapt 및 apksigner v1/v2 성공.|
|FAIL|gh workflow run android.yml --ref coop/phase2는 404. master에서 파일 조회 404, 개발 브랜치 파일 조회 성공. 관리 설정 GET은 403, 요구 권한 administration=read.|
|NOT RUN|GitHub REST 실행/Artifact 목록 모두 0건. GitHub 호스팅 빌드 및 실기기 검증 미실행. 승리 화면 버튼·전체 던전 완주, 협동 및 스트레스 테스트는 이 단계에 포함되지 않았다.|

Headless 기준은 실제 엔진 생성·행동·저장·종료에 적용한다. 화면 효과의 표시, 실제 터치, 몬스터 전체 종의 행동은 별도 검증 대상이다. 시드 지문 테스트는 JVM을 클래스별로 분리했고 기존 난수 구현을 유지했다.

## Phase 3 오프라인 두 영웅 — 2026-11-24

기준 브랜치 `coop/phase2`의 원격 HEAD는 `61deb846c4668b16272baec91c32ba21cd4d1c7b`였다. 변경 전 9건을 재실행했다. 아래 최종 빌드 대상은 `8207671d628240d2310f230503ca8ec52d865d53`이다. 테스트 엔진은 libGDX Headless, 실제 Dungeon/Level/Hero/Actor이며 클래스 단위로 JVM을 분리한다.

|상태|검증 대상|실제 결과|
|---|---|---|
|PASS|변경 전/후 단일플레이|기존 JUnit 9건을 보존했다. 시드 지형·장비·몹 지문, 이동·버프·공격·저장/복원, 원본 Actor 스레드, 보스·사망·승리 회귀 모두 통과했다.|
|PASS|두 좌석의 생성 및 초기화|같은 Level 인스턴스와 지형, 같은 몹 집합, 서로 다른 통행 가능한 위치·Actor ID, player ID/좌석 매핑, 반대 직업, 첫 영웅의 HP·위치·무기·버프 등록, 전역 골드·통계·제한 아이템 기록 및 다음 RNG 값 보존.|
|PASS|이동·시간·권한|두 영웅의 독립적인 한 칸 이동, 벽·점유 위치 거절, 잘못된 좌석/오래된 턴 거절. 가속 이동 비용을 원본 speed와 비교했고 게스트의 감속이 다음 행동 시각에만 반영됨을 확인했다.|
|PASS|인벤토리·장비·식량|원본 더미에서 획득·소멸, 두 영웅이 같은 더미에 접근하는 경쟁, 타인 소유 아이템 거부, 서로 다른 강화 수치가 있는 무기 장착, 양쪽의 식량 수량·섭취 횟수 검증.|
|PASS|기본 전투·경험치|동일한 쥐를 두 영웅이 공격. 실제 명중·피해·attackDelay와 중복 요청 거절 확인. 쥐가 게스트를 선택해 공격한 동안 호스트 HP 보존. 쥐의 4회 행동 시간과 게스트 처치 1회의 XP·등록 해제 검증.|
|PASS|중단 및 저장 격리|외부 스레드 요청·중복 스케줄러 진입·싱글 저장/삭제·층 전환 거절. 게스트 치명상과 미지원 Crab의 행동 시도에서 세션 종료. 기존 싱글 슬롯의 테스트 내용과 도감 식량 사용 횟수, 배지 상태, 기본 파일 경로 복원 확인.|
|PASS|반복 행동·종료|시드 402/2402/3402 각각 두 좌석 총 120회(호스트 대기 60, 게스트 왕복 이동 60). 시간 역행·차례 누락·점유 불일치·식량 복제 없이 시드당 원본 게임 시간 60 경과. 전용 엔진 스레드 종료 확인.|
|PASS|전체 및 반복|최종 빌드 시 JUnit 19/19 통과(기존 9 + 신규 10). 이어 새 JVM에서 협동 10건을 3회 재실행해 30/30 통과. 이 네 번의 실행에서 반복 시나리오의 유효 행동은 총 1,440회였다. XML별 오류·실패·스킵 모두 0.|
|PASS|Android 디버그 APK|36,795,935 bytes, SHA-256 `8bafb66c7056cc48dccd7e36b2714ed7852c60175ddbb745f23de5633964ae61`. aapt로 `org.spdcoop.dungeon.indev`, 4.0.2-INDEV(922), minSdk 21, targetSdk 36 확인. apksigner v1/v2 서명 검증 성공.|
|PASS|참조 색인 재현|원본 커밋을 대상으로 역할 표를 다시 생성해 바이트 단위로 비교. 직접 참조 2,159개·325개 파일 일치. 의미 분류는 수동 검토와 구분되는 1차 분류다.|
|NOT RUN|GitHub 호스팅·실기기·LAN·콘텐츠 완주|코드 커밋의 일반 푸시 후 Actions API 브랜치 실행 0건. Cloud APK를 검사했다. Android 기기 설치·화면·터치, LAN, 협동 전체 던전 완주는 실행하지 않았다.|

개발 중 실제 공격 테스트에서 새 몹을 감지한 `Hero.checkVisibleMobs`가 입력 직후 요청을 취소하는 경로를 확인했다. 새 적의 가시성을 명시적 입력 **전에** 갱신했다. 사냥꾼의 장갑 공격은 명중해도 피해가 0일 수 있어 피해 검증에서는 실제 RNG에 고정 시드를 주고 원본 방어·피해·시간 계산을 그대로 실행했다. 버프, 음식, 초기 장비, 배지와 도감 캐시의 주체를 점검해 게스트 사용이나 세션 종료가 다른 소유자의 상태에 적용되지 않도록 했다. apksigner의 META-INF 경고와 v3/v4 미적용은 원문에 포함했다.

### 재현 명령과 결과물

아래는 이 Cloud 작업에서 사용한 주요 명령이다. `/tmp/spd-env`는 이 인스턴스의 JDK/SDK/Gradle 경로 설정이며 새 환경은 BUILD.md 절차를 따른다.

```bash
git ls-remote --heads coop master coop/phase2 coop/phase3
git switch -c coop/phase3 61deb84
source /tmp/spd-env
./gradlew --offline :core:cleanTest :core:test
./gradlew --offline :core:test
python3 scripts/classify-hero-references.py > docs/coop/hero-roles.tsv
git diff --check
scripts/verify-android-build.sh /workspace/deliverables/phase3-engine
./gradlew --offline :core:cleanTest :core:test --tests '*CoopSessionTest'
git push -u coop coop/phase3
gh api 'repos/jwgobaek/SPD-Coop/actions/runs?branch=coop%2Fphase3&per_page=5'
```

`docs/coop/phase3-cloud-evidence.zip`에는 빌드 로그, APK 패키지·서명 출력, APK SHA256SUMS, 빌드 커밋, JUnit XML 및 추가 3회의 로그/XML이 들어 있다. ZIP 검사와 APK 체크섬 재검증 모두 통과했다. ZIP의 SHA-256은 `bb454bdca97b76d6436782b3e3c3b3b33b3176f7db0e04eaf354f4d3a6b87c1d`이다. APK는 Cloud 작업 공간의 `/workspace/deliverables/phase3-engine/android-debug.apk`에 있다. 영구 Git 보존 범위는 소스, 보고서와 증거 ZIP이다.
