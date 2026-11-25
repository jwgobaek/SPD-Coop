# Cloud에서 빌드하기

## 그래픽 대조 실행

JDK 21, 기존 Android SDK 및 실행 중인 Cloud X11 디스플레이를 사용한다. `scripts/capture-graphics-desktop.sh <공식 worktree> <변경 전 worktree> <출력 경로>`가 공식 Desktop JAR에 외부 드라이버를 컴파일하고 분리된 앱 프로필에서 원본·협동 장면을 촬영한다. 실제 그래픽 APK와 전체 JVM 회귀는 기존 `scripts/verify-android-build.sh <새 출력 경로>`로 검증한다. [비교 조건](GRAPHICS-PARITY.md)

## 현재 하수도 식물·복원 빌드

JDK 21과 기존 Android SDK, Gradle Wrapper를 사용한다. 브랜치의 검증 대상 커밋을 별도 worktree에 체크아웃한 뒤 `bash scripts/verify-android-build.sh <새 출력 디렉터리>`를 실행했다. 해당 디렉터리에 APK, 패키지·서명 확인, SHA-256, 커밋, 빌드 로그와 JUnit XML이 생성된다. 버전은 `4.0.2-INDEV-sewer-plants`, versionCode는 922다. Desktop에서는 `:desktop:compileTestJava :desktop:release` 후 X11에서 `CoopRenderSmoke`의 garden 시나리오를 양 언어로 실행한다. [검증 자료](docs/coop/sewer-plants-evidence.zip)


## 하수도 1–5층 빌드

독립 checkout에서 scripts/verify-android-build.sh를 실행하면 현재 커밋의 기존·신규 JUnit, APK, 패키지·서명·SHA-256 검사를 함께 기록한다. 이전 스키마 호환성은 각각 92ebf94와 9b6f58a checkout을 대상으로 scripts/verify-alpha-migration.py와 scripts/verify-sewer-migration.py로 검증한다. Desktop 검증기는 CoopRenderSmoke의 deep, kit 모드를 분리된 앱 프로필에서 실행한다. 셸에서 여러 빌드를 동시에 돌릴 때에는 각 빌드에 별도 Git worktree와 출력을 사용한다. [실행 결과](docs/coop/sewer-depths.md)

## 하수도 공통 마법 부여·함정·철 열쇠

기준 회귀에는 `./gradlew --offline :core:cleanTest :core:test`, Android 검증에는 `scripts/verify-android-build.sh /workspace/deliverables/sewer-effects-5015b18`을 사용했다. APK는 해당 디렉터리의 android-debug.apk이며 SHA-256은 `68ebaca303406837f17dbac41f729f114fd6e620ec771e15a3b94ea3beb793f0`이다. JUnit 100건, 패키지·기존 개발 서명 인증서·v1/v2 검사를 통과했다. 시드표는 scripts/coop-sewer-survey.py로 JUnit XML에서 생성한다. Desktop은 :desktop:compileTestJava와 :desktop:release 후 LWJGL CoopRenderSmoke의 golden, hazards, unstable 모드를 격리한 X11 프로필에서 실행했다. 호스팅 Actions 및 실기기 검증은 NOT RUN이다.

기준은 공식 v4.0.2의 build.gradle, android/build.gradle, gradle-wrapper.properties다. 공식 docs/getting-started-android.md는 Studio 중심이며 docs/getting-started-desktop.md는 JDK 21을 권장한다. 이 프로젝트의 빌드는 Gradle CLI로 수행한다.

|항목|기준|
|---|---|
|빌드 JDK / Java 소스·타깃|Temurin 21.0.13.1+2 / Java 11|
|Wrapper / Android Gradle Plugin|9.5.0 / 9.2.0|
|libGDX / controllers|1.14.0 / 2.2.4|
|compile / target / min SDK|36 / 36 / 21|
|SDK / Build Tools|android-36 rev 2 / 36.0.0|
|버전|4.0.2-INDEV (922)|

Linux x86_64 Cloud에서 JDK가 없는 경우 다음을 실행한다. 일반 사용자 쓰기 권한이 있는 새 도구 디렉터리를 사용한다.

```bash
set -eu
tools_dir=$(mktemp -d)
export JAVA_HOME="$tools_dir/jdk" ANDROID_HOME="$tools_dir/sdk" GRADLE_USER_HOME="$tools_dir/gradle"
mkdir -p "$JAVA_HOME" "$ANDROID_HOME"
curl -fsSL 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13.1%2B2/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz' -o "$tools_dir/jdk.tar.gz"
echo "bb8a93f9ae6e7a693b2b721e85dbeff26ae1a77447985a8d7afcdd9c7cf6222e  $tools_dir/jdk.tar.gz" | sha256sum -c -
tar -xf "$tools_dir/jdk.tar.gz" --strip-components=1 -C "$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
scripts/setup-android.sh
./gradlew --no-daemon :core:test :android:assembleDebug
scripts/inspect-apk.sh android/build/outputs/apk/debug/android-debug.apk org.spdcoop.dungeon.indev
```

