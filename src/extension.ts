import * as vscode from 'vscode';
import { parseMarkdown, writeCellsToMarkdown, RawNotebookCell } from './markdownParser';
import * as child_process from 'child_process'
import * as path from 'path';
import * as http from 'http';
import * as fs from 'fs';
import * as os from 'os';

/**
 * 	// "extensionDependencies": [
	// 	"redhat.java"
	//   ],


	depend on   "name": "vscode-data-table", "displayName": "Data Table Renderers" https://github.com/RandomFractals/vscode-data-table/blob/main/package.json
	https://github.com/Microsoft/vscode-notebook-renderers
 */

const providerOptions = {
	transientMetadata: {
		runnable: true,
		editable: true,
		custom: true,
	},
	transientOutputs: true
};

export function activate(context: vscode.ExtensionContext) {
	
	const classPath = listFilesAndJoinPaths(context.extensionPath + '/java/libs/');
	const noteBookJHellPorts: Map<string, JShellInfo> = new Map<string, JShellInfo>();
	
	let controller = new JShellNotebookController(noteBookJHellPorts, classPath);
	context.subscriptions.push(vscode.workspace.registerNotebookSerializer('jshell-notebook', new JShellNitebookSerializer(), providerOptions));
	context.subscriptions.push(controller);

	vscode.workspace.onDidOpenNotebookDocument(async (notebook) => {
        const notebookUri = notebook.uri.toString();
        // console.log("open notebook === " + notebookUri);
    });

	vscode.workspace.onDidCloseNotebookDocument(async (notebook) => {
        const notebookUri = notebook.uri.toString();
        // console.log("closed notebook === " + notebookUri);
    });



	const provider = vscode.languages.registerCompletionItemProvider(
        { language: 'java', scheme: 'vscode-notebook-cell' },
        {
            async provideCompletionItems(document: vscode.TextDocument, position: vscode.Position) {
                const lineToComplete = document.lineAt(position).text.substr(0, position.character);
				let offsetNumber = document.offsetAt(position);
				let documentPartBeforeCursor = document.getText().substring(0, offsetNumber);
				let notebookUri = document.fileName;

				const payload = {
					script: documentPartBeforeCursor
				};
		
				// const postData = JSON.stringify(payload);
				const postData = documentPartBeforeCursor;
				// console.log("fetching code completions for =========== " + postData);
		
				try {
					const completions = await fetchCompletions(postData, notebookUri, noteBookJHellPorts, classPath);
					// console.log("Received completions:", completions);
					return completions.map(item => new vscode.CompletionItem(item, vscode.CompletionItemKind.Method));
				} catch (error) {
					console.error("Error fetching completions:", error);
					return undefined;
				}
            }
        },
        '.' // Trigger character
    );

    context.subscriptions.push(provider)
}


function fetchCompletions(postData: string, notebookUri: string, noteBookJHellPorts: Map<string, JShellInfo>, classPath : string): Promise<string[]> {
	let jshellInfo = getJShellInfoForNotebook(notebookUri, noteBookJHellPorts, classPath);
    return new Promise((resolve, reject) => {
        const options: http.RequestOptions = {
            hostname: 'localhost',
            port: jshellInfo.port,
            path: '/suggest',
            method: 'POST',
            headers: {
                'Content-Type': 'application/text',
                'Content-Length': Buffer.byteLength(postData),
				'Authorization': jshellInfo.secret
            },
        };

        const req = http.request(options, (res) => {
            let data = '';

            res.on('data', (chunk) => {
                data += chunk;
            });

            res.on('end', () => {
                try {
                    const parsedData = JSON.parse(data);
                    resolve(parsedData.completions);
                } catch (error) {
                    reject(new Error('Failed to parse response data'));
                }
            });
        });

        req.on('error', (error) => {
            reject(error);
        });

        req.write(postData);
        req.end();
    });
}

function getRandomInt(min: number, max: number) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function getJShellInfoForNotebook(notebookUri: string, noteBookJHellPorts: Map<string, JShellInfo>, jshellExecutableClassPath: string) : JShellInfo {
	let jshellInfo = noteBookJHellPorts.get(notebookUri);
	if (jshellInfo) {
		return jshellInfo;
	} else {
		initializeJShell(notebookUri, noteBookJHellPorts, jshellExecutableClassPath);
	}
	jshellInfo = noteBookJHellPorts.get(notebookUri);
	if (jshellInfo) {
		return jshellInfo;
	}
	return {pid: -1, port: -1, secret: ""};
}

