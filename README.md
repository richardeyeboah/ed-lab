# ED Lab — Java assignment walkthrough

Everything for the website is in this folder. Double-click Start-Website.command, then use http://127.0.0.1:8765 in Chrome. Keep its Terminal window open; Ctrl+C stops the Java server. On this Mac a preserved Java runtime is included in .runtime. That runtime is not included in the source ZIP; another computer needs JDK 17 or newer.

## Your walkthrough
1. The sample CSV loads twelve patients, with fixed example arrival times shown in UTC.
2. Select Test 1, read the explanation and commented Java, then press Run Test 1.
3. Compare Before and After. Changed room cards are outlined.
4. Select each subsequent test. Every test restores its prerequisite state automatically.
5. Use Comparator and Data structures tabs to explain PriorityQueue, FIFO, the array and HashMap.
6. Open Details to compare arrival order against the main waiting list’s treatment order. Arrival records include treated patients. The waiting list initially shows the next four patients; Show all expands it.
7. To record, click Record walkthrough, choose the tab/window/screen, and allow your microphone. Stop & save finishes recording; click Download recording to save the file before leaving the page.

Recording uses browser MediaRecorder and screen/microphone permissions. It saves locally and never uploads. Actual microphone capture has not been tested on this Mac during development. Chrome is recommended for screen recording. If unavailable, use Shift–Command–5, select your microphone under Options, and record the browser window.

## Files
- src/EDProgram.java: fully commented Java assignment logic and all ten tests.
- src/WebsiteServer.java: a small local Java HTTP bridge. The browser does not imitate the Java rules.
- web/: presentation and recording controls.
- data/patients.csv: the sample import data.
- ASSIGNMENT.md: requirement mapping and assumptions.

Download Java file in the website saves EDProgram.java. This source can run without the website; its main method reads data/patients.csv, so keep that file alongside it in the data folder.

## Run on another computer
With JDK 17+ available:

    mkdir -p out
    javac --release 17 -d out src/*.java
    java -cp out WebsiteServer

Then visit http://127.0.0.1:8765. The server is bound to this computer only. Vercel deployment uses Dockerfile.vercel to compile and run the actual Java backend in a container. Each test request includes its visitor’s dataset; uploaded CSVs are never saved globally or shared between visitors. No database is required.

Console verification:

    java -cp out EDProgram

## CSV import
Columns: id,name,complaint,level. Level must be 1–5. IDs must be unique; DIRECT_LISA is reserved for the room-10 demonstration. Quoted fields and escaped quotes are supported. Each patient must fit on one CSV line. Up to 100 patients may be imported. The supplied twelve-patient dataset is designed for all tests. Custom data can fail prerequisite tests (too few patients or different severity in rooms 6 and 8); the page explains this and offers Sample data to reset.

## Verified
Java 17-target compilation; all ten console scenarios; later-urgent-before-earlier-low-priority; equal-level FIFO; all ten test buttons in the browser; source tabs; Before/After view; recording options dialog; CSV API rejects invalid levels/duplicate IDs and accepts quoted commas. Recording permission and actual audio/video capture need to be exercised by you.

Follow the course rules on AI assistance. Add your own name/group details only if required. No submission or publication has occurred.

## GitHub and deployment

Repository: https://github.com/richardeyeboah/ed-lab (private). Vercel detects Dockerfile.vercel. Build-time verification runs the Java console scenarios before the container is published. The .runtime and out directories are excluded from Git and deployment.

## Editable Java workspace

The source panel uses Microsoft's Monaco Editor (MIT, bundled version 0.57.0), with a VS Code-style theme, Java highlighting, line numbers, folding, find, undo/redo and a command palette. The tabs navigate within the full editable file; switching tests does not replace your changes. Expand editor gives more space. Cmd+S or Save draft stores the current text in this browser. Download saves the edited file. Reset restores the original, with editor undo available.

Compile & run test applies the current Java source. Errors appear below the editor with line markers. Keep the EDProgram class name and assignment method signatures so the display bridge can read the results. Selecting another test compiles edited code to restore its prerequisite state. The original source and repository are never overwritten by a visitor's draft.

Edited code is compiled with annotation processing disabled, then executed in a separate, memory/time/output-limited Java 21 process. An execution policy permits reading only the request's temporary files, and denies network access, process execution, writes, and environment access. An early System.exit ends only its child process and is reported as a missing-result error. Each request's temporary folder is removed afterwards. The deployed container includes the JDK for compilation. Verified: all ten edited-code scenarios; a changed name reflected in the UI; syntax-error diagnostics; draft reload/reset; denied file/network/process/write/environment attempts; early-exit handling; timeout of an infinite loop.

## Simplified test display

Each test shows a short purpose, one result message, a numbered waiting list, and ten room boxes. Severity badges use stronger red, orange, yellow, blue and green with ESI numbers. The next waiting patient is marked NEXT; arrival times remain visible for FIFO explanation. Occupied rooms show names and severity; empty rooms say Empty. Only changed rooms are outlined.

Statistics, counts by severity, arrival-order records and full Java output are under Details. Test 10 additionally reveals the actual Java search and peek results. Before/After changes the displayed snapshot and clearly labels the starting state. All ten scenarios passed through the revised browser interface. Java logic and the editable workspace are preserved.