setup-android.sh는 SDK 라이선스 프롬프트에 동의하여 플랫폼과 Build Tools를 설치한다. 관리형 프록시 환경에서는 승인된 네트워크 권한으로 실행하고 Java의 https.proxyHost/https.proxyPort 및 http.proxyHost/http.proxyPort를 환경 프록시에 맞춘다. 이 실행에서는 시스템 CA 저장소 /etc/ssl/certs/java/cacerts를 지정했다. 원본 비교 빌드는 별도 v4.0.2 checkout에서 :android:assembleDebug를 실행하며 기대 ID는 com.shatteredpixel.shatteredpixeldungeon.indev다.

GitHub의 사용자 소유 저장소에 브랜치를 연결하면 push/PR/Actions의 Android debug 수동 실행으로 검증한다. 로그와 APK는 해당 run의 Artifacts에서 받는다. JUnit 결과가 없는 초기 태그에서 :core:test는 NO-SOURCE다. Phase 2에서 실제 테스트가 들어오면 결과가 같은 Artifact로 수집된다. CI 설치본 간 업데이트에는 동일 개인키가 필요하다. 장기 키 생성·백업 후 GitHub 환경 secrets를 통해 주입할 예정이며 현재 디버그 키는 일시적이다.

## 개발 브랜치 CI 및 iPad/Cloud 대체 실행

2026-11-24에 master에는 android.yml이 없고 coop/phase2에는 있음을 원격에서 확인했다. workflow_dispatch의 최초 수동 실행에는 기본 브랜치에 있는 워크플로가 필요하다. push 이벤트는 푸시한 ref의 워크플로를 사용한다. 같은 저장소 안의 초안 PR #1에는 opened/synchronize 이벤트도 적용된다. master에 병합하거나 기본 브랜치를 바꿀 필요 없이 실행 가능한 구성이다.

GitHub는 포크 워크플로를 기본적으로 비활성화한다. 이 저장소의 실제 설정값은 Cloud 통합의 관리 설정 읽기가 403이라 확인하지 못했다. 실행 0건이나 탭의 시작 안내만으로 활성 상태를 판정할 수 없다. 저장소 소유자가 로그인한 브라우저에서 저장소의 Actions 탭에 활성화 버튼이 보이면 그 저장소에서 활성화한다. 정책 화면의 직접 경로는 https://github.com/jwgobaek/SPD-Coop/settings/actions 이다. Android debug에 필요한 액션은 actions/checkout, actions/setup-java, gradle/actions/setup-gradle, actions/upload-artifact이다. 기존 정책 변경은 별도 판단 사항이다.

다음 정상 개발 커밋의 push에서 run의 head SHA, conclusion, APK가 포함된 Artifact를 차례로 확인한다. 이미 승인된 실행이 없다면 Re-run 메뉴도 없다. workflow_dispatch를 등록하기 위한 master 변경에는 사용자 승인이 필요하다. 포크의 실행이 대기 중인 동안 Cloud와 Actions는 같은 스크립트를 사용할 수 있다.

```bash
scripts/setup-android.sh
scripts/verify-android-build.sh /tmp/spd-build-report-001
```

검증 스크립트의 첫 번째 인수로 새 출력 디렉터리 경로를 지정한다. 커밋, 실제 JUnit 결과, 빌드 로그, 검사 결과, APK와 SHA256SUMS가 생성된다. 스크립트는 비어 있거나 실패·오류·스킵이 있는 JUnit 결과를 거부한다. GitHub Actions Artifact에는 GitHub run ID가 있어야 한다.

## Phase 7 실제 프로세스와 파일 시스템 검사

전체 검증은 기존 scripts/verify-android-build.sh를 사용한다. 빠른 재검증은 다음 명령으로 수행한다.

```bash
./gradlew :core:cleanTest :core:test --tests '*CheckpointTest' --tests '*GenerationTest' --tests '*ReconnectTest' --tests '*HostRestartTest'
```

JUnit이 임시 앱 저장 경로와 독립된 호스트 및 클라이언트 JVM을 관리한다. Runtime.halt는 테스트 자식 프로세스에만 적용한다. 증거에는 JUnit XML·빌드 로그·APK 검사 및 체크섬만 수집한다. 임시 게임 디렉터리와 양쪽 좌석의 개인 키 파일은 업로드 대상에서 제외한다.

## Phase 8 협동 알파

