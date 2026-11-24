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
