# SYNC36 정식 교체와 대응 소스

2026-11-27에 승인된 APK와 Android 인수 증거를 포함한 통합 ZIP을 기존 01_Latest 파일에 각각 한 번 갱신했다. 두 개 모두 `09c9417d6ad07ddc065051136b09c0103dd3d664` 소스에 대응한다. 신규 파일 생성은 0회다.

| 관리 파일 | 바이트 | 실제 비로그인 다운로드 SHA-256 |
|---|---:|---|
| [APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) | 37,281,544 | 5addbd614f5d627aa205bc7d18a924ac731a4274bef2afcecd2ae96c5044b152 |
| [통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view) | 74,999,032 | a12d81368c49c2ba17fdc1e3bb09c8fed5f21997378e2caa726f93e3a6e771c4 |

후보 전달용 승인된 비공개 기능이 없어 사용자가 이 해시에 지정한 검증 전 발행 예외를 적용했다. 기존 APK를 교체한 뒤 두 Android 내부 Chromium에서 직접 내려받고 설치했다. 실제 두 설치본을 검사하고 인수한 뒤 ZIP을 교체했다. 각 파일 ID와 다운로드 권한을 대조했다. 두 개의 링크를 가진 사용자는 reader이고 allowFileDiscovery는 false다. APK의 기존 추가 편집자 권한도 유지했다.

발행 검사기는 성서 선택의 두 부팅 ID와 10개 결과를 확인했다. ZIP의 source 파일 2,363개가 빌드 커밋과 바이트 단위로 일치했고 체크섬 목록 2,523개를 확인했다. 에셋·라이선스·빌드 정보·338개 JUnit XML 시험 결과·관측표·화면을 포함했다. APK와 개인 저장, 시험 티켓, 개인 키는 포장에서 제외했다. 검증 내용과 미실행 조합은 [Android 인수 기록](shared-growth-android-acceptance.md)에 있다.

Cloud 후속 시험 종료 무렵 GB2에서 CPU 41%, 가용 RAM 2,532/16,085 MiB였고 Device-2 및 Device-2-1 창을 가진 MuMuNxDevice 두 프로세스가 모두 응답했다. 이 관측에는 새 ADB 연결을 사용하지 않았다.
