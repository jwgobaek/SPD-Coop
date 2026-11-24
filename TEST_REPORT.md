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
