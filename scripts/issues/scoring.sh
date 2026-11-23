#!/usr/bin/env bash
set -e

# =============================================================================
# 점수 산정 로직 이슈 생성 (docs/issues/scoring.md 기반)
#
#     bash scripts/issues/scoring.sh
#
# 본문은 scripts/issues/scoring/*.md 에 따로 있다. 이미 만든 이슈의 본문을 고치려면
# 그 파일을 수정한 뒤 덮어쓴다. 스크립트를 다시 돌릴 필요 없다.
#
#     gh issue edit <번호> --body-file scripts/issues/scoring/1-gate.md
#
# 우선순위와 규모는 조직 Issue field 다. gh 에는 플래그가 없지만 GraphQL 의
# setIssueFieldValue 뮤테이션으로 지정할 수 있어서 이 스크립트가 직접 넣는다.
# 필드와 옵션 ID 는 이름으로 조회한다. 하드코딩하면 필드를 다시 만들 때 깨진다.
#
# 의존 관계는 --blocked-by 로 건다. 앞 이슈 번호를 받아 넘기므로 실행 순서를 바꾸면 안 된다.
#
# 근거 링크는 커밋 8ae8b3a 에 고정한 permalink 다. 기준 커밋을 옮기려면
# scoring/*.md 의 8ae8b3a 를 일괄 치환하면 된다.
#
# 재실행하면 중복 이슈가 생긴다. 한 번만 실행할 것.
# =============================================================================

ORG="byeolbolil"
BODY_DIR="$(dirname "$0")/scoring"

if ! command -v gh >/dev/null 2>&1; then
	echo "gh 를 찾을 수 없습니다." >&2
	echo "설치돼 있는데도 이 메시지가 뜨면 셸의 PATH 가 낡은 것입니다." >&2
	echo "VS Code 를 완전히 재시작하거나, 터미널을 새로 열고 다시 실행하세요." >&2
	exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
	echo "gh 인증이 필요합니다: gh auth login" >&2
	exit 1
fi

gh label create "area:scoring" --color "5319E7" --description "별 관측 점수 산정 로직" 2>/dev/null || true

FIELD_QUERY="{ organization(login: \"$ORG\") { issueFields(first: 20) { nodes { ... on IssueFieldSingleSelect { id name options { id name } } } } } }"

field_id() {
	gh api graphql -f query="$FIELD_QUERY" \
		--jq ".data.organization.issueFields.nodes[] | select(.name==\"$1\") | .id"
}

option_id() {
	gh api graphql -f query="$FIELD_QUERY" \
		--jq ".data.organization.issueFields.nodes[] | select(.name==\"$1\") | .options[] | select(.name==\"$2\") | .id"
}

PRIORITY_FIELD=$(field_id "Priority")
SIZE_FIELD=$(field_id "Size")

# 이슈 번호, 우선순위 옵션명, 규모 옵션명
set_fields() {
	local issue_id priority_option size_option
	issue_id=$(gh issue view "$1" --json id --jq .id)
	priority_option=$(option_id "Priority" "$2")
	size_option=$(option_id "Size" "$3")
	gh api graphql -f query='
		mutation($i: ID!, $pf: ID!, $po: ID!, $sf: ID!, $so: ID!) {
			setIssueFieldValue(input: {
				issueId: $i,
				issueFields: [
					{ fieldId: $pf, singleSelectOptionId: $po },
					{ fieldId: $sf, singleSelectOptionId: $so }
				]
			}) { clientMutationId }
		}' \
		-f i="$issue_id" -f pf="$PRIORITY_FIELD" -f po="$priority_option" \
		-f sf="$SIZE_FIELD" -f so="$size_option" >/dev/null
	echo "#$1  $2 / $3"
}

# gh issue create 는 생성된 이슈 URL 을 출력한다. 마지막 경로 조각이 이슈 번호다.
issue_number() {
	local n="${1##*/}"
	case "$n" in
		''|*[!0-9]*) echo "이슈 번호를 읽지 못했습니다: $1" >&2; return 1 ;;
	esac
	echo "$n"
}

GATE=$(issue_number "$(gh issue create \
	--title "점수 엔진 특성화 테스트" \
	--label "area:scoring" \
	--body-file "$BODY_DIR/1-gate.md")")
set_fields "$GATE" P3 M

LIGHT_POLLUTION=$(issue_number "$(gh issue create \
	--title "광해 데이터 파이프라인 재작업" \
	--label "area:scoring" \
	--blocked-by "$GATE" \
	--body-file "$BODY_DIR/2-light-pollution.md")")
set_fields "$LIGHT_POLLUTION" P1 L

INPUT_FAILURE=$(issue_number "$(gh issue create \
	--title "입력 범위·외부 API 실패 처리 정책 통일" \
	--label "area:scoring" \
	--blocked-by "$GATE" \
	--body-file "$BODY_DIR/3-input-failure.md")")
set_fields "$INPUT_FAILURE" P1 L

DISPLAY=$(issue_number "$(gh issue create \
	--title "사용자에게 보이는 값 정합" \
	--label "area:scoring" \
	--blocked-by "$GATE" \
	--body-file "$BODY_DIR/4-display.md")")
set_fields "$DISPLAY" P1 M

CONSTANTS=$(issue_number "$(gh issue create \
	--title "점수 상수 근거 확정 및 docs/SCORING.md 작성" \
	--label "area:scoring" \
	--blocked-by "$GATE,$LIGHT_POLLUTION" \
	--body-file "$BODY_DIR/5-constants.md")")
set_fields "$CONSTANTS" P2 L

echo
echo "=== 완료: 5건 ==="
