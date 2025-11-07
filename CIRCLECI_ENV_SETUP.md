# CircleCI 환경변수 설정 가이드

## 필수 환경변수 목록

CircleCI 프로젝트의 **Project Settings** → **Environment Variables**에서 다음 변수들을 설정하세요.

### 1️⃣ AWS 관련 변수

#### AWS_REGION
- **값**: `eu-west-2`
- **설명**: AWS 리전

#### AWS_ACCESS_KEY_ID
- **값**: AWS IAM 사용자의 Access Key ID
- **설명**: AWS 인증용

#### AWS_SECRET_ACCESS_KEY
- **값**: AWS IAM 사용자의 Secret Access Key
- **설명**: AWS 인증용

### 2️⃣ Lightsail 서버 관련 변수

#### LIGHTSAIL_HOST
- **값**: Lightsail 서버의 공인 IP 또는 도메인
- **예**: `12.34.56.78` 또는 `my-server.com`

#### LIGHTSAIL_USER
- **값**: Lightsail 서버의 SSH 접속 사용자명
- **예**: `ubuntu`, `ec2-user`, `admin`

#### LIGHTSAIL_SSH_PRIVATE_KEY
- **값**: SSH 개인키 내용 (전체 텍스트)
- **설정 방법**:
  ```bash
  # 1. SSH 개인키 파일 확인
  cat ~/.ssh/your_private_key
  
  # 2. 전체 내용을 복사
  # -----BEGIN RSA PRIVATE KEY-----
  # ... 키 내용 ...
  # -----END RSA PRIVATE KEY-----
  
  # 3. CircleCI에 그대로 붙여넣기
  ```

---

## CircleCI Context 설정 (권장)

프로젝트마다 변수를 반복 설정하지 않으려면 **Context**를 사용하세요.

### Context 생성 순서

1. CircleCI 대시보드 → **Organization Settings** → **Contexts**
2. **Create Context** 클릭
3. 다음 2개 Context 생성:

#### Context 1: `aws-dev`
- AWS_REGION: `eu-west-2`
- AWS_ACCESS_KEY_ID: `AKIAIOSFODNN7EXAMPLE`
- AWS_SECRET_ACCESS_KEY: `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`

#### Context 2: `lightsail-dev`
- LIGHTSAIL_HOST: `12.34.56.78`
- LIGHTSAIL_USER: `ubuntu`
- LIGHTSAIL_SSH_PRIVATE_KEY: `-----BEGIN RSA PRIVATE KEY-----\n...`

---

## 검증

### 1. AWS 권한 확인
```bash
aws iam get-user --access-key-id AKIAIOSFODNN7EXAMPLE
```

### 2. SSH 연결 테스트 (로컬)
```bash
ssh -i ~/.ssh/your_private_key ubuntu@12.34.56.78
```

### 3. ECR 접근 확인 (로컬)
```bash
aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin 790187585820.dkr.ecr.eu-west-2.amazonaws.com
```

---

## 배포 파이프라인 흐름

```
main 브랜치에 Push
    ↓
[build] Gradle 빌드 (build context 사용 안함)
    ↓
[docker-build-and-push] Docker 빌드 & ECR 푸시 (aws-dev context)
    ↓
[deploy-to-lightsail] Lightsail 배포 (aws-dev + lightsail-dev context)
    ↓
✅ 배포 완료
```

---

## 문제 해결

### 1. "Permission denied (publickey)"
**원인**: SSH 개인키가 잘못되었거나 Lightsail 서버에 등록되지 않음

**해결**:
```bash
# Lightsail 서버에서 공개키 등록 확인
cat ~/.ssh/authorized_keys | grep "your-public-key"

# 없으면 추가
echo "your-public-key-content" >> ~/.ssh/authorized_keys
```

### 2. "aws: command not found" (Lightsail)
**원인**: Lightsail 서버에 AWS CLI가 설치되지 않음

**해결**:
```bash
# Lightsail 서버에 SSH 접속 후
sudo apt-get update
sudo apt-get install -y awscli
aws configure
```

### 3. "Docker daemon is not running"
**원인**: Lightsail 서버의 Docker가 실행되지 않음

**해결**:
```bash
sudo systemctl start docker
sudo systemctl enable docker
```

### 4. ECR 로그인 실패
**원인**: IAM 권한 또는 AWS credentials 설정 오류

**Lightsail 서버에 필요한 권한**:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchGetImage",
        "ecr:GetDownloadUrlForLayer"
      ],
      "Resource": "*"
    }
  ]
}
```

---

## SSH 개인키 형식 (주의)

CircleCI에 등록할 때 **여러 줄**의 개인키를 올바르게 처리하려면:

### 방법 1: 그대로 복사 (권장)
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA2Z3qX2BTLS39R...
(중간 생략)
...RV4lFp=
-----END RSA PRIVATE KEY-----
```

### 방법 2: 한 줄로 변환
```bash
cat ~/.ssh/your_private_key | tr '\n' '\\n' | pbcopy
```

---

## 추가 확인사항

- [ ] AWS IAM 사용자 생성됨
- [ ] AWS Access Key & Secret Key 생성됨
- [ ] Lightsail 서버의 SSH 포트(22)가 열려 있음
- [ ] Lightsail 서버에 Docker 설치됨
- [ ] Lightsail 서버에 AWS CLI 설치되고 configured 됨
- [ ] CircleCI Project Settings에서 Context 적용됨
- [ ] SSH 개인키가 Lightsail 서버의 `~/.ssh/authorized_keys`에 등록됨

