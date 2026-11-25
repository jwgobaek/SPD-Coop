# 비공개 체크포인트 백업

개발 브랜치는 GitHub의 `coop/phase8`이다. APK, 빌드 당시의 소스 ZIP, 시험 결과와 화면은 승인된 Google Drive 연결을 통해 `jwgobaek2022@gmail.com` 계정의 `내 드라이브/SPD-Coop`에 보존한다. 저장소 커밋과 Drive 파일은 각각 확인한다.

|위치|내용|
|---|---|
|01_Latest|검증된 APK, SHA256SUMS.txt, BUILD-INFO.txt, README.txt|
|02_APK_Archive|체크포인트별 원본 APK|
|03_Source_Backup|APK 빌드 커밋의 소스, 원본 에셋, 테스트, Wrapper, 라이선스, 빌드 안내|
|04_Test_Evidence|JUnit XML, 빌드·패키지·서명 로그, 소켓 및 장애 회복 기록, 시드와 변경 파일 목록|
|05_Screenshots|실제 Desktop 사진, 언어, 빌드, 시험 배치 또는 정상 시작 여부|
|06_Documentation|프로젝트 문서와 동기화·저장·재접속 명세|

## 체크포인트 발행

1. `git ls-remote coop refs/heads/coop/phase8`과 현재 HEAD를 기록한다. 소스 변경을 커밋한 뒤 해당 커밋에서 `scripts/verify-android-build.sh <새 출력 폴더>`를 실행한다. 기존 APK를 보존할 때는 원래 build.log, commit.txt, apk.txt, JUnit XML, SHA256SUMS로 대조한다.
2. 빌드 SHA를 사용해 `git archive --format=zip -o source.zip <빌드 SHA>`로 대응 소스를 생성한다. 용량을 줄이기 위해 이미 별도 보존한 과거 증거 ZIP을 제외할 수 있다. Wrapper JAR, 원본 에셋, 저작권·라이선스와 빌드 스크립트를 포함한다. APK와 검증 자료의 커밋이 다를 경우 두 커밋을 모두 기록한다.
3. 각 산출물의 SHA-256과 바이트 수를 기록하고 ZIP 내부 목록을 점검한다. 키스토어, 비밀번호, 개인 재접속 자격, 인증 토큰, 게임 세이브·세션 디렉터리를 제외한다. 실제 기기 검증은 ANDROID-VALIDATION.md에 별도 기록한다.
4. Drive 프로필을 확인하고 계정이 일치하는 연결만 사용한다. 부모 폴더의 실제 자식 목록에서 같은 이름의 폴더를 확인한다. 체크포인트 경로는 빌드 날짜·단계·빌드 커밋으로 정한다. 동일 이름의 다른 바이트는 덮어쓰지 않는다. 로컬 파일 업로드 또는 검증된 Drive 파일의 복사를 사용한다.
5. 업로드한 ID로 이름, 크기, 부모 폴더 및 공유 상태를 다시 읽는다. 내려받을 수 있는 파일은 다시 SHA-256을 계산해 원래 값과 비교한다. 기존 APK를 새로 빌드한 파일로 대체하지 않는다.
6. GitHub 일반 푸시와 원격 SHA 확인, 아카이브 업로드와 검증을 마친 뒤 최신 버전 정보를 발행한다. 라이선스와 소스는 같은 빌드의 APK와 함께 제공할 수 있도록 한다. 기본 접근 범위는 비공개다.

## 적용 시점과 기록

주요 기능·그래픽·저장 스키마·장애 복구 변경, APK 검증, 구역 완료 및 세션 종료 시 확인한다. 동일 커밋·동일 해시는 재사용한다. 누락된 과거 바이너리와 나중에 재빌드한 바이너리는 별도 표시한다.

* `UPLOADED_AND_HASH_VERIFIED` — 원격 파일의 SHA-256과 크기가 원본과 일치한다.
* `UPLOADED_METADATA_VERIFIED` — 원격 이름·크기·위치·비공개 상태를 확인했다. 로컬 해시는 별도로 제공한다.
* `UPLOAD_FAILED` — 시도한 파일과 오류, 남겨둔 Cloud 다운로드 경로를 기록한다.
* `NOT_RUN` — 연결·자료·환경이 없어 실행하지 못한 검사와 재시작 지점을 기록한다.

Drive 오류가 발생해도 개발 및 Cloud 빌드를 계속한다. 검증된 APK, 해당 소스 ZIP, 증거 ZIP과 체크섬을 Cloud 다운로드 폴더에 유지한다. 복구 과정에도 기존 백업과 접근 권한을 보존한다.
