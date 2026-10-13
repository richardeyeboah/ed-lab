import java.time.*;
import java.util.*;
import java.nio.file.*;

/**
 * THE ASSIGNMENT PROGRAM. All patient, queue, room and test decisions run in Java.
 * The website only displays snapshots returned by this class. This class can
 * also be compiled and run by itself: javac EDProgram.java && java EDProgram.
 * The numbered colours follow the assignment diagram, not medical diagnosis.
 */
public class EDProgram {
    static final String[] COLORS = {"", "Red", "Orange", "Yellow", "Blue", "Green"};
    static final String[] LABELS = {"", "Critical", "Emergent", "Urgent", "Less urgent", "Non-urgent"};
    static final String[] TITLES = {"", "Admit patients", "Display room statuses",
        "Reject an occupied room", "Fill all ten rooms", "Reject a null patient",
        "Discharge non-critical patients", "Display updated rooms",
        "Discharge cleared patients", "Reject an empty-room discharge", "Search, peek & final status"};
    static final String[] EXPLANATIONS = {"",
        "Choose patients by severity, preserve arrival order for ties, and admit Lisa directly to room 10.",
        "Read every room and the sorted waiting queue without changing them.",
        "Room 1 is occupied. Reject another admission; keep its occupant and every waiting patient.",
        "Fill rooms 4–9. All ten rooms are occupied; an additional admission must fail without losing a patient.",
        "A null value is not a patient. Reject it before changing the room array or waiting queue.",
        "Discharge the non-critical sample patients in rooms 6 and 8, representing explicit medical clearance. Severity alone never triggers automatic discharge.",
        "Display the rooms after those discharges. Rooms 6 and 8 are available.",
        "After doctor clearance, discharge the occupants of rooms 1 and 3. Their old severity does not prevent discharge after recovery.",
        "Room 6 is already empty. Reject discharge and leave all counters unchanged.",
        "Find a discharged patient, show the final queue and rooms, and peek twice. Peek must not remove anyone."};

    // PATIENT: one visit. Immutable priority fields protect the heap's ordering.
    static class Patient {
        final String id, name, complaint;
        final int level;
        final Instant arrival;
        final long sequence;
        String status = "Waiting";
        Patient(String id, String name, String complaint, int level, Instant arrival, long sequence) {
            if (id == null || id.isBlank() || name == null || name.isBlank()
                    || complaint == null || complaint.isBlank() || level < 1 || level > 5)
                throw new IllegalArgumentException("Patient needs ID, name, complaint and ESI 1–5.");
            this.id=id.trim(); this.name=name.trim(); this.complaint=complaint.trim();
            this.level=level; this.arrival=Objects.requireNonNull(arrival); this.sequence=sequence;
        }
        String description() { return id+" · "+name+" · ESI "+level+" ("+COLORS[level]+")"; }
    }

    // COMPARATOR: a smaller ESI number means higher treatment priority.
    // Within a level, earlier arrival wins. Sequence resolves identical times.
    static class PatientComparator implements Comparator<Patient> {
        public int compare(Patient a, Patient b) {
            int severity=Integer.compare(a.level,b.level);
            if (severity!=0) return severity;
            int time=a.arrival.compareTo(b.arrival);
            return time!=0 ? time : Long.compare(a.sequence,b.sequence);
        }
    }

    // DATA STRUCTURES: heap for waiting, fixed array for rooms, map for lookup.
    // Ten rooms is our choice to support the assignment's room-10 example.
    final PriorityQueue<Patient> waiting = new PriorityQueue<>(new PatientComparator());
    final Patient[] rooms = new Patient[10];
    final HashMap<String,Patient> records = new HashMap<>();
    final List<String> log = new ArrayList<>();
    long sequence=0;
    int discharged=0;
    final Instant start=Instant.parse("2026-10-12T14:00:00Z");

