# CircleCI 배포 파이프라인 설정 가이드

## 파이프라인 흐름
```
Git Push (main)
  ↓
Gradle 빌드 (gradlew clean build)
  ↓
Docker 이미지 빌드 (AMD64)
  ↓
AWS ECR에 푸시
  ↓
Lightsail 서버 SSH 접속
  ↓
ECR에서 최신 이미지 Pull
  ↓
기존 컨테이너 중지/삭제
  ↓
새 컨테이너 실행 (포트 8080, SPRING_PROFILES_ACTIVE 적용)
  ↓
배포 완료
```

## CircleCI에 필요한 환경변수 설정

CircleCI 프로젝트 설정 > Environment Variables에서 다음을 추가하세요:

### AWS 관련
- `AWS_REGION`: AWS 리전 (예: `ap-northeast-2`)
- `AWS_ACCOUNT_ID`: AWS Account ID (12자리 숫자)
- `ECR_REPOSITORY_URL`: ECR 리포지토리 URL
  - 예: `123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/config-repo`

### Lightsail 서버 관련
- `LIGHTSAIL_SERVER_IP`: Lightsail 서버 공인 IP
- `LIGHTSAIL_SSH_USER`: SSH 접속 사용자 (일반적으로 `ubuntu` 또는 `ec2-user`)
- `LIGHTSAIL_SSH_PRIVATE_KEY`: Lightsail SSH 개인키를 Base64로 인코딩한 값
  - 생성 방법:
    ```bash
    cat ~/.ssh/lightsail_key.pem | base64 | tr -d '\n'
    ```

### 애플리케이션 관련
- `SPRING_PROFILES_ACTIVE`: Spring 프로파일 (예: `dev`, `qa`, `prod`)

## AWS IAM 권한 설정

CircleCI가 ECR에 접근할 수 있도록 AWS IAM 사용자를 생성하고 다음 권한을 부여하세요:

### 필요한 정책 (Policy)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    }
  ]
}
```

## Lightsail 서버 준비 사항

배포 대상인 Lightsail 서버에 다음이 설치되어 있어야 합니다:

1. **Docker 설치**
   ```bash
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   ```

2. **AWS CLI 설정** (Lightsail에서 ECR 로그인용)
   ```bash
   sudo apt-get install -y awscli
   aws configure
   ```

3. **SSH 공개키 추가**
   - Lightsail 서버의 `~/.ssh/authorized_keys`에 CircleCI용 공개키 추가
   - 또는 AWS 콘솔에서 "기본 키 페어" 설정

## 배포 프로세스 확인

1. main 브랜치에 push
2. CircleCI에서 자동으로 파이프라인 시작
3. CircleCI 대시보드에서 진행 상황 확인

## 트러블슈팅

### ECR 로그인 실패
- AWS_ACCOUNT_ID, AWS_REGION 확인
- IAM 권한 확인 (GetAuthorizationToken 필요)

### SSH 연결 실패
- LIGHTSAIL_SERVER_IP 확인
- LIGHTSAIL_SSH_USER 확인
- SSH 공개키가 Lightsail에 등록되어 있는지 확인

### Docker 로그인 실패 (Lightsail)
- Lightsail 서버에 AWS CLI 설치 여부 확인
- IAM 권한 설정 확인
- 서버의 IAM 역할(Role) 확인

## 수동 배포 명령어 (테스트용)

로컬에서 수동으로 테스트하려면:

```bash
# 1. 빌드
./gradlew clean build -x test

# 2. Docker 빌드
docker build -t config-repo:latest .

# 3. ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com

# 4. ECR에 푸시
docker tag config-repo:latest 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/config-repo:latest
docker push 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/config-repo:latest

# 5. Lightsail에 SSH로 접속
ssh -i ~/.ssh/lightsail_key.pem ubuntu@your-server-ip

# 6. Lightsail에서 배포
aws ecr get-login-password --region ap-northeast-2 | \
docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com

docker pull 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/config-repo:latest

docker stop config-repo || true
docker rm config-repo || true

docker run -d \
  --name config-repo \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/config-repo:latest
```