보존한 UI 알파는 4.0.2-INDEV-p8, 일반 문·소형 식량 확장판은 4.0.2-INDEV-p8c1이다. 두 APK의 versionCode는 922이며, 패키지는 org.spdcoop.dungeon.indev다. scripts/verify-android-build.sh에 새 출력 디렉터리를 전달한다. 빌드 시 Git 리비전 12자리가 진단 화면에 포함된다. 다른 Gradle 작업이 같은 checkout에서 클래스 파일을 재생성하면 장시간 실행 중인 테스트 JVM이 클래스를 잃을 수 있으므로 이 검증은 독립 checkout에서 진행한다. 디버그 업데이트 설치에는 동일한 서명자가 필요하다.

Desktop 렌더링 시험은 X11 가상 화면에서 실제 LWJGL 창과 별도 게스트 JVM을 사용한다. 테스트 전용 임시 XDG_DATA_HOME과 스크린샷 출력 위치를 사용한다.

```bash
./gradlew :desktop:compileTestJava :desktop:release
XDG_DATA_HOME="$(mktemp -d)" LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null \
  java -Duser.language=en -cp 'desktop/build/classes/java/test:desktop/build/libs/desktop-4.0.2.jar' \
  com.shatteredpixel.shatteredpixeldungeon.desktop.CoopRenderSmoke /tmp/spd-screens
```

DISPLAY와 XAUTHORITY는 실행 환경에서 제공하는 가상 X 화면을 가리켜야 한다. 초기 지도는 시드 402다. 전투 경계·부활 장면은 정상 이동과 식사 이후 테스트가 엔진 소유 스레드에서 배치한 쥐·앙크·치명상을 사용한다. 부활과 공격 입력은 실제 렌더링된 지도와 TCP를 통과한다.

같은 Desktop 경로를 한국어로 검증하려면 JVM 언어를 -Duser.language=ko -Duser.country=KR로 지정한다. 테스트는 화면에 표시된 해당 언어의 버튼을 찾아 누른다.

## 마법사 및 다층 검증

기존 환경에서 `scripts/verify-android-build.sh <새 결과 디렉터리>`를 실행한다. JUnit은 실제 하수도 왕복과 두 번의 호스트 강제 종료를 포함한다. Desktop 시험에는 CoopRenderSmoke 출력 디렉터리 뒤에 `mage` 또는 `floors`를 지정한다. 별도 프로필을 사용한다. 다층 Desktop 장면은 원본 계단 옆에 두 영웅을 배치한 렌더링 시험이다. 독립 JVM의 HostRestartTest는 실제 이동과 전투로 계단에 도달한다.

기존 알파 커밋 92ebf94를 별도 worktree에 체크아웃한 뒤 `python3 scripts/verify-alpha-migration.py /path/to/that/worktree`로 두 버전을 실행할 수 있다. 개인 테스트 프로필과 키는 임시 디렉터리에서 만들고 종료 시 정리한다. JUnit의 schema1-engine.gz에는 같은 알파가 생성한 테스트 던전의 엔진 부분만 들어 있다. 검증 ZIP에는 개인 프로필과 키 파일을 넣지 않는다.

## 하수도 특수 방 체크포인트

기준 회귀는 `./gradlew --offline :core:test`, APK와 서명 검사는 `scripts/verify-android-build.sh <새 출력 디렉터리>`로 실행한다. 실제 Desktop 시험은 위 CoopRenderSmoke에 `sentry` 또는 `ghost`를 마지막 인수로 전달한다. 언어별로 새 XDG_DATA_HOME을 사용한다. 이 시험은 원본 생성 방에 두 영웅을 시험 배치한 뒤 실제 Noosa 입력을 사용한다. 전체 하수도 생성·계단 왕복 조사표는 성공한 결과 디렉터리에 `python3 scripts/coop-sewer-survey.py <결과 디렉터리>/junit`을 실행해 재생성한다.

## 하수도 S1 미믹·석상 빌드

APK의 versionName은 4.0.2-INDEV-sewer-guardians, versionCode는 922, 패키지는 org.spdcoop.dungeon.indev다. scripts/verify-android-build.sh는 실행한 소스 커밋·전체 JUnit XML·서명·크기·체크섬을 같은 출력 디렉터리에 기록한다. 독립 JVM 시험에는 실제 loopback 소켓 접근이 필요하다. Desktop 회귀는 :desktop:compileTestJava 및 :desktop:release 후 별도의 X11 디스플레이에서 CoopRenderSmoke <출력> mimic, crystal, statue를 각각 en·ko로 실행한다. 실행 중인 프로세스가 사용 중인 Desktop JAR은 다시 만들지 않는다.

시드 자료는 최종 XML 디렉터리를 scripts/coop-sewer-survey.py에 전달해 다시 생성한다. --gates 옵션은 원본 22개, 추가 연속 22개와 희귀 지정 시드 1개를 구분한다. 디버그 인증서가 달라지면 기존 앱에 업데이트 설치할 수 없다. APK와 서명 키는 Git 추적에서 제외한다.