    // REGISTRATION: reject duplicate IDs; store timestamps and a tie-break sequence.
    // Demo time advances 30 seconds per arrival so repeated tests are reproducible.
    Patient registerPatient(String id,String name,String complaint,int level) {
        if (id==null || records.containsKey(id.trim())) throw new IllegalArgumentException("Duplicate or missing ID.");
        Patient p=new Patient(id,name,complaint,level,start.plusSeconds(sequence*30),++sequence);
        records.put(p.id,p); waiting.add(p); log.add("ARRIVAL · "+p.description()); return p;
    }

    // PEEK: O(1). Inspect the queue head without removing it.
    Patient peekNextPatient() { return waiting.peek(); }

    // GET NEXT: O(log n). Remove the head but keep a SELECTED record for tracking.
    Patient getNextPatient() {
        Patient p=waiting.poll(); if(p!=null) p.status="Selected"; return p;
    }

    // ADMISSION: validate first; only successful admission changes the data.
    // A waiting patient is removed once. Direct admission is allowed for examples.
    boolean admitPatient(int room,Patient p) {
        if(p==null) {log.add("REJECTED · Null patient; nothing changed.");return false;}
        if(room<1 || room>10) {log.add("REJECTED · Room must be 1–10.");return false;}
        if(rooms[room-1]!=null) {log.add("REJECTED · Room "+room+" is occupied by "+rooms[room-1].name+". Queue preserved.");return false;}
        Patient known=records.get(p.id);
        if((known!=null && known!=p) || p.status.startsWith("Room") || p.status.equals("Discharged")) {
            log.add("REJECTED · Duplicate ID or patient already treated/discharged.");return false;
        }
        waiting.remove(p); records.put(p.id,p); rooms[room-1]=p; p.status="Room "+room;
        log.add("ADMITTED · "+p.description()+" → room "+room);return true;
    }

    // ADMIT NEXT: check availability BEFORE removing someone from the queue.
    // peek chooses the next patient; admitPatient performs the actual removal.
    boolean admitNextPatient(int room) {
        if(waiting.isEmpty()) {log.add("No patients waiting.");return false;}
        return admitPatient(room,peekNextPatient());
    }

    // DISCHARGE: explicit doctor-clearance action, never a capacity-based shortcut.
    boolean dischargePatient(int room) {
        if(room<1 || room>10 || rooms[room-1]==null) {
            log.add("REJECTED · Room "+room+" is invalid or empty. Nothing changed.");return false;
        }
        Patient p=rooms[room-1]; rooms[room-1]=null; p.status="Discharged"; discharged++;
        log.add("DISCHARGED · "+p.description()+" ← room "+room+"; room now available.");return true;
    }

    // SEARCH: HashMap lookup averages O(1); discharged records remain searchable.
    Patient findPatientById(String id) {return id==null?null:records.get(id.trim());}

    // DISPLAY: PriorityQueue iteration is NOT sorted. Poll a copy, not the live heap.
    List<Patient> treatmentOrder() {
        PriorityQueue<Patient> copy=new PriorityQueue<>(waiting); List<Patient> result=new ArrayList<>();
        while(!copy.isEmpty()) result.add(copy.poll()); return result;
    }
    List<Patient> arrivalOrder() {
        List<Patient> result=new ArrayList<>(records.values());
        result.sort(Comparator.comparingLong(p->p.sequence));return result;
    }
    int occupied() {int n=0;for(Patient p:rooms) if(p!=null)n++;return n;}
    void displayStatus() {
        log.add("STATUS · Waiting "+waiting.size()+"; occupied "+occupied()+"; available "+(10-occupied())+"; discharged "+discharged);
        for(int i=0;i<10;i++) log.add("Room "+(i+1)+": "+(rooms[i]==null?"Available":rooms[i].description()));
        for(Patient p:treatmentOrder()) log.add("WAITING · "+p.description());
    }

