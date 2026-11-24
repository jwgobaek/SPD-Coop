# Phase 5 동기화 프로토콜 SYNC1

## 방과 연결

호스트의 엔진 스레드는 `LanRoom.synchronizedGame(bind,tcp,udp,seed)`로 방을 만들고 pump한다. 각 클라이언트는 `LanClient.synchronizedGame(address)`로 연결한다. 기존 v1 길이 헤더와 HELLO 뒤에 MODE SYNC1을 보내고 같은 응답을 확인한 뒤 HOST 또는 JOIN을 보낸다. 일반 v1 방과 동기화 방은 서로의 참가 방식을 거부한다. 동기화 방에서는 기존 ACT를 쓸 수 없다. UDP 광고와 로비의 SELECT·READY·START는 Phase 4와 같다.

엔진은 두 영웅을 생성해 첫 입력 차례가 준비되면 버전 1을 확정한다. 이후 하나의 DO를 실행하고 다음 입력 차례가 준비됐을 때 양쪽 공개 상태를 함께 확정한다. 사이에 처리된 몹·버프·회복도 이 경계에 포함한다. TURN과 stateVersion은 별도 카운터다. 재생 시간 재조정, 조회·PING, 사전 검증 거부, 동일 결과 반환은 버전을 소모하지 않는다. 단절·엔진 중단에 따른 일시정지는 한 번 확정한다.

## 행동과 결과

```text
1 rid session DO player seat actor actionID baseVersion turn verb cell itemID
1 rid RESULT actionID seat actor requestedTurn requestedBase committedVersion outcome
```

바깥 rid는 연결마다 증가하는 전송 번호다. 동일 논리 행동을 재전송할 때 rid만 새 번호가 된다. actionID는 세션·좌석별 양의 long이다. 입력은 2,048바이트 이내이며 DO만 13개 토큰을 쓴다. 문자열 표현이 다른 숫자도 파싱한 수치로 비교한다. 동기화 방의 아이템 ID는 v1 STATE의 턴별 핸들과 구분한다.

1. 세션 UUID, 실제 연결의 좌석, player 및 해당 영웅의 Actor ID부터 확인한다.
2. 좌석별 최근 64개의 행동 기록에서 ID를 찾는다. 같은 base·turn·verb·cell·item이면 저장된 RESULT를 반환하고 다른 내용이면 ERROR CONFLICT다. 기록이 밀려났지만 처리한 최고 ID 이하인 요청은 ERROR EXPIRED다. 이전 세션이나 다른 좌석의 결과를 조회할 수 없다.
3. 새로운 ID면 버전·턴·소유권과 실제 CoopSession.act의 이동·공격·소비 조건을 검사한다. RESULT의 REJECT_VERSION/TURN/ITEM/INVALID/ACTION/PAUSED는 거부다. 인증된 새 ID의 거부 결과도 64개 한도에 포함된다. 사용하지 않은 작은 번호를 나중에 제출할 수 없다.
4. APPLIED는 정상 확정, APPLIED_STOPPED는 승인된 행동 후 엔진이 정상 중단 조건을 보고한 경우다. HALTED_UNCERTAIN은 실행이나 확정 중 예외가 발생해 일부 효과가 이미 반영됐을 수 있다는 뜻이다. 재실행이나 롤백을 시도하지 않는다. PAUSED 모델은 행동권과 플레이 중의 지도를 제거한다. 공개 사유는 DISCONNECTED, ENGINE_STOPPED, ENGINE_ERROR 중 하나이며 예외 전문은 호스트에만 둔다.

영수증에는 좌표·다른 사람의 인벤토리·전체 상태를 싣지 않는다. 마지막 확정 경계가 자신의 행동이면 자신의 모델에만 receipt 행이 들어간다. 이 캐시와 최고 ID는 방의 메모리에 있다. 캐시 밖의 과거 결과나 완전히 끊어진 연결에서의 좌석 복원은 지원하지 않는다.

## 상태 전송

| 요청 | 응답 |
|---|---|
| SYNC 0 NEW | 해당 좌석의 현재 전체 스냅샷 첫 PART |
| SYNC version sha256 | 같은 상태이면 CURRENT version sha256. 직전 버전과 해시가 일치하면 변경분 PART. 그 밖에는 RESYNC |
| PAGE transfer index | 같은 연결·좌석의 활성 전송에서 요청한 PART. 만료·새 확정 경계 또는 전송 교체 후에는 ERROR TRANSFER |

