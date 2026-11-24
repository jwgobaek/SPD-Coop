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

## Phase 4 LAN — 2026-11-24

시작 시 로컬/원격 coop/phase3는 95b42adef88ac2ea3abcc94aa4f7e8d8f908f5cd였다. 트리는 깨끗했고 원격 coop/phase4는 없었다. 분기 후 기존 19건을 먼저 재실행했다. 최종 검증 커밋은 5aac3b91de33e5c92be24cc02ba14a4154988c46이다.

|상태|실행 경로|관측 결과|
|---|---|---|
|PASS|기존 :core:test|최종 빌드에서 싱글 9건 + 협동 엔진 10건 통과. 기존 테스트와 Actor/CoopSession 소스를 보존했다.|
|PASS|LanIntegrationTest → LanScenarioClient|JUnit 호스트 JVM에서 실제 HeadlessApplication·CoopSession과 I/O 워커를 실행했다. 별도 java 프로세스에서 loopback 호스트 컨트롤러와 게스트 클라이언트를 각각 TCP로 연결했다. 테스트용 로컬 방장 권한은 자식의 stdin으로 전달했다. 클라이언트 JVM에서 게임을 초기화하지 않았다.|
|PASS|UDP·TCP·로비|loopback UDP 검색, TCP 직접 접속, 동일 방의 게스트 참가/종료 50회, 좌석 발급, 정원·중복 참가·게스트 시작·준비 전 시작 거부, 직업·준비 토글, 실제 두 영웅 생성, 시작 후 잠금과 검색 중단을 확인했다.|
|PASS|원본 게임 행동|한 세션에서 APPLIED 310회(방장 155, 게스트 155). 실제 이동·대기·근접 공격·바닥 무기 획득·장착·식량 섭취를 포함한다. 호스트 테스트가 생성된 층에 실제 Shortsword와 HP 1 Rat를 배치했다. 매 승인 시 actor 점유와 게임 시간, 턴 증가를 확인하고 실제 장비·식량·몬스터 사망을 검사했다.|
|PASS|권한·재전송|타 좌석 요청, 잘못된 Actor ID, 올바른 신원을 가진 비활성 좌석, 타인의 아이템 핸들, 범위 밖 이동, 미지원 행동, 과거 턴·동일 요청 ID·이전 세션·버전 불일치를 거부했다. WAITING에는 다른 좌석의 아이템 목록이 없었다.|
|PASS|경계·오류|분할 프레임, 연속 프레임, 잘린 프레임과 과대 길이, 미완성 헤더의 유휴 만료 및 다른 정상 클라이언트의 PONG을 확인했다. LanBoundaryTest 5건에서 UDP 중복 제거, UDP 포트 점유 중 TCP 성공, 응답 없는 서버의 타임아웃과 접속 거절 코드, 64개 입력 큐 경계, 실제 읽지 않는 TCP 피어의 출력 정체 종료를 확인했다.|
|PASS|단절·정리|게스트 소켓 종료 후 PAUSED, 방장 BYE 후 CLOSED. 자식 JVM 종료 코드 0, LAN 워커 종료, Actor.processing() 해제, Dungeon.coop 정리를 확인했다.|
|PASS|최종 APK와 증거|JUnit 25건(기존 19 + 신규 6), 실패·오류·스킵 0. 4.0.2-INDEV(922), org.spdcoop.dungeon.indev, 36,807,237 bytes. aapt 패키지/minSdk 21/targetSdk 36 확인, apksigner v1·v2 검증 성공. APK SHA-256 ad6c59f77a7fff1592eb6093680400848a784d56b73b801841739d70b53de0c6.|
|NOT RUN|외부 환경|GitHub 호스팅 Actions 실행 0건. Android 기기 설치·화면·Wi-Fi 브로드캐스트·핫스팟·OS별 로컬 권한은 실기기 검증 대상이다. 패킷이 전혀 도달하지 않는 실제 무선망의 TCP 연결 타임아웃도 재현하지 않았다.|

