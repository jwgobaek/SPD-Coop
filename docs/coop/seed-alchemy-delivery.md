# SYNC41 Drive 발행 기록

이번 한정 승인으로 기존 APK 파일의 내용을 한 번 갱신했다. 두 MuMu 내부 Chromium으로 내려받아 기존 데이터 위에 설치했다. 후보의 개발 인증서 SHA-256은 `0dd9070b5f17192a8b07e5ddb6735161b0d2bb6e49013e4d61ca5ee6216ddb53`이다. 패키지는 `org.spdcoop.dungeon.indev`, versionCode는 922다. classes2.dex에서 성서 대상 선택의 명시적 배열 타입 저장을 재검사했다.

| 파일 | 실제 대응 소스 | 비로그인 다운로드 바이트 | SHA-256 |
| --- | --- | ---: | --- |
| APK | `7ba9bb3` | 37,378,536 | `7a26db27a25f984c8c808b71e9b338762b6a4ff71623adba54c6ce3b57484990` |
| ZIP | `7ba9bb3` | 77,693,505 | `1b1130b3fca9ac0c3e007158f1302df6a7bf4275424cbb4e3e2068301174b643` |

APK ID `1UhjuelcgZwugUyiGmAHco_dXdnTFuDZ2`, ZIP ID `1nxC-3dq29HUmt7pSuoYP8JGqUALNVvW8`. 각 파일의 링크 뷰어는 anyone/reader, allowFileDiscovery=false였고 APK의 기존 편집자도 확인했다. SYNC41의 필수 118/118 인수와 검사기 통과 뒤 기존 ZIP을 한 번 갱신했다. 이번 세션의 갱신은 APK 1회, ZIP 1회이며 신규 파일은 0개다. 두 업데이트가 플랫폼에서 완료됐고 최종 비로그인 다운로드에서 위 크기와 SHA-256을 각각 실측했다. APK의 추가 편집자와 두 파일의 기존 공유 권한도 재확인했다. 후속 SYNC42 APK는 Cloud에 보존한다. [실제 Android 인수](seed-alchemy-android-acceptance.md)

ZIP의 source 파일 2,408개를 커밋 `7ba9bb3bfd0d876446d55c400a7fea32fb691266`의 원본 바이트와 대조했다. 전체 검사기의 체크섬 2,889개가 일치했다. 원래 빌드 및 독립 재실행의 394/394 JUnit XML, DEX와 서명 정보, 역할별 Android 관측과 화면은 evidence에 포함했다. 이전 SYNC40 ZIP은 74,501,007바이트, SHA-256 `30ec67b11e189626c7fc8a3fcaef1c871984c0ce4b0020d0978225b95b3b32af`였으며 당시 대응 소스는 `0998031`이었다.