    // TESTS: every button executes one of these actual Java scenarios.
    // Browser requests first recreate prerequisite tests, then expose BEFORE/AFTER.
    // This lets you repeat or jump to a test without corrupting its starting state.
    boolean runTest(int number) {
        switch(number) {
            // TEST 1: first admissions; use getNextPatient and direct admission too.
            case 1:
                boolean first=admitNextPatient(1) && admitNextPatient(2);
                first=admitPatient(3,getNextPatient()) && first;
                Patient lisa=new Patient("DIRECT_LISA","Lisa Anderson","Flu",5,start.plusSeconds(sequence*30),++sequence);
                return admitPatient(10,lisa) && first && occupied()==4;
            // TEST 2: a read-only view; show room occupancy and sorted waiting list.
            case 2:
                String before2=stateJson();displayStatus();return before2.equals(stateJson());
            // TEST 3: reject an occupied room; both queue and room must be unchanged.
            case 3:
                String before3=stateJson();boolean admitted=admitNextPatient(1);
                return !admitted && before3.equals(stateJson());
            // TEST 4: fill remaining rooms; no extra patient may replace an occupant.
            case 4:
                for(int room=4;room<=9;room++) admitNextPatient(room);
                String full=stateJson();boolean extra=admitNextPatient(10);
                return occupied()==10 && !waiting.isEmpty() && !extra && full.equals(stateJson());
            // TEST 5: null input is rejected even when the chosen room is occupied.
            case 5:
                String before5=stateJson();boolean nullAdmit=admitPatient(5,null);
                return !nullAdmit && before5.equals(stateJson());
            // TEST 6: explicitly discharge non-critical sample occupants after clearance.
            // The assignment's wording says DISCHARGE, so we follow that instruction.
            case 6:
                if(rooms[5]==null || rooms[7]==null || rooms[5].level<3 || rooms[7].level<3) {
                    log.add("DATA CHECK · Test 6 needs non-critical occupants in rooms 6 and 8. Try the supplied sample CSV.");return false;
                }
                return dischargePatient(6) && dischargePatient(8);
            // TEST 7: show the updated rooms without changing state.
            case 7:
                String before7=stateJson();displayStatus();return before7.equals(stateJson());
            // TEST 8: patients in rooms 1 and 3 are now cleared by their doctor.
            case 8:return dischargePatient(1) && dischargePatient(3);
            // TEST 9: room 6 was emptied in Test 6; a second discharge is rejected.
            case 9:
                String before9=stateJson();boolean emptyDischarge=dischargePatient(6);
                return !emptyDischarge && before9.equals(stateJson());
            // TEST 10: final state, successful/unsuccessful lookup, repeated read-only peek.
            case 10:
                String before10=stateJson();displayStatus();
                Patient found=findPatientById("P003");
                log.add(found==null?"SEARCH · P003 not in this imported dataset.":"SEARCH · "+found.description()+" · "+found.status);
                log.add("SEARCH · UNKNOWN_ID: "+(findPatientById("UNKNOWN_ID")==null?"not found":"found"));
                Patient next=peekNextPatient();log.add("PEEK · "+(next==null?"Nobody waiting":next.description()));
                log.add("PEEK AGAIN · "+(peekNextPatient()==null?"Nobody waiting":peekNextPatient().description()));
                return before10.equals(stateJson()) && next==peekNextPatient();
            default:throw new IllegalArgumentException("Test must be 1–10.");
        }
    }

