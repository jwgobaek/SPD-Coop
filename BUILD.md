# Cloud에서 빌드하기

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
