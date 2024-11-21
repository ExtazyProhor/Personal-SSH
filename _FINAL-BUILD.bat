@echo off
cargo build --release --manifest-path ./rust-agent/Cargo.toml
mvn -f java-server/pom.xml clean package

copy rust-agent\target\release\personal-ssh-ws-client.exe target\
copy java-server\target\*jar-with-dependencies.jar target\
