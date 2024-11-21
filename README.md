# Personal SSH
### Minimalistic alternative to SSH protocol for Windows

# Test build
### `WARNING`
Update the contents of `example-variables.json` and rename
it to `variables.json`
```sh
cargo build --manifest-path ./rust-agent/Cargo.toml
mvn -f java-server/pom.xml clean package
```

# Test build
### `WARNING`
Update the contents of `example-variables.json` and rename
it to `variables.json`
```sh
cargo build --release --manifest-path ./rust-agent/Cargo.toml
mvn -f java-server/pom.xml clean package
```

# Launch
```sh
rmdir /q /s target
mkdir target
copy variables.json target
copy rust-agent/target/debug/personal-ssh-ws-client.exe target
copy js-client/* target
copy java-server/target/*.jar target
```
