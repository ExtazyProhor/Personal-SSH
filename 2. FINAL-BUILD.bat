@echo on
cargo build --release --manifest-path ./rust-agent/Cargo.toml
mvn -f java-server/pom.xml clean package
pause