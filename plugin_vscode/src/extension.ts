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
    if (orchestratorProcess) {
        orchestratorProcess.kill();
    }

    // Call the CLI command directly, assuming it is on the system's PATH.
    // This is more robust than a hardcoded relative path.
    const command = 'geministrator';
    const args = ['run', prompt];

    orchestratorProcess = spawn(command, args);

    orchestratorProcess.stdout?.on('data', (data: Buffer) => {
        const message = data.toString();
        orchestratorPanel?.webview.postMessage({ command: 'log', text: message });
    });

    orchestratorProcess.stderr?.on('data', (data: Buffer) => {
        const message = data.toString();
        orchestratorPanel?.webview.postMessage({ command: 'log', text: `ERROR: ${message}` });
    });

    orchestratorProcess.on('close', (code) => {
        orchestratorPanel?.webview.postMessage({ command: 'log', text: `\n--- Process finished with exit code ${code} ---` });
        orchestratorProcess = undefined;
    });

    orchestratorProcess.on('error', (err) => {
        orchestratorPanel?.webview.postMessage({ command: 'log', text: `\n--- FAILED TO START PROCESS ---\nIs 'geministrator' installed and in your system's PATH?\nError: ${err.message}` });
        orchestratorProcess = undefined;
    });
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
                        case 'log':
                            outputArea.textContent += message.text;
                            outputArea.scrollTop = outputArea.scrollHeight; // Auto-scroll
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