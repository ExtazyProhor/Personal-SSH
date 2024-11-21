# Personal SSH
### Minimalistic alternative to SSH protocol for Windows

# Preparation
### `WARNING`
Update the contents of `example-variables.json` and rename
it to `variables.json`. Also change the WebSocket server address
to a valid one in the `script.js`.
```sh
rmdir /q /s target
mkdir target
copy variables.json target
copy js-client\* target
```

# Test build
```sh
cargo build --manifest-path ./rust-agent/Cargo.toml
mvn -f java-server/pom.xml clean package

copy rust-agent\target\debug\personal-ssh-ws-client.exe target
copy java-server\target\*jar-with-dependencies.jar target
```

# Final build
```sh
cargo build --release --manifest-path ./rust-agent/Cargo.toml
mvn -f java-server/pom.xml clean package

copy rust-agent\target\release\personal-ssh-ws-client.exe target
copy java-server\target\*jar-with-dependencies.jar target
```

# Launch
```sh
java -jar target\personal-ssh-ws-server-1.0-SNAPSHOT-jar-with-dependencies.jar

target\personal-ssh-ws-client.exe

target\index.html
```