첫 고속 통합 시도에서는 기대한 310회 중 86회만 관측되어 FAIL이었다. 테스트 클라이언트의 STATE·재전송 검사가 정상 요청 예산 안에서 실행되도록 속도를 조절하고, 자식 프로세스의 최종 종료 코드와 오류 출력을 검사했다. 운영 측의 요청 제한과 310회 통과 조건은 유지했다. UDP 바인딩 실패가 TCP까지 닫던 경로도 수정하고 점유된 UDP 포트에서 실제 TCP 왕복을 확인했다.

주요 실행 명령은 다음과 같다. Java 및 SDK 설정은 BUILD.md를 따른다.

```bash
git ls-remote --heads coop coop/phase3 coop/phase4
git switch -c coop/phase4 95b42ad
source /tmp/spd-env
./gradlew --offline :core:cleanTest :core:test
./gradlew --offline :core:test --tests '*LanIntegrationTest'
scripts/verify-android-build.sh /workspace/deliverables/phase4-verified
git push -u coop coop/phase4
gh api 'repos/jwgobaek/SPD-Coop/actions/runs?branch=coop%2Fphase4'
```

LanRoom.pump/dispatch/action/view/close가 좌석·로비·실제 엔진 실행을, LanTransport.run/service/pollDiscovery가 소켓·프레임·제한 큐를, LanClient.discover/request/exchange가 검색·직접 접속·응답을 담당한다. core/build.gradle에는 자식 JVM용 테스트 클래스패스를 설정했다. 테스트 파일은 LanIntegrationTest.java, LanScenarioClient.java, LanBoundaryTest.java다.

[검증 ZIP](docs/coop/phase4-cloud-evidence.zip)에는 최종 빌드 로그, 모든 JUnit XML, APK 검사 결과·체크섬·소스 커밋이 있다. ZIP SHA-256은 01abca1a3ddf6d0e277f7532b6f721b97a0f1ff61816887a58cad7f260eab020이다. sha256sum -c와 unzip -tq가 통과했다. APK는 /workspace/deliverables/phase4-verified/android-debug.apk에 있다.

## Phase 5 — 확정 행동과 좌석별 공개 상태

시작 시 로컬 HEAD와 원격 coop/phase4가 모두 757a4a7c38ff641417b2f7e95c265a4531c0a095였고 작업 트리는 깨끗했다. 기존 25건을 먼저 실행한 뒤 SYNC1을 별도 브랜치에 구현했다. 최종 검증 대상은 18700148a3770cc050087d72c14e39a06e342281이다.

### 검증 방법

- SyncIntegrationTest.normalSynchronizedGameAcrossSocket은 실제 Headless 호스트 JVM과 별도 클라이언트 JVM에 두 TCP 좌석을 둔다. 양쪽이 직접 만든 DisplayReplica를 읽어 현재 활성 좌석에서 SeatActions.Pending을 제출한다. 호스트는 원본 턴·시간을 별도로 검사한다. 중간에 실제 Actor.fixTime을 실행하고 누적 시간과 이후 상태 버전의 연속성을 검사한다.
- committedViewsAcrossThreeIndependentClientRuns는 세 번 새 실제 방과 자식 JVM을 만든다. 생성된 첫 층에 실제 일반 식량·근접 무기·쥐를 배치하고 게스트에게 가속을 부여한다. 여섯 행동을 실제 CoopSession.act로 처리한다. 바닥 식량이 기존 스택에 병합되고 합쳐진 ID가 사라지는지, 수량 2에서 1로 줄어드는지, 무기의 귀속·장착과 쥐 처치가 실제로 일어났는지 확인한다.
- 같은 논리 행동의 영수증을 응용 계층에서 버리고 지연 후 동일 좌석의 같은 TCP 연결에서 새 request ID로 재조회한다. 이미 지난 턴의 캐시 결과, 내용 충돌, 다른 좌석의 조회, 타인 소유·퇴역한 아이템 ID, 이전 세션과 오래된 상태·턴, 캐시 64개를 지난 과거 ID를 검사한다. 기존 ACT로 동기화 방의 검사를 우회할 수도 없다.
- 실제 TCP로 받은 변경분 두 개를 표시 모델에 순서 반대로 전달하고, 정상 순서 및 중복도 적용한다. 누락 후 새 스냅샷으로 복구하며 최신 좌석·턴에서 다음 명령이 승인되는지 확인한다. 실제 스냅샷 조각의 마지막 바이트를 클라이언트에서 손상시켜 독립적인 SHA 검증과 전체 재요청을 확인한다. 응답 유실·중복·순서 변경·손상은 TCP 수신 후의 응용 계층 오류 주입이다.
- 오류 사례에서는 지원 범위 밖의 실제 Crab을 스케줄러에 등록한다. WAIT가 원본 행동 시간을 이미 소모한 뒤 다음 Actor 처리 중 오류가 발생한다. 호스트의 실제 시간을 확인하고 HALTED_UNCERTAIN을 재조회한다. 양쪽의 PAUSED와 ENGINE_ERROR, 행동자 한 명에게만 제공되는 영수증을 검사한다. 이 중단 시도를 정상 확정 횟수에 포함하지 않는다.
- directSightPrivacyAndLossOnGeneratedFloor는 원본 Level에 차폐 벽·비밀 문·숨은 함정과 몹을 배치한 뒤 두 실제 Hero의 공개 상태를 비교한다. 자기 쪽의 HP 변경·표시 버프, 반대편 가방·바닥 아이템·캐릭터·지형의 비공개, 시야에서 나간 몹의 관측 상실, 가려진 후 사라진 몹의 사망 미통지, 전체 재동기화 시 정보 제한을 확인한다.
- DisplayReplicaTest는 다중 조각·불완전한 조립·순서·토큰·개수 상한, 같은 버전의 손상된 상태 본문, 다른 좌석과 과대 상태, 닫힌 모델을 검사한다. 손상된 상태는 기존 이미지 객체를 교체하지 않는다. 기존 LanBoundaryTest와 v1의 프레임 크기 제한 검증도 함께 실행한다.

