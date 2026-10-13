import com.sun.net.httpserver.HttpExchange;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import java.io.*;

/** Compile a visitor's edited Java in a temporary folder; never overwrite the repository. */
public class EditorExecution {
    static final Semaphore slots=new Semaphore(2);
    static final String RUNNER="""
        import java.nio.file.*;
        public class EditedTestRunner {
            public static void main(String[] args) {
                try {
                    int n=Integer.parseInt(args[0]);
                    EDProgram ed=EDProgram.load(EDProgram.parseCsv(Files.readString(Path.of("patients.csv"))));
                    for(int k=1;k<n;k++)if(!ed.runTest(k))throw new IllegalArgumentException("Your code/data failed prerequisite Test "+k);
                    String before=ed.stateJson();
                    if(args[1].equals("prepare")) {System.out.println("ED_RESULT "+before);return;}
                    ed.log.clear();boolean passed=ed.runTest(n);
                    String logs="["+String.join(",",ed.log.stream().map(EDProgram::quote).toArray(String[]::new))+"]";
                    System.out.println("ED_RESULT {\\"test\\":"+n+",\\"title\\":"+EDProgram.quote(EDProgram.TITLES[n])
                        +",\\"explanation\\":"+EDProgram.quote(EDProgram.EXPLANATIONS[n])+",\\"passed\\":"+passed
                        +",\\"before\\":"+before+",\\"after\\":"+ed.stateJson()+",\\"log\\":"+logs+"}");
                } catch(Throwable e) {
                    System.out.println("ED_RESULT {\\"error\\":"+EDProgram.quote(e.toString())+"}");
                }
            }
        }
        """;
    static String capture(List<String> command,Path folder,int seconds) throws Exception {
        ProcessBuilder builder=new ProcessBuilder(command).directory(folder.toFile()).redirectErrorStream(true);
        // Edited code receives no Vercel credentials or application environment.
        builder.environment().clear();builder.environment().put("LANG","C.UTF-8");
        Process process=builder.start();
        ExecutorService reader=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});
        Future<String> output=reader.submit(()->{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int count;
            while((count=process.getInputStream().read(chunk))!=-1) {
                if(bytes.size()+count>64000){process.destroyForcibly();throw new IOException("Output limit exceeded.");}
                bytes.write(chunk,0,count);
            }
            return bytes.toString(StandardCharsets.UTF_8);
        });
        try {
            if(!process.waitFor(seconds,TimeUnit.SECONDS)){process.destroyForcibly();throw new IOException("Java took too long. Check for an infinite loop.");}
            String text=output.get(2,TimeUnit.SECONDS);
            if(process.exitValue()!=0)throw new IOException(text.replace(folder.toString(),"your-code"));
            return text;
        } finally {process.destroyForcibly();reader.shutdownNow();}
    }
    static void handle(HttpExchange x) throws Exception {
        if(!x.getRequestMethod().equals("POST")){WebsiteServer.send(x,405,"application/json","{\"error\":\"POST required\"}");return;}
        if(!slots.tryAcquire()){WebsiteServer.send(x,429,"application/json","{\"error\":\"Compiler is busy. Try again shortly.\"}");return;}
        Path folder=null;
        try {
            byte[] body=x.getRequestBody().readNBytes(250001);
            if(body.length>250000)throw new IllegalArgumentException("Code and data are too large.");
            Map<String,String> fields=new HashMap<>();
            for(String part:new String(body,StandardCharsets.UTF_8).split("&")) {
                String[] entry=part.split("=",2);
                if(entry.length==2)fields.put(URLDecoder.decode(entry[0],StandardCharsets.UTF_8),URLDecoder.decode(entry[1],StandardCharsets.UTF_8));
            }
            String source=fields.getOrDefault("source","");
            if(source.length()>100000 || !source.contains("class EDProgram"))throw new IllegalArgumentException("Keep the EDProgram class name; use at most 100,000 characters.");
            int test=Integer.parseInt(fields.getOrDefault("test","0"));
            if(test<1 || test>10)throw new IllegalArgumentException("Test must be 1–10.");
            String mode=fields.getOrDefault("mode","run");if(!mode.equals("run")&&!mode.equals("prepare"))throw new IllegalArgumentException("Unknown mode.");
            String csv=fields.getOrDefault("csv","");EDProgram.parseCsv(csv);
            folder=Files.createTempDirectory("ed-editor-");
            Files.writeString(folder.resolve("EDProgram.java"),source);
            Files.writeString(folder.resolve("EditedTestRunner.java"),RUNNER);
            Files.writeString(folder.resolve("patients.csv"),csv);
            String java=Path.of(System.getProperty("java.home"),"bin","java").toString();
            String javac=Path.of(System.getProperty("java.home"),"bin","javac").toString();
            // -proc:none prevents source compilation from running annotation processors.
            capture(List.of(javac,"-J-Xmx96m","-proc:none","--release","17","-d",folder.toString(),"EDProgram.java","EditedTestRunner.java"),folder,12);
            // Java 21's SecurityManager restricts edited code to reading its own files.
            // No sockets, process execution, file writes, environment access, or System.exit.
            String safe=folder.toString().replace("\\","\\\\");
            Path policy=folder.resolve("execution.policy");
            Files.writeString(policy,"grant { permission java.io.FilePermission \""+safe+"/-\", \"read\"; };\n");
            String output=capture(List.of(java,"-Xmx64m","-XX:MaxMetaspaceSize=64m","-XX:ActiveProcessorCount=1",
                "-Djava.security.manager","-Djava.security.policy=="+policy,"-cp",folder.toString(),"EditedTestRunner",String.valueOf(test),mode),folder,6);
            int marker=output.lastIndexOf("ED_RESULT ");
            if(marker<0)throw new IllegalArgumentException("Your code produced no test result. Keep the assignment methods and JSON snapshot helpers.");
            String result=output.substring(marker+10).strip();
            WebsiteServer.send(x,result.startsWith("{\"error\"")?400:200,"application/json",result);
        }catch(Exception e){WebsiteServer.send(x,400,"application/json","{\"error\":"+EDProgram.quote(e.getMessage())+"}");}
        finally {
            if(folder!=null)try(Stream<Path> files=Files.walk(folder)){for(Path file:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(file);}catch(IOException ignored){}
            slots.release();
        }
    }
}