function isProcessRunning(pid: number): boolean {
	try {
		process.kill(pid, 0); // Sending signal 0 does not kill the process
		return true; // Process is still running
	} catch (error) {
		return false; // Process does not exist
	}
}

async function initializeJShell(notebookUri: string, noteBookToPortMap: Map<string, JShellInfo>, jshellExecutableClassPath: string) {
	// console.log("init jshell for uri 1)" + notebookUri + " ======================== in map " + noteBookToPortMap);
	if (noteBookToPortMap.has(notebookUri)) {
		const jshellInfo = noteBookToPortMap.get(notebookUri);
		if (jshellInfo && !isProcessRunning(jshellInfo.pid)) {
			noteBookToPortMap.delete(notebookUri);
		}
	}
	
	if (!noteBookToPortMap.has(notebookUri)) {
		const randomPort = getRandomInt(8090, 9090);
		const secretPhrase = generateRandomPhrase(128);
		// console.log("init jshell for uri 2)" + notebookUri + " | " + randomPort + " ========================" + secretPhrase);
		const command = "java"; 
		const args = [ "-cp", jshellExecutableClassPath, "s4gh.HttpJsonServerWithSuggestion", randomPort.toString(), secretPhrase];
		// const args = [ "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","-cp", jshellExecutableClassPath, "s4gh.HttpJsonServerWithSuggestion", randomPort.toString(), secretPhrase];
		// console.log("Executing: " + command + args);
		const process = child_process.spawn(command, args, { detached: true });
		const pid = process.pid;
		noteBookToPortMap.set(notebookUri, {port: randomPort, pid: pid, secret: secretPhrase})
		// console.log("Runniing pid " + pid + " on port " + randomPort);

		blockEventLoop(2000); //wait for jshell to start
	}
}

// there are globals in workers and nodejs
declare class TextDecoder {
	decode(data: Uint8Array): string;
}
declare class TextEncoder {
	encode(data: string): Uint8Array;
}

class JShellNotebookController {
	readonly controllerId = 'jshell-notebook-controller-id';
	readonly notebookType = 'jshell-notebook';
	readonly label = 'JShell Notebook';
	readonly supportedLanguages = ['java'];

	private readonly controller: vscode.NotebookController;
	private executionOrder = 0;
	private noteBookToPortMap: Map<string, JShellInfo>;
	private readonly jshellExecutableClassPath: string;

	constructor(noteBookJHellPorts: Map<string, JShellInfo>, jshellClassPath: string) {
		// console.log("=====controller created===" + this);
		this.controller = vscode.notebooks.createNotebookController(
			this.controllerId,
			this.notebookType,
			this.label
		);

		this.controller.supportedLanguages = this.supportedLanguages;
		this.controller.supportsExecutionOrder = true;
		this.controller.executeHandler = this._execute.bind(this);

		this.noteBookToPortMap = noteBookJHellPorts;
		this.jshellExecutableClassPath = jshellClassPath;
	}

	private _execute(
		cells: vscode.NotebookCell[],
		notebook: vscode.NotebookDocument,
		controller: vscode.NotebookController
	): void {
		// console.log("Executing notebook  ============ " + notebook.uri);
		initializeJShell(notebook.uri.fsPath, this.noteBookToPortMap, this.jshellExecutableClassPath);
		for (let cell of cells) {
			this._doExecution(notebook.uri.fsPath, cell);
		}
	}

