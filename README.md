# 📊 UniTime – 대학생을 위한 AI 자동 시간표 생성 서비스

>UniTime은 AI가 직접 시간표를 조합해주는 스마트 시간표 생성 서비스입니다.
사용자는 단순히 “3일 등교로 만들어줘” 또는 “월요일은 비워줘” 같은 조건만 말하면 됩니다.
AI가 강의 목록을 분석하고, 충돌을 자동으로 제거하며, 최적의 시간표를 즉시 구성해줍니다.

**직관적인 UI와 강력한 AI 백엔드 모델을 기반으로, 번거로운 시간표 작성 과정을 효율적이고 즐겁게 바꾸는 것을 목표로 합니다.**

<br>

## 👨‍💻 Developers

| <img src="https://avatars.githubusercontent.com/u/197270758?v=4" width=100> 
| :------------------------------------------------------------------------: | 
|                   [김민호](https://github.com/minho0805)                   |         


<br>

---

# 🛠 사용 기술 (Tech Stack)

## **Backend**
- Spring Boot 3
- Java 17
- Spring Security + JWT
- JPA / Hibernate
- PostgreSQL
- OpenAI API (Chat Completions / Vision)

## **DevOps / Tooling**
- Render Web Service
- Docker (개발 환경용 일부)
- IntelliJ 

<br>

---

# 🖼 주요 화면

## 🏠 메인 페이지
사용자가 가장 먼저 만나게 되는 홈 화면으로, AI 시간표 생성 기능과 마이페이지로 쉽게 이동할 수 있습니다.

<img width="1462" height="734" alt="main" src="https://github.com/user-attachments/assets/d1e97751-3a3b-4b8d-a321-fd919903fbe6" />

<br>

## 🤖 AI 시간표 생성 페이지
AI에게 원하는 조건을 말하면 자동으로 시간표를 만들어주는 핵심 기능 페이지입니다.

<img width="1907" height="910" alt="ai-timetable" src="https://github.com/user-attachments/assets/3c5a182d-f4c2-4978-8cd3-6d50e89cc708" />

<br>

## 🙋‍♂️ 프로필 / 시간표 목록 페이지
저장된 시간표들을 한 번에 확인하고, 프로필 관리도 할 수 있는 공간입니다.

<img width="1918" height="990" alt="profile-list" src="https://github.com/user-attachments/assets/0e697bc6-53f9-44bc-96d0-94c0104b2698" />


<br>

---


# 🔑 ERD

<img width="1735" height="819" alt="ERD" src="https://github.com/user-attachments/assets/c29ae2c4-617f-4f27-8317-0afef2f7c2d6" />

<br>

---

# ☁️ System Architecture

React 기반의 프론트엔드, Spring Boot 백엔드, Render 및 Netlify 배포 구조가 포함된 전체 시스템 구성도입니다.

<img width="1170" height="697" alt="system-architecture" src="https://github.com/user-attachments/assets/3e661682-51b9-42ea-bf1c-521c03cffafa" />
