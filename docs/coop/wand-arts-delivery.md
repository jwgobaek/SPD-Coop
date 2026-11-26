# 여섯 기본 직업 APK와 통합 ZIP

개발 시작 커밋은 17bfa4a158c68bcdd2a11c4570d958f0f97f24b7이며 배포 APK의 소스는 dcd7a771a8b6fddf4636018e233157971773fbe2다. 패키지 org.spdcoop.dungeon.indev, versionCode 922, 버전 4.0.2-INDEV-wand-arts다. APK는 35,354,538바이트이며 SHA-256은 22f5aa08b2629e6a24a5220933e0ba93346b16b036fbc567a0ccc1c03686081e다. 기존 인증서 0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53으로 v1 및 v2 서명을 검증했다. [배포 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view)

통합 ZIP의 source 디렉터리는 위 APK 빌드 커밋을 담는다. documentation과 evidence에는 이후의 시험, JUnit XML, 한국어·영어 Desktop과 실제 Android 자료를 담는다. [통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) · [기능·저장·후속 작업](wand-arts-results.md) · [Android 및 PC 실측](wand-arts-android.md)

## 발행 확인

ZIP 검증 자료의 커밋은 18ef1fc13a09acad56c0ad3772c48e9441895c8d다. 실제 APK 소스 파일 2,326개와 체크섬 2,637개가 일치했다. ZIP에는 JUnit XML 72개(265개 시험), Android 화면 19개와 Desktop 화면 18개가 들어 있다. 파일명·민감값 검사와 ZIP 구성원별 체크섬 검사를 통과했다.

| 기존 파일 | 비로그인 다운로드 바이트 | SHA-256 | 이번 전송 |
| --- | ---: | --- | ---: |
| APK | 35,354,538 | 22f5aa08b2629e6a24a5220933e0ba93346b16b036fbc567a0ccc1c03686081e | 1 |
| ZIP | 76,204,224 | c30d60ba50c1b8b71d90a0b25a20ed22cc44cf645fdefc8d0e3bb610fe9f092e | 1 |

두 결과물을 익명 클라이언트로 끝까지 내려받아 크기와 SHA-256을 로컬 결과와 대조했다. 기존 파일 ID와 상위 폴더, anyone/reader 및 allowFileDiscovery=false를 확인했다. 직전 리비전은 각각 35,345,914바이트와 74,581,688바이트의 기존 배포물로 기록돼 있고 keepForever=false였다. 연결 도구는 리비전 목록·조회만 제공한다. 장기 보존과 실제 롤백은 미검증이다.
