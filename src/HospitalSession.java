import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Replays one visitor's actions into a fresh Java hospital, without shared state. */
public class HospitalSession {
    static String[] decode(String line) {
        return Arrays.stream(line.split("\t",-1)).map(s->URLDecoder.decode(s,StandardCharsets.UTF_8)).toArray(String[]::new);
    }
    static boolean apply(EDProgram ed,String line) {
        String[] a=decode(line);
        switch(a[0]) {
            case "add": if(a.length!=5)throw new IllegalArgumentException("Patient fields missing.");
                if(ed.records.size()>=100)throw new IllegalArgumentException("Use at most 100 patients.");
                ed.registerPatient(a[1],a[2],a[3],Integer.parseInt(a[4]));return true;
            case "admit": return ed.admitNextPatient(Integer.parseInt(a[1]));
            case "discharge": return ed.dischargePatient(Integer.parseInt(a[1]));
            case "peek": EDProgram.Patient p=ed.peekNextPatient();ed.log.add("PEEK · "+(p==null?"Nobody waiting":p.description()));return true;
            case "search": EDProgram.Patient found=ed.findPatientById(a[1]);ed.log.add("SEARCH · "+(found==null?"Patient not found":found.description()+" · "+found.status));return true;
            case "state": return true;
            default: throw new IllegalArgumentException("Unknown hospital action.");
        }
    }
    static String execute(String csv,String history,String command) {
        EDProgram ed=csv.isBlank()?new EDProgram():EDProgram.load(EDProgram.parseCsv(csv));
        String[] actions=history.isEmpty()?new String[0]:history.split("\n");
        if(actions.length>300)throw new IllegalArgumentException("Start a new session after 300 actions.");
        for(String action:actions)if(!apply(ed,action))throw new IllegalArgumentException("Previous action could not be replayed. Reset the hospital after changing Java.");
        String before=ed.stateJson();ed.log.clear();boolean passed=apply(ed,command);
        String logs="["+String.join(",",ed.log.stream().map(EDProgram::quote).toArray(String[]::new))+"]";
        return "{\"passed\":"+passed+",\"before\":"+before+",\"after\":"+ed.stateJson()+",\"log\":"+logs+"}";
    }
    static Map<String,String> fields(String body) {
        Map<String,String> fields=new HashMap<>();
        for(String part:body.split("&")){String[] pair=part.split("=",2);if(pair.length==2)fields.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),URLDecoder.decode(pair[1],StandardCharsets.UTF_8));}
        return fields;
    }
}
