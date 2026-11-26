# 인장·후반 갑옷 APK와 후속 수정

Drive에 발행하고 두 Android에 실제 설치한 소스는 `07b18899f48b22142a602bbf095cddcef621a033`이다. 패키지는 `org.spdcoop.dungeon.indev`, 버전은 `4.0.2-INDEV-seal-arts` (922)다. 실제 크기는 35,369,779바이트, SHA-256은 `4e580728fa22a73ee748f749e154edfe3c695e5aaeacbe9f1abe388269b8ef35`이다. 기존 인증서 SHA-256 `0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53`에 대해 v1·v2 서명을 검사했다. [관리 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view)

배포판의 성서 물품 선택은 Android에서 게스트 앱 종료를 일으켰다. 후속 소스 `9969d235a85c57019ba2963f873ab492367af164`에서 원본 확인창을 닫은 뒤 새 선택창을 협동 게임 장면에 붙이도록 수정했다. 이번 세션의 APK 전송 1회는 위 배포본에 사용했다. 후속 수정은 독립 Cloud 결과물로 보존한다. [실제 기기 관측](seal-arts-android.md)

통합 ZIP의 source/는 설치 APK의 정확한 빌드 커밋이다. documentation/에는 후속 재현기, 검증 결과 및 지원표가 있으며 evidence/에는 배포판 JUnit XML, 서명 검사, Cloud 실행 및 실제 화면, 후속 화면 수정 패치를 수록한다. 시험 계정·좌석 키와 저장 세대는 로컬 비공개 검증에만 사용했다. [관리 통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)

수정 APK는 37,306,668바이트, SHA-256 `66e1da255f50026f91db5cf89f9633842782a72e28b0f4191758cb0a428a1547`이다. 같은 인증서의 v1·v2 서명과 전체 JUnit 278/278을 확인했다. Cloud의 SPD-Coop-9969d23에 보존했다. GB2 설치본은 위 Drive 배포판이다.

## 비로그인 재다운로드 및 이전 리비전

| 기존 파일 | 바이트 | 비로그인 다운로드 SHA-256 | 내용 갱신 |
| --- | ---: | --- | ---: |
| APK | 35,369,779 | `4e580728fa22a73ee748f749e154edfe3c695e5aaeacbe9f1abe388269b8ef35` | 1회 |
| 통합 ZIP | 73,920,737 | `9218e329b5a66af74d75cb4c572fab768eed5fd48ac04dbfb4213dd3c13d1452` | 1회 |

두 파일에서 기존 ID·상위 폴더·anyone/reader·allowFileDiscovery=false를 대조했다. 자격 증명 없는 별도 HTTP 다운로드의 바이트 수와 SHA-256이 Cloud 원본과 일치했다. 각 파일의 직전 리비전 ID가 발행 전 조회한 값과 일치했다. APK의 직전 크기는 35,359,365바이트, ZIP은 73,511,823바이트다. 두 직전 리비전 모두 keepForever=false다. 연결 도구의 리비전 조회·가져오기는 지원되며 영구 고정 기능은 제공되지 않았다.

통합 ZIP의 빌드 소스 파일 2,338개를 `07b1889`와 대조하고 체크섬 2,736개를 검증했다. 검증·문서 커밋은 `7e716c3917a93878edd549363f6c59c6c33e0c4a`다. 배포 및 후속 수정 빌드의 JUnit XML 각 74개, Android 실제 화면 20개, Desktop 실제 화면 8개를 수록했다. 먼저 닫힌 선택창에 대한 수정은 패치로도 포함했다. 이 다운로드 대조 결과는 발행한 ZIP의 자체 해시가 확정된 후 Git에 기록했다.
