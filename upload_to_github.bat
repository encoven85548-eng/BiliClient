@echo off
chcp 65001 >nul
REM ============================================================
REM  BiliClient GitHub Upload Script (safe version)
REM  Prerequisites:
REM   1. Git installed (https://git-scm.com/download/win)
REM   2. A GitHub repository already created (Public)
REM   3. A Personal Access Token generated (with repo scope)
REM
REM  This script never writes your token to disk.
REM  When git prompts for credentials during push:
REM    - Username: your GitHub username
REM    - Password: paste your Personal Access Token
REM ============================================================

echo ============================================
echo  Preparing to upload BiliClient to GitHub
echo ============================================

REM ---- EDIT THESE 2 VALUES ----
set GIT_USERNAME=encoven85548-eng
set REPO_NAME=BiliClient
REM ------------------------------

echo.
echo [1/5] Checking Git installation...
where git >nul 2>nul
if errorlevel 1 (
    echo   [ERROR] Git not found. Please install: https://git-scm.com/download/win
    pause
    exit /b 1
)
echo   Git is ready

echo [2/5] Initializing Git repository...
if not exist .git (
    git init
)
git config user.name "%GIT_USERNAME%"
git config user.email "encoven85548@gmail.com"

echo [3/5] Staging files...
git add .

echo [4/5] Committing...
git commit -m "BiliClient source"

echo [5/5] Pushing to GitHub...
git branch -M main
git remote remove origin 2>nul
git remote add origin "https://github.com/%GIT_USERNAME%/%REPO_NAME%.git"
echo   If prompted, enter your username and paste your PAT as password:
git push -u origin main

echo.
echo ============================================
echo  Upload finished!
echo ============================================
pause
