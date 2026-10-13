import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Independent behavioral checks, separate from the website's PASS indicators. */
public class EDVerification {
    static int checks=0;
    static void check(boolean condition,String description) {
        checks++;
        if(!condition)throw new AssertionError(description);
    }
    public static void main(String[] args) throws Exception {
        EDProgram ed=EDProgram.load(EDProgram.parseCsv(Files.readString(Path.of("data/patients.csv"))));
        int[] expectedOccupied={4,4,4,10,10,8,8,6,6,6};
        int[] expectedWaiting={9,9,9,3,3,3,3,3,3,3};
        int[] expectedDischarged={0,0,0,0,0,2,2,4,4,4};
        Set<Integer> readOnly=Set.of(2,3,5,7,9,10);
        for(int n=1;n<=10;n++) {
            String before=ed.stateJson();
            check(ed.runTest(n),"Assignment Test "+n+" should pass");
            check(ed.occupied()==expectedOccupied[n-1],"Occupancy after Test "+n);
            check(ed.waiting.size()==expectedWaiting[n-1],"Queue size after Test "+n);
            check(ed.discharged==expectedDischarged[n-1],"Discharge count after Test "+n);
            if(readOnly.contains(n))check(before.equals(ed.stateJson()),"Test "+n+" must preserve state");
            for(EDProgram.Patient p:ed.waiting) {
                for(EDProgram.Patient occupant:ed.rooms)check(p!=occupant,"No patient waiting and in a room simultaneously");
            }
        }
        check(ed.rooms[5]==null,"Room 6 must be empty for Test 9");
        check(ed.findPatientById("P003").status.equals("Discharged"),"Discharged patients stay searchable");
        check(ed.findPatientById("missing")==null,"Unknown ID lookup returns no patient");

        // Highest urgency beats arrival order; equal-level arrivals remain FIFO.
        EDProgram order=new EDProgram();
        EDProgram.Patient early=order.registerPatient("A","First arrival","Example",5);
        EDProgram.Patient firstTie=order.registerPatient("B","Earlier level two","Example",2);
        EDProgram.Patient secondTie=order.registerPatient("C","Later level two","Example",2);
        EDProgram.Patient urgent=order.registerPatient("D","Last arrival","Example",1);
        check(order.getNextPatient()==urgent,"Later ESI 1 precedes earlier ESI 5");
        check(order.getNextPatient()==firstTie,"Earlier equal-level patient first");
        check(order.getNextPatient()==secondTie,"Later equal-level patient second");
        check(order.getNextPatient()==early,"Least urgent patient last");
        check(order.peekNextPatient()==null && order.getNextPatient()==null,"Empty queue handled");
        // Equal timestamps still preserve registration sequence.
        EDProgram.Patient same1=new EDProgram.Patient("T1","One","Example",3,Instant.EPOCH,1);
        EDProgram.Patient same2=new EDProgram.Patient("T2","Two","Example",3,Instant.EPOCH,2);
        PriorityQueue<EDProgram.Patient> tied=new PriorityQueue<>(new EDProgram.PatientComparator());
        tied.add(same2);tied.add(same1);
        check(tied.poll()==same1 && tied.poll()==same2,"Identical timestamps use arrival sequence");

        EDProgram failures=new EDProgram();
        EDProgram.Patient patient=failures.registerPatient("V1","Valid","Example",2);
        String before=failures.stateJson();long sequence=failures.sequence;
        try{failures.registerPatient("BAD","","Example",3);throw new AssertionError("Blank name accepted");}
        catch(IllegalArgumentException expected){}
        check(failures.sequence==sequence && failures.stateJson().equals(before),"Rejected registration changes no state");
        check(!failures.admitPatient(1,null),"Null patient rejected");
        check(!failures.admitNextPatient(0) && !failures.admitNextPatient(11),"Invalid room numbers rejected");
        check(failures.stateJson().equals(before),"Rejected admissions preserve queue and rooms");
        EDProgram.Patient selected=failures.getNextPatient();
        check(selected==patient && failures.findPatientById("V1")==patient,"Get-next preserves tracking");
        check(failures.admitPatient(1,selected),"Selected patient admitted");
        check(!failures.admitPatient(2,selected),"Patient cannot occupy two rooms");
        failures.registerPatient("V2","Waiting","Example",3);
        before=failures.stateJson();
        check(!failures.admitNextPatient(1) && before.equals(failures.stateJson()),"Occupied room preserves waiting patient");
        check(failures.dischargePatient(1),"Discharge frees room");
        before=failures.stateJson();
        check(!failures.dischargePatient(1) && before.equals(failures.stateJson()),"Empty discharge preserves counters");
        check(failures.admitNextPatient(1),"Freed room accepts next waiting patient");
        try{failures.registerPatient("V1","Duplicate","Example",1);throw new AssertionError("Duplicate ID accepted");}
        catch(IllegalArgumentException expected){}
        System.out.println("Independent checks passed: "+checks);
    }
}