### 재현 및 변경 파일

```bash
git ls-remote --heads coop master coop/phase4 coop/phase5
git switch -c coop/phase5 757a4a7
source /tmp/spd-env
./gradlew --offline :core:cleanTest :core:test
./gradlew --offline :core:test --tests '*SyncIntegrationTest' --tests '*DisplayReplicaTest'
scripts/verify-android-build.sh /workspace/deliverables/phase5-verified
```

- LanRoom.synchronizedGame/dispatch/pump, LanClient.synchronizedGame/exchange 및 LanTransport.service가 협상과 제한된 프레임을 처리한다.
- RoomSync.action/apply/commit/sync/part, SeatActions.prepare/Pending.submit이 확정·결과 조회와 전송의 경계다.
- SeatProjection.capture/resolve, StateImage.encode/hash, DisplayReplica.refresh/apply/Assembly가 서버 공개 범위와 클라이언트 모델을 구현한다.
- Level.updateDirectSight/updateFieldOfView가 기본 시야 계산을 공유한다. CoopSession.basicMob과 Mob.wasDefeatedBy는 검증된 몹 종류와 실제 직접 처치를 식별한다.
- 신규 테스트·러너는 SyncIntegrationTest, SyncScenarioClient, ProjectionChecks, DisplayReplicaTest다.

### 최종 실행 결과

|상태|검증|실제 결과|
|---|---|---|
|PASS|기존 25건|싱글플레이 9, 협동 엔진 10, LAN 6 통과. 기존 v1 행동 310회 및 로비 참가·종료 50회를 다시 수행했다.|
|PASS|신규 4건|정상 독립 JVM 시나리오, 장애 포함 3개 세션, 원본 시야·관측 상실·프라이버시, 조각·모델 무결성 검증이 통과했다. 전체 JUnit 29건에서 실패·오류·스킵 0.|
|PASS|정상 SYNC1|320회 확정, 요청 975건. 거부·캐시 재응답·재동기화·응답 유실·엔진 중단 각 0건.|
|PASS|장애 SYNC1|세션당 341회 확정, 요청 1,131건, 거부 55건, 캐시 재응답 26건, 전체 재동기화 2회, 응용 계층 응답 유실 1회, 부분 적용 후 엔진 중단 1회. 3개 JVM 시나리오에서 같은 집계 확인.|
|PASS|SYNC1 합계|정상 확정 1,343회, 요청 4,368건, 거부 165건, 캐시 재응답 78건, 전체 재동기화 6회, 응용 계층 응답 유실 3회, 실행 중단 3회. 요청 수에는 조회·조각·로비도 포함한다. 충돌·만료는 거부, 기존 영수증 재반환은 캐시로 센다. 정상 행동 집계에서 중복·중단과 v1 회귀는 제외했다.|
|PASS|APK|4.0.2-INDEV(922), org.spdcoop.dungeon.indev, 36,818,193 bytes, minSdk 21, targetSdk 36. apksigner v1/v2 성공. SHA-256 554e06c844c3a37315c3039c6d428c5bd3860a47516f424f89a5dcdc65e342c0.|
|NOT RUN|외부 검증|coop/phase5 일반 푸시 후 GitHub Actions의 브랜치 실행 기록 0건. 실제 Android 설치·화면·터치·무선 LAN과 재접속은 미검증이다.|

