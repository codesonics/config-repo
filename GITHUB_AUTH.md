# GitHub 푸시 인증 문제 해결 가이드

## 문제
```
remote: Permission to codesonics/config-repo.git denied to ycyoon-humaxmobility.
fatal: unable to access 'https://github.com/codesonics/config-repo.git/'
```

## 원인
- 로컬 Git 설정이 `ycyoon-humaxmobility` 계정으로 되어 있음
- 또는 macOS 키체인에 잘못된 계정이 저장되어 있음

---

## 해결 방법

### 방법 1: GitHub Personal Access Token 사용 (HTTPS)

#### 1단계: GitHub에서 Personal Access Token 생성
1. GitHub에 로그인 (codesonics 계정)
2. Settings → Developer settings → Personal access tokens → Tokens (classic) → Generate new token
3. 다음 권한 선택:
   - `repo` (전체)
   - `workflow`
4. Token 복사 (이후 다시 볼 수 없음)

#### 2단계: macOS 키체인에 저장
```bash
# 기존 인증 정보 삭제
git credential-osxkeychain erase <<< "host=github.com"

# 새로운 토큰으로 인증 (처음 push 시 자동으로 요청됨)
# 또는 수동으로:
git credential-osxkeychain approve <<< "host=github.com
protocol=https
username=codesonics
password=ghp_YOUR_PERSONAL_ACCESS_TOKEN"
```

#### 3단계: Git 푸시
```bash
cd /Users/codesonic/Documents/config-repo
git push origin main
```

---

### 방법 2: SSH 키 사용 (권장)

#### 1단계: SSH 키 생성 (이미 있으면 스킵)
```bash
ssh-keygen -t ed25519 -C "your-email@example.com"
# 또는 RSA 사용:
ssh-keygen -t rsa -b 4096 -C "your-email@example.com"

# Enter passphrase (비밀번호) 설정 (선택사항)
```

#### 2단계: 공개키를 GitHub에 등록
1. GitHub 로그인 (codesonics 계정)
2. Settings → SSH and GPG keys → New SSH key
3. 공개키 추가:
   ```bash
   cat ~/.ssh/id_ed25519.pub
   # 내용 복사해서 GitHub에 붙여넣기
   ```

#### 3단계: Remote URL을 SSH로 변경
```bash
cd /Users/codesonic/Documents/config-repo
git remote set-url origin git@github.com:codesonics/config-repo.git
```

#### 4단계: 푸시
```bash
git push origin main
```

---

### 방법 3: Git config에 인증 정보 포함 (비권장 - 보안 위험)

```bash
# HTTPS URL에 사용자명과 토큰 포함
git remote set-url origin https://codesonics:ghp_YOUR_TOKEN@github.com/codesonics/config-repo.git
git push origin main
```

**주의:** 토큰이 노출될 수 있으므로 권장하지 않습니다.

---

## 빠른 해결 스크립트

```bash
# 1. Git 계정 설정
git config --global user.name "codesonics"
git config --global user.email "your-github-email@example.com"

# 2. SSH 키 생성 (없으면)
ssh-keygen -t ed25519 -C "your-github-email@example.com"

# 3. SSH agent에 키 추가
ssh-add ~/.ssh/id_ed25519

# 4. Remote URL을 SSH로 변경
git remote set-url origin git@github.com:codesonics/config-repo.git

# 5. 푸시
git push origin main
```

---

## 검증

```bash
# SSH 키 연결 테스트
ssh -T git@github.com

# 예상 출력:
# Hi codesonics! You've successfully authenticated, but GitHub does not provide shell access.
```

## 참고
- **HTTPS**: 매번 토큰/비밀번호 입력 필요 (또는 자동 저장)
- **SSH**: 한 번 설정 후 계속 사용 가능
- **Personal Access Token**: `ghp_` 또는 `github_pat_` 로 시작

