# 서리·감지·전직 배포 확인

개발을 시작한 원격 커밋은 `7743e2cb217fa9c1c38e0d0f1f78aac59afbc2f1`이다. 빌드 소스는 `47000714604bde08c15d80e22a813e6a9f173f02`이며 ZIP에 포함한 최종 시험·문서의 커밋은 `671f4835e901f261c72b5ab8f654ff19fd749936`이다. 배포 버전은 `4.0.2-INDEV-frost-senses`, versionCode 922, 패키지는 `org.spdcoop.dungeon.indev`다. 개발 인증서 SHA-256은 `0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53`이며 v1·v2 서명을 검증했다.

| 기존 파일 | 비로그인 다운로드 바이트 | SHA-256 | 내용 갱신 횟수 |
| --- | ---: | --- | ---: |
| [APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) | 35,359,365 | `dd6c9d185a02e841b5458892bd9318dd5f1efff5c607363ddd80cf7b88a9b0c3` | 1 |
| [통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) | 73,511,823 | `4cd2d7e57b609e605cef403383a66516c1e4350a4bdc4ae49972959da550a2c1` | 1 |

두 파일 모두 최종 발행 뒤 익명 HTTPS 클라이언트로 끝까지 내려받았다. 파일 ID, 원래 01_Latest 폴더, anyone/reader 및 allowFileDiscovery=false를 재조회했고, 크기와 SHA-256이 Cloud 원본과 일치했다. APK를 올리기 전 첫 파일 경로 조회는 전송 전에 실패했다. 기존 리비전이 그대로임을 확인하고 커넥터에서 읽을 수 있는 위치에 파일을 복사한 뒤 내용 갱신 1회를 수행했다.

APK의 직전 리비전(35,354,538바이트)과 ZIP의 직전 리비전(76,204,224바이트)은 발행 전 최신 리비전 ID와 일치했다. 두 직전 리비전의 keepForever는 false였다. 연결 도구는 리비전 조회를 제공한다. 영구 보존 보장 및 실제 롤백 시험은 없다.

통합 ZIP은 APK 소스 파일 2,329개를 빌드 커밋과 대조했다. 검증기가 확인한 체크섬은 APK를 포함해 2,632개다. 원본 에셋·라이선스, JUnit 271개 시험의 XML 73개, 한국어·영어 Desktop 화면 5개와 Android 화면 19개가 들어 있다. 별도 배치형 시험의 개인 키·저장 세대는 제외했다. Android 자료를 추가한 뒤 최종 ZIP을 한 번 발행했다. [기능별 범위와 후속 개발](frost-senses-results.md) · [Android 실측 및 PC 정리](frost-senses-android.md)
