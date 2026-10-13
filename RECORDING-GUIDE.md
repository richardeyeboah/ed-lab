# Record the assignment explanation

The screenshots ask for a program AND a video explaining it. The video is 30% of the grade. This guide matches the current website; it does not say a recording already exists.

1. Open the live website in Chrome. Use the supplied sample data. If you have saved edited Java, download it first and use Reset code to demonstrate the verified original.
2. Click Record walkthrough. Choose the browser tab/window/screen and allow your microphone. Make a short test recording and listen to it before the final take. Mac Shift–Command–5 with a microphone selected is the fallback.
3. Introduce the problem: severity decides who receives treatment first; equal-level patients keep arrival order; there are only ten rooms in this simulation.
4. Show the Comparator and Data structures editor tabs. Explain PriorityQueue, the custom comparator, the room array, and the HashMap. Severity is compared first, then arrival timestamp, then sequence. Queue insertion/removal is O(log n); peek is O(1).
5. Explain the assignment's ESI colours: 1 red, 2 orange, 3 yellow, 4 blue, 5 green. The earlier video's care-category colours are different. Severity is explicitly entered, not diagnosed from complaints.
6. Run Test 1. Show the patients entering rooms 1, 2, 3 and 10. Open Details to compare arrival order and treatment order. Point out that P001 arrived before P006 and both are level 2. Their relative order is preserved. Arrival timestamps are simulated, fixed demo times shown in UTC.
7. Run Test 2 and show all ten room statuses.
8. Run Test 3: admission to an occupied room is rejected; its occupant and waiting patients remain.
9. Run Test 4: all ten rooms fill; three patients remain waiting. Attempting another admission is rejected.
10. Run Test 5: a null patient is rejected without changing the state.
11. Run Test 6: explicitly discharge the non-critical sample patients cleared to leave. Test 6 follows the written discharge instruction despite the contradictory admission example.
12. Run Test 7: show the updated room statuses.
13. Run Test 8: the patients in rooms 1 and 3 are assumed cured/cleared by the doctor and discharged. Old severity is not an automatic discharge decision.
14. Run Test 9: room 6 is empty, so another discharge is rejected.
15. Run Test 10: show search and peek results. Expand the waiting list if needed and open Details for the final queue size, severity statistics, arrival order and full Java output. Repeated peek does not remove the next patient.
16. Stop and download the recording. Watch/listen to the saved file, checking readable code, audible explanation, and coverage of every scenario.

Submit the live website URL and the completed video as permitted by the assignment form. The GitHub repository provides the source as well. Follow your course's rules on AI assistance. No submission is performed by this guide.
