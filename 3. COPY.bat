@echo on
copy /Y rust-agent\target\release\personal-ssh-ws-client.exe target\
copy /Y java-server\target\*jar-with-dependencies.jar target\
pause