빌드 스크립트 종료 코드 0. APK 파일에 sha256sum -c, 증거 ZIP에 unzip -tq를 실행해 통과했다. [최종 빌드 로그·JUnit XML·패키지와 서명 검사·소스 커밋](docs/coop/phase5-cloud-evidence.zip). ZIP SHA-256은 a82bb060ac2581b227074313f059bf46432f2205705d79f92775f7ab067911e7이다. APK는 Cloud 작업 공간의 /workspace/deliverables/phase5-verified/android-debug.apk에 있다.

## Phase 6 — 2026-11-24

기준 브랜치 coop/phase5와 실제 원격 d86d21ffb8ab2203077dc6bf86e7a0efc711bc19를 확인했다. 아래 최종 수치는 엔진 커밋 53770eabfd0c40ead0d4d1848ddf480c2743930b의 단일 Cloud 검증 빌드에서 얻었다. 단계별 반복 실행은 최종 합계에 더하지 않았다.

| 결과 | 실행 | 관측 |
|---|---|---|
| PASS | 변경 전 전체 회귀 | 싱글 9, 협동 엔진 10, LAN 6, 동기화 4. 총 29건 |
| PASS | 6A 체크포인트 | 전체 29건. 실제 벽 차폐, 다른 좌석의 숨은 몹·더미 제외, 시야 밖에서 지형을 수정한 뒤 원래 기억 유지, 스냅샷 복원과 재관측 갱신을 검사 |
| PASS | 6B 체크포인트 | 선택 회귀 13건. 벽으로 분리한 두 전투, 표적 변경, 경계 병합·분리 5회, 몹 사망, Actor 집합·시간·턴·HP 불변 검사 |
| PASS | 6C 체크포인트 | 선택 회귀 14건. 원본 가방에 식량 스택 병합, 한 단위 드롭, 분할 후 새 ID와 소유권, 타인 가방 수집 실패, 소유자가 다른 바닥 스택 공존, 원본 장비 해제와 재장착 |
| FAIL → PASS | 6D 체크포인트 | 테스트에서 외부 강제 사망으로 취소된 입력 차례를 곧바로 사용해 좌석 -1 오류가 났다. 호스트의 외부 효과 묶음을 finishEffects로 마친 뒤 생존자의 실제 차례를 검증했다. 수정 후 선택 회귀 15건 통과 |
| FAIL → PASS | 동일 몹 교전 소멸 검사 | 두 영웅의 실제 공격을 받은 몹의 최근 교전이 만료된 후, 테스트 구역에 다른 생성 몹이 접근해 새 교전이 형성될 수 있었다. 단일 표적 테스트에서 다른 몹을 격리했다. 엔진의 표적 선택과 구역 소멸 조건을 유지한 채 선택 회귀 16건 통과 |
| PASS | 최종 원본 회귀 및 확장 | 기존 29건과 새 JUnit 메서드 5건, 합계 34건. 실패·오류·스킵 0. 기존 치명상 중단 기대값은 새로운 DEAD·동료 진행·과거 턴 거부 검사로 변경했다. 싱글플레이 축복 앙크·최종 사망·슬롯 정리 회귀도 통과 |
| PASS | 엔진 세부 검증 | 100, 17, 10, 9, 3, 2, 1과 큰 정수의 부활 HP 및 최대 HP 상한, 실제 Rat 근접 공격·Poison.act 치명상, 사망 후 반복 die, 거리·턴·자원 검사, 동결 시계 리베이스와 부활 후 행동, 앙크 1회 소모, 공동 표적에 대한 실제 공격을 검사 |
| PASS | 원본 Foresight 및 공개 범위 | 게스트에 실제 Foresight를 부여하고 원본 search로 차폐 너머 지형만 파악했다. 호스트의 mapped와 원본 level.mapped 배열이 변하지 않는지 확인했다. 현재 시야의 숨은 더미, 비공개 인벤토리, 은폐된 몹, 시야를 떠난 몹은 공개 대상에서 제외됐다. |
| PASS | 이전 독립 JVM 시나리오 | v1 실제 행동 310회 및 로비 50회, 기존 SYNC1 행동 1,343회. 응답 유실·재전송·과거 버전과 엔진 중단 회귀 통과 |
| PASS | Phase 6 독립 JVM | 아래 세 시나리오, 합계 1,023회. 주어진 게임 행동으로 구역을 합치고 분리한 뒤 실제 아이템·부활·전멸까지 확인 |
| PASS | Android Cloud 빌드 | 4.0.2-INDEV, versionCode 922, org.spdcoop.dungeon.indev, minSdk 21, targetSdk 36, 36,823,839바이트. 디버그 서명 v1·v2를 검증하고 체크섬을 다시 확인했다. apksigner의 META-INF 경고 및 v3/v4 미적용은 apk.txt에 있다. |
| NOT RUN | GitHub Actions | 엔진 커밋 푸시 후 브랜치 실행 기록 0, 등록 워크플로 0. android.yml 수동 실행은 조회 단계에서 HTTP 404였다. 포크의 관리자 설정은 확정하지 않았다. GitHub Actions APK 아티팩트는 없다. |
| NOT RUN | 실기기 | Android 설치·화면·터치·무선 Wi-Fi·핫스팟·기기별 성능. 협동 UI, 저장 및 재접속은 후속 단계다. |