    // CSV IMPORT: simple four-column CSV, with quoted fields supported.
    // Validate the whole import before accepting it. No medical severity inference.
    static List<String[]> parseCsv(String csv) {
        List<String[]> rows=new ArrayList<>();Set<String> ids=new HashSet<>();int line=0;
        for(String raw:csv.split("\\R")) {
            line++;if(raw.isBlank())continue;
            List<String> values=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false;
            for(int i=0;i<raw.length();i++) {
                char c=raw.charAt(i);
                if(c=='"') {if(quoted && i+1<raw.length() && raw.charAt(i+1)=='"'){cell.append('"');i++;}else quoted=!quoted;}
                else if(c==',' && !quoted){values.add(cell.toString().trim());cell.setLength(0);}else cell.append(c);
            }
            if(quoted)throw new IllegalArgumentException("Unclosed quote on CSV line "+line);
            values.add(cell.toString().trim());
            if(line==1 && values.get(0).equalsIgnoreCase("id"))continue;
            if(values.size()!=4)throw new IllegalArgumentException("CSV line "+line+" needs id,name,complaint,level.");
            String[] row=values.toArray(new String[0]);
            int severity;try{severity=Integer.parseInt(row[3]);}catch(Exception e){throw new IllegalArgumentException("Invalid level on CSV line "+line);}
            new Patient(row[0],row[1],row[2],severity,Instant.EPOCH,0);
            if(!ids.add(row[0]) || row[0].equals("DIRECT_LISA"))throw new IllegalArgumentException("Duplicate or reserved ID on CSV line "+line);
            rows.add(row);
        }
        if(rows.isEmpty())throw new IllegalArgumentException("CSV has no patients.");
        if(rows.size()>100)throw new IllegalArgumentException("Use at most 100 demo patients.");return rows;
    }
    static EDProgram load(List<String[]> rows) {
        EDProgram ed=new EDProgram();for(String[] row:rows)ed.registerPatient(row[0],row[1],row[2],Integer.parseInt(row[3]));return ed;
    }

    // JSON DISPLAY SNAPSHOTS: transport only. JavaScript does not reorder patients.
    static String quote(String s) {
        if(s==null)return "null";StringBuilder b=new StringBuilder("\"");
        for(char c:s.toCharArray()) {switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}
        return b.append('"').toString();
    }
    static String patientJson(Patient p) {
        if(p==null)return "null";
        return "{\"id\":"+quote(p.id)+",\"name\":"+quote(p.name)+",\"complaint\":"+quote(p.complaint)
            +",\"level\":"+p.level+",\"color\":"+quote(COLORS[p.level])+",\"arrival\":"+quote(p.arrival.toString())
            +",\"sequence\":"+p.sequence+",\"status\":"+quote(p.status)+"}";
    }
    static String listJson(List<Patient> list) {return "["+String.join(",",list.stream().map(EDProgram::patientJson).toArray(String[]::new))+"]";}
    String stateJson() {
        List<String> roomData=new ArrayList<>();for(Patient p:rooms)roomData.add(patientJson(p));
        int[] counts=new int[6];for(Patient p:waiting)counts[p.level]++;
        return "{\"waiting\":"+listJson(treatmentOrder())+",\"arrivals\":"+listJson(arrivalOrder())
            +",\"rooms\":["+String.join(",",roomData)+"],\"occupied\":"+occupied()+",\"discharged\":"+discharged
            +",\"counts\":"+Arrays.toString(Arrays.copyOfRange(counts,1,6))+"}";
    }

    // CONSOLE ENTRY POINT: run the same ten tests without the website.
    public static void main(String[] args) throws Exception {
        List<String[]> rows=parseCsv(Files.readString(Path.of("data/patients.csv")));
        EDProgram ed=load(rows);
        for(int n=1;n<=10;n++) {
            ed.log.clear();boolean passed=ed.runTest(n);
            System.out.println("\nTEST "+n+" · "+TITLES[n]+" · "+(passed?"PASS":"FAIL"));
            ed.log.forEach(System.out::println);
            if(!passed)throw new AssertionError("Test "+n+" failed.");
        }
        // Extra proof: a later urgent patient beats earlier low-priority arrivals.
        EDProgram ties=new EDProgram();
        ties.registerPatient("A","Earlier","Example",4);
        Patient b=ties.registerPatient("B","Same level first","Example",2);
        Patient c=ties.registerPatient("C","Same level second","Example",2);
        Patient d=ties.registerPatient("D","Later critical","Example",1);
        if(ties.getNextPatient()!=d || ties.getNextPatient()!=b || ties.getNextPatient()!=c)
            throw new AssertionError("Severity/FIFO ordering failed.");
        System.out.println("\nALL TEN SCENARIOS + SEVERITY/FIFO CHECK PASSED.");
    }
}
