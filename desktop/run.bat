@echo off
cd /d "%~dp0"

if not exist "venv" (
    echo Setting up virtual environment...
    python -m venv venv
)

call venv\Scripts\activate.bat

echo Checking dependencies...
pip install -q -r requirements.txt

python main.py
pause
