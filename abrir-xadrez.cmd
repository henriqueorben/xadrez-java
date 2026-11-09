@echo off
setlocal
set "CHESS_ROOT=%~dp0"
set "CHESS_JDK_BIN="

if defined JAVA_HOME call :useJdk "%JAVA_HOME%\bin"
if not defined CHESS_JDK_BIN for /f "delims=" %%J in ('where javac.exe 2^>nul') do call :useJdk "%%~dpJ."
if not defined CHESS_JDK_BIN for /d %%D in ("%USERPROFILE%\.jdks\*") do call :useJdk "%%~fD\bin"

if not defined CHESS_JDK_BIN (
    echo Instale um JDK 21 ou configure JAVA_HOME e tente novamente.
    pause
    exit /b 1
)

if not exist "%CHESS_ROOT%bin\" mkdir "%CHESS_ROOT%bin"
"%CHESS_JDK_BIN%\javac.exe" --release 21 -encoding UTF-8 -d "%CHESS_ROOT%bin" -sourcepath "%CHESS_ROOT%src" "%CHESS_ROOT%src\application\ChessWindow.java" "%CHESS_ROOT%src\application\Program.java"
if errorlevel 1 (
    echo Falha ao compilar o xadrez. Confira se o JDK 21 esta instalado.
    pause
    exit /b 1
)

start "Xadrez" "%CHESS_JDK_BIN%\javaw.exe" -cp "%CHESS_ROOT%bin" application.ChessWindow
exit /b %errorlevel%

:useJdk
if defined CHESS_JDK_BIN exit /b 0
if not exist "%~1\javac.exe" exit /b 0
if not exist "%~1\javaw.exe" exit /b 0
"%~1\javac.exe" --release 21 -version >nul 2>&1
if not errorlevel 1 set "CHESS_JDK_BIN=%~1"
exit /b 0
