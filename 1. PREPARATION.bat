@echo off
rmdir /q /s target
mkdir target
copy variables.json target
copy js-client\* target