### 독립 프로세스 시나리오

RuleIntegrationTest는 HeadlessApplication 호스트에 실제 생성된 첫 던전을 열고 결정적 검사용 지형, 분리벽·통로, 원본 Rat 두 마리, 일반 식량과 앙크를 배치했다. 다른 생성 몹은 이 시나리오에서 제외했다. 두 Rat가 일정 시간 제자리에 있도록 처음 행동을 지연했고 부활·전멸 지점에서는 테스트 Actor가 원본 공격·피해 함수를 호출했다. 일반 몹의 자율 AI와 무작위 월드 회귀는 기존 테스트가 별도로 실행했다. 별도 JVM의 RuleScenarioClient가 두 TCP 좌석으로 접속하고 표시 모델에서 행동을 선택했다.

| 시드 | 방장 / 게스트 | 확정 행동 | 전송 요청 | SYNC·PAGE 조회 | 거부 | 캐시 응답 | 재동기화 | 부활 |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| 402 | 전사 / 사냥꾼 | 341 | 1,049 | 694 | 1 | 2 | 1 | 1 |
| 2402 | 사냥꾼 / 전사 | 341 | 1,049 | 694 | 1 | 2 | 1 | 1 |
| 3402 | 전사 / 사냥꾼 | 341 | 1,049 | 694 | 1 | 2 | 1 | 1 |
| 합계 | | **1,023** | **3,147** | **2,082** | **3** | **6** | **3** | **3** |

각 실행에는 일반 APPLIED 340회와 파티 전멸을 확정한 마지막 행동 1회가 포함된다. 로비·협상 요청은 총 33건이다. 실제 턴·버전이 거부와 재전송에서 증가하지 않는지 확인했다. 클라이언트에서 수신한 스냅샷 조각을 한 차례 손상시켜 재동기화를 일으켰다. 복구된 상태 행·해시와 기존 표시 상태를 비교하고 이후 행동을 계속했다. 호스트에서는 실제 구역 수, HP 17 → 5, 인벤토리에서 사라진 앙크, 최종 행동 횟수를 대조했다. 두 프로세스 모두 정상 종료했다.

