@echo off
:: Minimal gradlew.bat shim: use gradle if available, otherwise exit with a friendly message.
@where gradle >nul 2>&1
@if %ERRORLEVEL%==0 (
	gradle %*
	goto :eof
)

echo Gradle not found in PATH on Windows runner.
echo On Windows runners, automatic Gradle installation isn't handled by this script.
echo If you need a Windows build, install Gradle or run the build on a Linux runner.
exit /b 1
