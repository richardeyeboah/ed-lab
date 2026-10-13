# Assignment coverage

- PriorityQueue + PatientComparator: actual Java heap; severity, timestamp, sequence.
- Five triage levels: ESI 1–5; red, orange, yellow, blue, green, with numbers.
- FIFO ties: earlier timestamp, then arrival sequence. Equal-level patients retain order.
- Array: ten room slots, 1–10. Ten is a design choice supporting the example using room 10.
- HashMap: patient ID lookup; records remain searchable after discharge.
- admitPatient / getNextPatient / peekNextPatient: implemented and exercised.
- Monitoring: queue size, sorted waiting patients, counts by level, occupancy, discharge count.
- Arrival tracking: fixed demo timestamps, sequence, arrival-order display.
- Test 1 admissions, including room 10 and separate get-next/admit flow.
- Test 2 room status display.
- Test 3 reject occupied-room admission, preserving state.
- Test 4 full capacity, waiting patients retained.
- Test 5 null patient rejected.
- Test 6 DISCHARGE the sample non-critical patients after explicit clearance.
- Test 7 updated room statuses.
- Test 8 discharge cured/cleared occupants of rooms 1 and 3.
- Test 9 reject empty room 6 discharge.
- Test 10 final queue/rooms, successful and unsuccessful lookup, repeated peek.

The video's care-group colors are different from its numbered ESI diagram. No complaint-to-severity diagnosis is implemented. Discharge is explicit, never based on capacity. The assignment lists stacks/trees but does not clearly require every structure; no unnecessary structures were added. A PriorityQueue is heap-based.

The explanatory video is worth 30% of the grade. The recording controls let you demonstrate the actual tests while explaining them in your voice. A completed video has not been generated for this new website.