PART는 `PART transfer index count transferSHA256 segment`이다. 인덱스는 0부터 시작한다. 조각은 최대 1,400 ASCII 바이트, 최대 64개, 전체 최대 89,600바이트, 공개 상태 최대 2,048행이다. 마지막 조각을 제외한 길이는 일정하다. 좌석당 현재/직전 공개 상태와 전송 본문 하나만 유지한다. 조립 제한은 5초다. 커밋되면 두 좌석의 이전 전송을 무효화한다. 스냅샷 생성·변경분 생성은 불변 버전을 사용한다. TCP의 길이·속도·큐·열린 연결 제한은 기존 명세를 따른다.

본문은 `S|session|seat|0|version|stateSHA256|operations` 또는 `D|session|seat|base|version|stateSHA256|operations`이다. D는 한 커밋만 전진한다. 일반 동작은 키 단위로 달라진 값만 보낸다. 아주 큰 변경분은 상한 내 전체 스냅샷으로 대체할 수 있다. 연산은 세미콜론으로 끝난다. `+key=value`는 추가·교체, `-key`는 모델에서 제거한다. 몹의 -는 관측 상실이다. `!m<ID>`는 이전에 보였고 직접 근접 공격으로 처치가 확인된 몹이 같은 관측 위치에서 사라졌음을 표시한다. 시야 밖의 죽음은 보고하지 않는다.

## 공개 행

| 키 | 의미 |
|---|---|
| status, turn, active, clock | 세션 상태, 입력 차례, 현재 좌석(-1이면 없음), 원본 누적 시간과 Actor.now의 합 |
| reason, receipt | 일시정지 사유, 행동자 본인의 마지막 확정 경계에 대한 actionID·verb·outcome |
| width, self, ally | 셀 인덱스 해석용 폭. self는 actor·위치·HP·HT·직업·레벨·경험치. ally는 직접 볼 수 있을 때만 actor·위치·직업 |
| t셀, h셀 | 현재 직접 시야 안의 표시 지형, 내용물을 열람할 수 없는 더미의 일반 컨테이너 표기. 비밀 문은 벽, 숨은 함정은 빈 바닥으로 보냄 |
| mActorID | 현재 직접 관측 가능한 지원 몹의 위치·종류·HP·HT. 투명 몹 제외 |
| bActorID | 자신의 표시 아이콘이 있는 버프/디버프의 아이콘과 원본 표시용 남은 시간(FlavourBuff), 기타 NA |
| i좌석별ID | 소유·장착 슬롯 또는 현재 보이는 일반 바닥 더미의 맨 위 셀, 종류(FOOD/MELEE/OTHER), 표시 이미지·수량·알고 있는 강화 수치 |

직접 시야는 원본 Level의 차폐·거리 계산을 공유하며 특수 감지와 기존 탐험 배열을 합치지 않는다. 자기 칸은 포함한다. 아이템 ID는 좌석별로 발급하며 연속해서 공개되는 같은 객체는 바닥→가방→장비로 이동해도 같은 ID다. 스택에 흡수되거나 소모되거나 시야에서 사라지면 해당 좌석의 매핑을 제거한다. 재발견 시 새 ID를 받을 수 있다. 아이템의 ID와 행동 권한은 별도로 검사한다. 다른 사람의 장비·가방 내용, 미관측 지형, 숨은 몹, 월드 RNG, 방장 자격값은 공개 모델과 해시에서 제외한다.

## 표시 모델과 오류 처리

좌석마다 하나의 SeatActions와 DisplayReplica를 유지한다. refresh로 첫 상태를 받은 뒤 UI에는 불변 StateImage를 전달한다. 현재 좌석이라면 SeatActions.prepare로 Pending을 만들고 submit한다. 영수증이 애플리케이션에서 유실되면 같은 Pending을 같은 연결에서 다시 제출한다. 화면을 새로 동기화해도 SeatActions의 번호 생성기는 유지한다.

조각 순서·토큰·개수·시간·전송 해시를 확인한 뒤 임시 맵에 연산을 적용한다. 클라이언트는 **적용한 행**을 키 순서로 정렬해 `session|seat|version|key=value;...`의 SHA-256을 직접 계산한다. 호스트의 공개 상태 해시와 일치할 때만 모델을 교체한다. 동일 변경분은 재적용하지 않는다. 버전 누락·잘못된 순서·손상에는 전체 스냅샷을 한 번 새로 요청한다. 그 시도도 실패하면 마지막 정상 모델을 유지하고 호출자가 오류를 받는다. 실제 전송 연결이 끊겨 refresh가 통신 실패를 받으면 모델을 닫는다. 화면에서 주도적으로 퇴장할 때도 close를 호출한다.

플랫폼 UI, 플레이어별 영구 탐험 기록, 특수 감지, 장기 저장용 아이템 ID와 캐시, 재접속은 후속 단계다.