	private async _doExecution(notebookUri: string, cell: vscode.NotebookCell): Promise<void> {
		const execution = this.controller.createNotebookCellExecution(cell);
		execution.executionOrder = ++this.executionOrder;
		execution.start(Date.now()); // Keep track of elapsed time to execute cell.
		let jshellInfo = getJShellInfoForNotebook(notebookUri, this.noteBookToPortMap, this.jshellExecutableClassPath);

		const payload = {
			script: cell.document.getText()
		};

		// const postData = JSON.stringify(payload);
		const postData = cell.document.getText();
		// console.log("executing script =========== " + postData);

		const options: http.RequestOptions = {
			hostname: 'localhost',
			port: jshellInfo.port,
			path: '/execute',
			method: 'POST',
			headers: {
				'Content-Type': 'application/text',
				'Content-Length': Buffer.byteLength(postData),
				'Authorization': jshellInfo.secret
			},
		};

		const req = http.request(options, (res) => {
            let data = '';

            // Collect response data
            res.on('data', (chunk) => {
                data += chunk;
            });

            // Process the response once it's complete
            res.on('end', () => {
                // Display the response in the notebook cell output
				if (isValidJson(data)) {
					execution.replaceOutput([
						new vscode.NotebookCellOutput([
							vscode.NotebookCellOutputItem.json(JSON.parse(data), 'application/json')
						])
					]);

				} else {
					let output = data;
					if (data !== null && data !== undefined && data.length > 0) {
						output = JSON.parse(data);
					}
					execution.replaceOutput([
						new vscode.NotebookCellOutput([
							vscode.NotebookCellOutputItem.stdout(output)
						])
					]);
				}

                // End the execution successfully
                execution.end(true, Date.now());
            });
        });

        // Handle request errors
        req.on('error', (e) => {
            // Display the error in the notebook cell output
            execution.replaceOutput([
                new vscode.NotebookCellOutput([
                    vscode.NotebookCellOutputItem.text(`Error: ${e.message}`)
                ])
            ]);

            // End the execution with failure
            execution.end(false, Date.now());
        });

        // Write the POST data and end the request
        req.write(postData);
        req.end();
		//------------ execution ------------------
	}

	dispose() {
		for (const entry of this.noteBookToPortMap.entries()) {
			const notebook = entry[0];
			const jshellInfo = entry[1];
			process.kill(jshellInfo.pid, 'SIGTERM');
		}
	}
}

function blockEventLoop(ms: number) {
    const start = Date.now();
    while (Date.now() - start < ms) {
        // Busy-wait loop
    }
}


class JShellNitebookSerializer implements vscode.NotebookSerializer {

	private readonly decoder = new TextDecoder();
	private readonly encoder = new TextEncoder();

	deserializeNotebook(data: Uint8Array, _token: vscode.CancellationToken): vscode.NotebookData | Thenable<vscode.NotebookData> {
		const content = this.decoder.decode(data);

		const cellRawData = parseMarkdown(content);
		const cells = cellRawData.map(rawToNotebookCellData);

		return {
			cells
		};
	}

	serializeNotebook(data: vscode.NotebookData, _token: vscode.CancellationToken): Uint8Array | Thenable<Uint8Array> {
		const stringOutput = writeCellsToMarkdown(data.cells);
		return this.encoder.encode(stringOutput);
	}
}

type JShellInfo = {
    port: number;
    pid: number;
	secret: string;
};

function isValidJson(str: string): boolean {
	if (typeof str !== "string") return false;

	try {
	  const parsed = JSON.parse(str);
  
	  // Check if the parsed result is an object or an array
	  return (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) || Array.isArray(parsed);
	} catch (e) {
	  return false;
	}
}

function listFilesAndJoinPaths(folderPath: string): string {
    try {
		const isWindows = os.platform() === 'win32';
		let pathSeparator = ':';
		if (isWindows) {
			pathSeparator = ';';
		}
        const files = fs.readdirSync(folderPath);
        const fullPaths = files.map(file => path.join(folderPath, file));
        let classPath = fullPaths.join(pathSeparator);
		return classPath;
    } catch (error) {
        console.error(`Error reading directory: ${error}`);
        return '';
    }
}
  

function generateRandomPhrase(length: number): string {
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    const charactersLength = characters.length;

    for (let i = 0; i < length; i++) {
        const randomIndex = Math.floor(Math.random() * charactersLength);
        result += characters.charAt(randomIndex);
    }

    return result;
}

export function rawToNotebookCellData(data: RawNotebookCell): vscode.NotebookCellData {
	return <vscode.NotebookCellData>{
		kind: data.kind,
		languageId: data.language,
		metadata: { leadingWhitespace: data.leadingWhitespace, trailingWhitespace: data.trailingWhitespace, indentation: data.indentation },
		outputs: [],
		value: data.content
	};
}