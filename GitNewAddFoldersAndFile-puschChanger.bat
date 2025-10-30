@echo off
REM Megmutatja a git státuszt
git status

REM Hozzáad minden változást
git add .

REM Commitol egy fix üzenettel
git commit -m "Add new files and folders"

REM Feltolja a változásokat a main ágra
git push origin main