### 재현과 결과물

```bash
git ls-remote coop refs/heads/coop/phase5 refs/heads/coop/phase6
git switch -c coop/phase6 d86d21f
source /tmp/spd-env
./gradlew --offline :core:cleanTest :core:test
./gradlew --offline :core:cleanTest :core:test --tests '*CoopSessionTest' --tests '*SyncIntegrationTest.directSight*' --tests '*SchedulerRegressionTest'
./gradlew --offline :core:cleanTest :core:test --tests '*CoopSessionTest' --tests '*LanIntegrationTest' --tests '*SyncIntegrationTest.directSight*'
./gradlew --offline :core:cleanTest :core:test --tests '*CoopSessionTest' --tests '*EndgameRegressionTest' --tests '*SyncIntegrationTest.directSight*'
./gradlew --offline :core:cleanTest :core:test --tests '*RuleIntegrationTest'
scripts/verify-android-build.sh /workspace/deliverables/phase6-verified
git diff --check
git push -u coop coop/phase6
gh workflow run android.yml --repo jwgobaek/SPD-Coop --ref coop/phase6
```

최종 APK SHA-256은 `690e3c4070a5401c549cd31400708e6e011dd9c6932d985d984d2275b29a642a`이다. Cloud 경로는 `/workspace/deliverables/phase6-verified/android-debug.apk`다. 증거 ZIP은 최종 JUnit XML·빌드·서명 로그·소스 커밋·변경 파일과 중간 실패 및 재검증 기록을 포함한다. ZIP 무결성 검사와 APK의 sha256sum -c가 통과했다. ZIP SHA-256은 `d8c7330421bfd252c34b928415f7315ace96d380a116bf3817879fdd74cc2a27`이다. [Git에 보존한 검증 ZIP](docs/coop/phase6-cloud-evidence.zip)

## Phase 7 클라우드 검증 결과

빌드 대상은 `da173259ca546d1e289197d4336a4e5421c83c10`이다. 같은 커밋으로 원본 JUnit 회귀, 협동 저장·소켓 검사 및 Android APK 검사를 실행했다.

| 검사 | 상태 | 실제 결과 |
|---|---|---|
| 변경 전 기준선 | PASS | 기존 34건, 실패·오류·스킵 0 |
| 7A·7B·7C·7D 단계별 실행 | PASS | 7A 수정 후 16건, 7B 19건 및 보강 5건, 7C 6건, 7D 최종 집중 검사 10건. 각 실행의 XML을 ZIP에 보존 |
| 최종 전체 JUnit | PASS | 기존 34건과 신규 10건. 총 44건, 실패·오류·스킵 0 |
| 일반 체크포인트 | PASS | 두 시드·양쪽 직업 배치에서 파일로 저장하고 엔진을 닫은 후 20회 복원. 실제 다음 Actor와 시간 확인 후 20회 행동. 전투 불능·부활 후 상태와 파티 전멸 복원 별도 검사 |
| 독립 JVM 재시작 | PASS | 호스트/클라이언트 JVM을 각각 12회 기동. 초기 게임 이후 디스크에서 11회 재개. 두 좌석의 확정된 공개 상태를 개인 표시 모델과 비교 |
| 연결 재인증 | PASS | 주 시나리오에서 실제 새 TCP 연결 54건을 기존 좌석에 연결. 별도 인증 테스트에서 잘못된 서명·좌석·중복 연결·신규 참가·다른 좌석의 확인 해시 거부 |
| 호스트 네트워크 행동 | PASS | 성공 응답을 받은 행동 1,019건. 응답 전 강제 종료 후 검증한 부활·대기 영수증 2건. 확정 합계 1,021건 중 최초 호스트 재시작 이후 1,006건. 조회·거부·롤백된 메모리 변경은 집계에서 제외 |
| 행동 후 단절과 재조회 | PASS | 획득·공격·식량의 확정 뒤 소켓을 닫고 재접속. 동일 행동 재제출과 LOOKUP에서 기존 결과 반환. 부활은 실제 저장 직후 응답 전에 종료하여 HP 5와 앙크 소모 1회를 새 호스트에서 검증. 1,000회 추가 진행 후 오래된 영수증 EXPIRED |
| 강제 종료 | PASS | 실제 호스트 JVM 6회. 부활 저장 직후 응답 전, 장시간 진행의 마지막 응답 후, 기록 중·검증 중·HEAD 교체 전·후. 별도 파일 계층 시험에서 독립 작성자 JVM 4회 강제 종료 |
| 쓰기 실패·손상 | PASS | 실패를 주입한 행동에는 CHECKPOINT_FAILED를 반환했고 재시작 후 같은 턴과 UNKNOWN을 확인. 파일 누락·잘림·SHA 불일치·다른 세대·HEAD 소실·잘못된 스키마·중복 Actor/아이템·귀속·영수증 오류 거부. 이전 정상 세대 복구 시 새 epoch 강제 |
| Android Cloud 디버그 APK | PASS | org.spdcoop.dungeon.indev, 4.0.2-INDEV (922), min SDK 21, target SDK 36, 36,842,183바이트. aapt 및 apksigner v1/v2 검증과 SHA-256 재검사 통과 |
| GitHub 호스팅 CI / Artifact | NOT RUN | 개발 브랜치를 일반 푸시했으나 해당 브랜치 run 조회에서 0건 |
| Android 실기기 | NOT RUN | 설치, 렌더링, 터치, 실제 Wi-Fi·핫스팟, 앱 생명주기와 전원 차단 내구성 |

