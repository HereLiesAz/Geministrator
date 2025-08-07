import * as vscode from 'vscode';
import { spawn, ChildProcess } from 'child_process';

let orchestratorPanel: vscode.WebviewPanel | undefined;
let orchestratorProcess: ChildProcess | undefined;

export function activate(context: vscode.ExtensionContext) {
    context.subscriptions.push(
        vscode.commands.registerCommand('geministrator.start', () => {
            if (orchestratorPanel) {
                orchestratorPanel.reveal(vscode.ViewColumn.Two);
                return;
            }

            orchestratorPanel = vscode.window.createWebviewPanel(
                'geministrator',
                'Geministrator',
                vscode.ViewColumn.Two,
                {
                    enableScripts: true,
                    localResourceRoots: [vscode.Uri.joinPath(context.extensionUri, 'media')]
                }
            );

            orchestratorPanel.webview.html = getWebviewContent();

            orchestratorPanel.onDidDispose(() => {
                orchestratorPanel = undefined;
                if (orchestratorProcess) {
                    orchestratorProcess.kill();
                    orchestratorProcess = undefined;
                }
            }, null, context.subscriptions);

            orchestratorPanel.webview.onDidReceiveMessage(
                message => {
                    switch (message.command) {
                        case 'runWorkflow':
                            runCliProcess(message.text);
                            return;
                        case 'sendToCli':
                            if (orchestratorProcess && orchestratorProcess.stdin) {
                                orchestratorProcess.stdin.write(`${message.text}\n`);
                            }
                            return;
                    }
                },
                undefined,
                context.subscriptions
            );
        })
    );
}

function runCliProcess(prompt: string) {
    // For now, we assume the server is running. In a real implementation,
    // we would start the server here if it's not already running.
    orchestratorPanel?.webview.postMessage({ command: 'connect', prompt: prompt });
}

function getWebviewContent(): string {
    return `
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Geministrator</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
                #output { white-space: pre-wrap; background-color: #222; color: #eee; padding: 10px; font-family: monospace; height: 70vh; overflow-y: scroll; border-radius: 5px; }
                textarea { width: 95%; margin-bottom: 10px; }
                button { padding: 5px 10px; }
            </style>
        </head>
        <body>
            <h1>Geministrator</h1>
            <textarea id="prompt" rows="4" placeholder="Enter your high-level task..."></textarea>
            <br>
            <button id="run-button">Run Workflow</button>

            <h3>Output Log</h3>
            <div id="output">Welcome! Enter a prompt and click "Run Workflow" to begin.</div>

            <script>
                const vscode = acquireVsCodeApi();
                const runButton = document.getElementById('run-button');
                const promptArea = document.getElementById('prompt');
                const outputArea = document.getElementById('output');
                let socket;

                function connect(prompt) {
                    socket = new WebSocket('ws://localhost:8080/ws');

                    socket.onopen = function() {
                        outputArea.textContent = 'Connection established. Starting workflow...\\n';
                        socket.send(JSON.stringify({ command: 'run', prompt: prompt }));
                    };

                    socket.onmessage = function(event) {
                        outputArea.textContent += event.data + '\\n';
                        outputArea.scrollTop = outputArea.scrollHeight; // Auto-scroll
                    };

                    socket.onclose = function() {
                        outputArea.textContent += '\\n--- Connection closed ---';
                    };

                    socket.onerror = function(error) {
                        outputArea.textContent += '\\n--- WebSocket Error: ' + error.message + ' ---';
                    };
                }

                runButton.addEventListener('click', () => {
                    const text = promptArea.value;
                    if (text) {
                        outputArea.textContent = ''; // Clear previous output
                        vscode.postMessage({ command: 'runWorkflow', text: text });
                    }
                });

                window.addEventListener('message', event => {
                    const message = event.data;
                    switch (message.command) {
                        case 'connect':
                            connect(message.prompt);
                            break;
                    }
                });
            </script>
        </body>
        </html>
    `;
}

export function deactivate() {
    if (orchestratorProcess) {
        orchestratorProcess.kill();
    }
}