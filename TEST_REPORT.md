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
