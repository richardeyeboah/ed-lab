import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Local bridge: browser requests call the actual Java assignment methods. */
public class WebsiteServer {
    static List<String[]> dataset;
    static final Path WEB=Path.of("web").toAbsolutePath();
    static void send(HttpExchange x,int status,String type,String body) throws IOException {
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control","no-store");
        x.sendResponseHeaders(status,bytes.length);x.getResponseBody().write(bytes);x.close();
    }
    static List<String[]> requestDataset(HttpExchange x) throws IOException {
        byte[] bytes=x.getRequestBody().readNBytes(100001);
        if(bytes.length>100000)throw new IllegalArgumentException("CSV is too large.");
        return bytes.length==0 ? dataset : EDProgram.parseCsv(new String(bytes,StandardCharsets.UTF_8));
    }
    public static void main(String[] args) throws Exception {
        dataset=EDProgram.parseCsv(Files.readString(Path.of("data/patients.csv")));
        String configuredPort=System.getenv("PORT");
        int port=args.length>0?Integer.parseInt(args[0]):configuredPort==null?8765:Integer.parseInt(configuredPort);
        HttpServer server=HttpServer.create(new InetSocketAddress(configuredPort==null?"127.0.0.1":"0.0.0.0",port),0);
        server.createContext("/api",x->{
            try {
                // All test requests are stateless. Each visitor supplies their own CSV;
                // no uploaded dataset can change what another visitor sees.
                String path=x.getRequestURI().getPath();
                if(path.equals("/api/edit")){EditorExecution.handle(x);return;}
                if(path.equals("/api/source")) {send(x,200,"text/plain",Files.readString(Path.of("src/EDProgram.java")));return;}
                if(path.equals("/api/sample")) {send(x,200,"text/csv",Files.readString(Path.of("data/patients.csv")));return;}
                if(path.equals("/api/state")) {send(x,200,"application/json",EDProgram.load(dataset).stateJson());return;}
                if(path.equals("/api/prepare")) {
                    String query=x.getRequestURI().getQuery();
                    int n=Integer.parseInt(query==null?"0":query.replace("test=",""));
                    if(n<1 || n>10)throw new IllegalArgumentException("Test must be 1–10.");
                    EDProgram ed=EDProgram.load(requestDataset(x));
                    for(int k=1;k<n;k++)if(!ed.runTest(k))throw new IllegalArgumentException("Starting data failed prerequisite Test "+k+". Reload sample data.");
                    send(x,200,"application/json",ed.stateJson());return;
                }
                if(!x.getRequestMethod().equals("POST")){send(x,405,"application/json","{\"error\":\"POST required.\"}");return;}
                if(path.equals("/api/import")) {
                    byte[] bytes=x.getRequestBody().readNBytes(100001);
                    if(bytes.length>100000)throw new IllegalArgumentException("CSV is too large.");
                    List<String[]> next=EDProgram.parseCsv(new String(bytes,StandardCharsets.UTF_8));
                    send(x,200,"application/json",EDProgram.load(next).stateJson());return;
                }
                if(path.equals("/api/run")) {
                    String query=x.getRequestURI().getQuery();
                    int n=Integer.parseInt(query==null?"0":query.replace("test=",""));
                    if(n<1 || n>10)throw new IllegalArgumentException("Test must be 1–10.");
                    EDProgram ed=EDProgram.load(requestDataset(x));
                    for(int k=1;k<n;k++)if(!ed.runTest(k))throw new IllegalArgumentException("Starting data failed prerequisite Test "+k+". Reload sample data.");
                    String before=ed.stateJson();ed.log.clear();boolean passed=ed.runTest(n);
                    String messages="["+String.join(",",ed.log.stream().map(EDProgram::quote).toArray(String[]::new))+"]";
                    String result="{\"test\":"+n+",\"title\":"+EDProgram.quote(EDProgram.TITLES[n])
                        +",\"explanation\":"+EDProgram.quote(EDProgram.EXPLANATIONS[n])+",\"passed\":"+passed
                        +",\"before\":"+before+",\"after\":"+ed.stateJson()+",\"log\":"+messages+"}";
                    send(x,200,"application/json",result);return;
                }
                send(x,404,"application/json","{\"error\":\"Unknown request.\"}");
            }catch(Exception e){send(x,400,"application/json","{\"error\":"+EDProgram.quote(e.getMessage())+"}");}
        });
        server.createContext("/",x->{
            Path file=WEB.resolve(x.getRequestURI().getPath().substring(1)).normalize();
            if(x.getRequestURI().getPath().equals("/"))file=WEB.resolve("index.html");
            if(!file.startsWith(WEB) || !Files.isRegularFile(file)){send(x,404,"text/plain","Not found");return;}
            String name=file.toString();String type=name.endsWith(".css")?"text/css":name.endsWith(".js")?"application/javascript":name.endsWith(".ttf")?"font/ttf":name.endsWith(".woff")?"font/woff":name.endsWith(".json")?"application/json":name.endsWith(".html")?"text/html":"application/octet-stream";
            byte[] bytes=Files.readAllBytes(file);
            x.getResponseHeaders().set("Content-Type",type);
            x.sendResponseHeaders(200,bytes.length);x.getResponseBody().write(bytes);x.close();
        });
        server.start();System.out.println("Open http://127.0.0.1:"+port+" — actual Java backend running. Ctrl+C stops it.");
    }
}
