# 저장 가능한 SYNC1 방과 좌석 재접속

영구 저장을 사용할 방은 엔진 실행 스레드에서 `LanRoom.persistentGame(bind,tcp,udp,seed,saves)`로 연다. 로컬 호스트 좌석도 TCP로 참가한다. `synchronizedGame`과 구형 방은 이전 세션 수명을 유지한다. 단절된 연결을 살리는 과정에서 JOIN이나 새 캐릭터 생성 경로를 호출하지 않는다.

## 최초 참가와 인증

두 좌석은 SYNC1을 협상하고 HOST 또는 JOIN에 성공한 후 P-256 키 쌍을 각 클라이언트에서 생성한다. ENROLL은 자신의 공개 키만 전송하며 준비 전에 성공해야 한다. 같은 키로 양쪽 좌석을 등록할 수 없다. 호스트의 초기 HOST 자격값과 클라이언트의 개인 키는 서로 독립적이다. 서버 저장본에는 각 좌석의 공개 검증 키만 들어간다. 클라이언트 컨트롤러는 개인 키, 세션 UUID, epoch, 좌석 및 Player ID를 앱 전용 저장소에 보관해야 한다. 개인 키 파일을 내보내거나 진단 자료에 넣지 않는다. Android Keystore 연동과 실제 기기 저장소 시험은 후속 작업이다.

새 TCP 연결은 HELLO와 MODE SYNC1을 수행한다. CHALLENGE에 세션 epoch, 좌석 및 Player ID를 넣는다. 호스트는 연결마다 32바이트 난수 nonce를 발행한다. PROVE는 `SPD-RESUME-1 session epoch seat player nonce`의 ASCII 바이트에 대한 SHA256withECDSA 서명을 보낸다. 서명 시도 한 번이면 도전을 폐기하며 유효 시간은 5초다. 연결에 다른 좌석을 중복 배정할 수 없다. 이전 연결이 살아 있으면 중복 오류를 반환한다. 연결 상실을 호스트가 감지하면 해당 연결의 요청은 처리 대상에서 제외한다. LAN의 도청, 초기 키 등록 가로채기 및 서버 위장에 대한 종단간 보호에는 별도의 보안 전송이 필요하다.

| 요청 | 성공 응답 및 효과 |
|---|---|
| ENROLL publicKeyHex | ENROLLED epoch |
| CHALLENGE epoch seat player | NONCE nonceHex |
| PROVE signatureHex | RESUMED seat player actor highestActionID |
| SYNC 0 NEW 및 PAGE | 좌석 전용 전체 공개 상태 |
| LOOKUP epoch actionID | 기존 RESULT, UNKNOWN 또는 ERROR EXPIRED |
| CONFIRM stateVersion checksum | CONFIRMED. 두 좌석이 같은 경계를 확인하면 진행 재개 |
| DO_epoch player seat actor actionID baseVersion turn verb cell itemHandle | 저장 세대 발행 후 RESULT |

TCP Request ID는 연결마다 새로 시작한다. 재접속 클라이언트는 새 DisplayReplica에 전체 모델을 적용하고 로컬에서 계산한 체크섬을 확인한다. 재개 전에도 자신의 receipt를 조회할 수 있다. 상대편도 최신 모델을 확인해야 행동권을 연다. 대기 중 `active=-1`이며 엔진 시계는 전진하지 않는다. 전멸한 세션은 확인 후에도 전멸 상태를 유지한다.

## 행동과 장애 복구

세션, 좌석, epoch를 검증한 다음 실제 엔진의 행동을 실행한다. 확정 엔진 이미지, 최고 Action ID, 각 좌석의 최근 64개 영수증과 공개 핸들 발급 상한을 한 세대에 묶는다. 최고 번호를 소비한 검증 거부도 저장한다. 캐시 조회와 PING은 저장을 유발하지 않는다. 새 확정 성공은 HEAD를 교체하고 디렉터리를 동기화한 후에 응답한다. 이 버전은 행동마다 전체 이미지를 기록한다. 쓰기 실패는 CHECKPOINT_FAILED를 반환하고 이후 행동과 영수증 제공을 차단한다. 불확실한 엔진 상태는 정상 세대로 발행할 수 없다.

재접속 클라이언트는 기존 Action ID로 먼저 LOOKUP을 수행한다. 새 요청은 RESUMED의 최고 번호 다음에서 시작한다. UNKNOWN은 미확정 상태로 표시하며 자동 재실행하지 않는다. EXPIRED는 캐시 밖의 과거 번호로 다시 행동할 수 없다. 오래된 공개 아이템 핸들은 새 요청 권한을 부여하지 않는다. 완료된 명령과 동일한 원본을 재전송하면 엔진 호출 없이 저장한 영수증을 반환한다.

컨트롤러가 이전 세대 사용을 명시적으로 선택하면 새 epoch로 발행한다. 기존 epoch의 재접속은 RECOVERY_EPOCH를 돌려준다. 클라이언트는 진행이 되돌아갔음을 표시하고 승인을 받은 뒤 인증한다. 이전 epoch의 DO 및 LOOKUP은 거부한다. 이 절차에는 호스트 이전과 별도 기기 간 저장 복제가 포함되지 않는다.

## Phase 8 컨트롤러

저장 목록은 CoopSaves.sessions, 메타데이터와 정상·백업 세대 점검은 inspect, 이어하기는 LanRoom.resume, 저장 후 종료는 saveAndClose를 사용한다. 방 검색은 LanClient.discover를 사용하고, 접속 후 사용자 작업은 LanClient와 SeatActions로 제출한다. SeatCredential.reconnect로 인증하고 DisplayReplica.confirmReconnect로 모델을 확인한 뒤 refresh로 최신 입력권을 읽는다. 현 세션이 다른 epoch로 돌아갔을 때는 새 DisplayReplica를 생성한다. LanClient.isOpen은 로컬 소켓의 열림 상태를 반환하며 상대 방의 응답 여부는 PING으로 확인한다. 엔진 실행 스레드에서는 LanRoom.state와 failure를 읽는다. 오류, 연결 상태, 확정 영수증과 공개 모델만 UI로 넘긴다. 실제 파일 I/O와 서명·소켓 호출을 렌더링 스레드에서 수행하지 않는다.

## Phase 8 UI

SeatTickets가 세션·좌석·플레이어·최근 주소·epoch·마지막 미확정 ID를 보관한다. 네이티브 키 저장을 지원하면 파일에는 키 참조만 보관한다. 앱 재실행 후 LOOKUP 및 CONFIRM을 마치고 입력을 연다.

SYNC3 다층 세션을 다시 열면 저장된 활성 층과 휴면 층은 같은 세대에서 복원한다. 재접속 확인은 새 공개 핸들과 같은 층의 전체 스냅샷을 사용한다. 계단 동의는 만료된다. 이전 층 이동의 영수증은 원래 Action ID로 조회한다. HEAD 교체 전에 중단한 이동은 UNKNOWN일 수 있으며 자동으로 재실행하지 않는다.