**발견한 오류와 재검증.** 첫 7A 검사 한 건은 FAIL이었다. 아이템을 내려놓는 행동에서 몬스터 AI가 표적을 다시 선택했는데 테스트가 이전 표적을 기대했다. 저장 직전의 실제 표적을 확정하고 같은 Actor를 복원하는 검증으로 수정했다. 최종 결과에 해당 실패 실행을 합산하지 않았다. 손상된 현재 세대가 정상 백업을 밀어낼 수 있는 경우도 검토해 백업 승격 전 무결성을 재확인하고 이를 회귀로 고정했다.

**주요 변경 파일과 진입점.** 이 단계의 전체 경로는 ZIP의 `changed-files.txt`에서 확인할 수 있다.

| 영역 | 클래스와 주요 메서드 |
|---|---|
| 원본 엔진 저장 | Dungeon.worldState/restoreWorld, CoopSession.checkpoint/restore/load, Actor.schedulerState/restoreSchedule, Random.worldState/restoreWorld |
| 플레이어·객체 상태 | FloorKnowledge.save/load, ItemOwnership.restored, Bag.restoreFromBundle, Char.add, Heap·Foresight와 Level의 복원 경로 |
| 디스크·인증 | CoopSaves.sessions/inspect, Slot.read/publish, SeatCredential.generate/enroll/reconnect, LanRoom.persistentGame/resume/saveAndClose |
| 행동·표시 모델 | RoomSync.checkpoint/restore/lookup, SeatProjection.resetHandles, SeatActions의 복구된 발급 상한, DisplayReplica.confirmReconnect, LanTransport의 연결별 nonce |
| 신규 검사 | CheckpointTest, GenerationTest, ReconnectTest, HostRestartTest, LedgerChecks, RestartHost, RestartClient |

**실행 명령.** 단계별 Gradle 명령, 원격 조회, 변경 검사 및 푸시 명령은 ZIP의 `commands.txt`에 있다. 최종 실행에 사용한 명령은 다음과 같다.

```bash
scripts/verify-android-build.sh /workspace/deliverables/phase7-final
git diff --check
git push coop coop/phase7
gh api 'repos/jwgobaek/SPD-Coop/actions/runs?branch=coop%2Fphase7&per_page=2'
```

APK SHA-256은 `c531bb4a0352dd7198d793b49fe1a8ce69505535519ddb5ee60b848ba95c802c`이다. Cloud에 생성한 APK는 `/workspace/deliverables/phase7-final/android-debug.apk`에 있다. Git에 보존한 검증 ZIP의 SHA-256은 `9185756fdbf5a0fd6487767b71131c4a800c3a8223d62139c76782b0e00a476d`이다. [검증 ZIP과 JUnit XML](docs/coop/phase7-cloud-evidence.zip)
