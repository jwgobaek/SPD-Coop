# SYNC40 배포 및 무결성 확인

2026-11-27 지정 후보 0998031을 기존 Drive APK에 한 번 갱신했다. 두 Android 부팅의 필수 인수 92건 통과 후 정확히 같은 커밋의 대응 ZIP을 기존 ID에 한 번 갱신했다. 새 Drive 파일은 0개다. 배포 권한은 이 두 전송에 사용했다. [Android 세부 결과와 미감정 주문서 표시 결함](cauldron-android-acceptance.md)

| 파일 | 바이트 | 비로그인 HTTPS 응답 | 다운로드 SHA-256 |
|---|---:|---|---|
| APK | 37,367,918 | 200 | `a684461eb4ac34e988153059ce8ada614aa689f2fd52b53ff4b0280453252797` |
| 대응 ZIP | 74,501,007 | 200 | `30ec67b11e189626c7fc8a3fcaef1c871984c0ce4b0020d0978225b95b3b32af` |

두 다운로드를 인증 쿠키 없이 끝까지 받아 해시를 계산했다. 두 Android 설치본도 APK와 같았다. 검사기는 ZIP의 소스 2,399개를 빌드 커밋의 git archive와 대조하고 체크섬 2,768개를 검증했다. ZIP에는 원본 에셋·라이선스·빌드·DEX·서명 정보·전체 JUnit XML·Android 관측을 담았다. 대응 소스는 `0998031843b59021e54fdf3d9d4b601eff9c9ce9`다.

두 기존 파일은 anyone/reader, allowFileDiscovery=false로 조회됐다. APK의 기존 추가 writer와 각 파일의 owner 권한을 확인했다. [배포 APK](https://drive.google.com/file/d/1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2/view) [통합 ZIP](https://drive.google.com/file/d/1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8/view)

후속 빌드 7ba9bb3은 별도 Cloud APK다. 실제 Android 인수와 Drive 발행은 NOT RUN이다. [후속 기능과 검증](seed-alchemy-results.md)
