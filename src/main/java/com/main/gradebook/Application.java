package com.main.gradebook;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@SpringBootApplication
@RestController
@ComponentScan(basePackageClasses = Application.class)
public class Application extends SpringBootServletInitializer {

    private static ConfigurableApplicationContext context;

    private AtomicInteger idCounter = new AtomicInteger();
    private static int presentId = 0;
    private Map<Integer, String> gradebookObject = new ConcurrentHashMap<Integer, String>();
    private ArrayList allServers = new ArrayList(Arrays.asList( "8081", "8082", "8083", 
            "8084", "8085", "8086", "8087", "8088", "8089", "9001", "9002", "9003"));
    
    private Map<Integer, String> available_secondary_copy = new ConcurrentHashMap<Integer, String>();

    private static Map<Integer, String> gradebookObjectSecondary = new ConcurrentHashMap<Integer, String>();
    private Map<Integer, String> ports = new ConcurrentHashMap<>();
    private final String[] availablePorts = {"8086", "8087", "8088", "8089", "9001", "9002", "9003"};
    private HashMap<String, Integer> primaryGradeBooks = new HashMap<>();
    private HashMap<String, Integer> secondaryServer = new HashMap<>();
    private List<Student> students = new ArrayList();
    private int id = 0;
    private boolean status = false;

    @Value("${client.type}")
    private String type;  // Determines if admin of secondary / client

    @Value("${server.port}")
    private String port;
    
    @Value("${server.id}")
    private String serverId;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);

    }

    public static void main(String[] args) {
        // System.setProperty("spring.datasource.url","jdbc:h2:tcp://localhost:9090/mem:mydb");
        for (String s : args) {
            String[] split = s.split(":");
            String key = split[0];
            String value = split[1];
            System.out.println("Args set " + key + " value " + value);
            System.setProperty(key, value);
        }
        context = SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    private void postConstruct() {
        if (type.equals("admin") || type.equals("secondary")) {
            System.out.println("Newly created -->");
            if (!port.equals("8081")) {
                System.out.println("Query primary Server -->");
                queryPrimaryServer("gradebook");
                queryPrimaryServer("students");
                if(type.equals("admin")) queryPrimaryServer("secondaryServers");
                
                
                if(type.equals("secondary")) {
                    available_secondary_copy.put(Integer.parseInt(serverId), "");
                } 
            }
        }
    }
    
    @RequestMapping(value = "/secondaryServers", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    @ResponseBody
    public PortList<Port> getport() {
        ArrayList<Port> portList = new ArrayList<>();
        secondaryServer.forEach((k, v) -> {
            Port p = new Port();
            p.setPort(k);
            p.setId(v);
            portList.add(p);
        });
        
        PortList<Port> p = new PortList(portList);
        return p;
    } 

    @RequestMapping(value = "/gradebook", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    @ResponseBody
    public GradeList<Gradebook> gradebook() {

        ArrayList<Gradebook> gradebookList = new ArrayList<>();
        System.out.println("Port running is " + port);
        
        gradebookObject.forEach((k, v) -> {
            Gradebook gradeBook = new Gradebook();
            gradeBook.setId(k);
            gradeBook.setName(v);
             if(type.equals("secondary")) {
                 if(available_secondary_copy.containsKey(k)) {
                     gradebookList.add(gradeBook);
                 }
             } else{
                 gradebookList.add(gradeBook);
             }
        });

        GradeList<Gradebook> list = new GradeList<>(gradebookList);

        return list;
    }

    @RequestMapping(value = "/gradebook/primary/{id}/{name}", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    @ResponseBody
    public int updateServer(@PathVariable("id") int id, @PathVariable("name") String name) {
        if (type.equals("admin") || type.equals("secondary")) {
            System.out.println("From Primary Server to another primary Server ---> " + id + " title = " + name);
            gradebookObject.put(id, name);
            return 1;
        }
        return 0;
    }
    
    @RequestMapping(value = "/gradebook/primary/{id}/secondary/port/{port}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
    @ResponseBody
    public int updateSecondaryData(@PathVariable("id") int id, @PathVariable("port") String port) {
        if (type.equals("admin")) {
            System.out.println("We came to update the secondary port");
            secondaryServer.put(port, id);
            return 1;
        }    
        
        return 0;
    }

    @RequestMapping(value = "/gradebook/{name}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
    @ResponseBody
    public Gradebook addGradeBook(@PathVariable("name") String name) throws Exception {
        // Only primary
        
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized access");
        }

        presentId = idCounter.incrementAndGet();

        while (gradebookObject.containsKey(presentId)) {
            presentId = idCounter.incrementAndGet();
        }
        
        
        if(!validName(name)) {
            throw new Exception("Name must contain only characters and no space");
        }

        if (gradebookObject.containsValue(name)) {
            
            throw new Exception(name+ "Name Exits in systm");
        }

        // Check all Servers
        allServers.forEach(i -> {
            try {

                if (!i.toString().equals(port)) {
                    postPrimaryServer("http://localhost:" + i + "/gradebook/primary/" + presentId + "/" + name, "GET");
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        });
        gradebookObject.put(presentId, name);
        Gradebook g = new Gradebook();
        g.setId(presentId);
        g.setName(name);

        return g;
    }

    @RequestMapping(value = "/gradebook/{id}", method = RequestMethod.DELETE, produces = {"application/xml", "application/json"})
    @ResponseBody
    public Object deleteGradeBook(@PathVariable("id") int id) throws Exception {
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized access");
        }
        
        if (gradebookObject.containsKey(id)) {
            gradebookObject.remove(id);
            allServers.forEach(i -> {
                try {
                    if (!i.toString().equals(port)) {
                        postPrimaryServer("http://localhost:" + i + "/gradebook/" + id, "DELETE");
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            });

            if (secondaryServer.containsValue(id)) {
                secondaryServer.forEach((k, v) -> {
                    if (v == id) {
                        String portServer = k;
                        try {
                            postPrimaryServer("http://localhost:" + portServer + "/secondary/" + id, "DELETE");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                // Check if secondary server exists
                // Propagate to all secondary servers
            }
            
            return null;

        } else {
            System.out.println("");
        }
        return null;

    }

    @RequestMapping(value = "/secondary/{id}", method = RequestMethod.POST, produces = {"application/xml", "application/json"})
    @ResponseBody
    public Student secondaryAddGradeBook(@PathVariable("id") int id) throws Exception {
        
        if(!gradebookObject.containsKey(id)) {
            System.err.println("Grade book does not exists");
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "GradeBook does not exists");
        }
        
        
        if (type.equals("admin")) {
            if (secondaryServer.containsValue(id)) {
                System.out.println("Server already Exists");
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Secondary Server Already Exists");
                // throw new Exception("Server Already Exists");
            
            } else {
                String portNumber = availablePorts[secondaryServer.size()];
                secondaryServer.put(portNumber, id);
                startServer(new String[]{"server.port:" + portNumber + "", "primary.port:" + port + "", "server.id:" + id + "", "client.type:secondary"});
                allServers.forEach(i -> {
                try {
                    if (!i.toString().equals(port)) {
                        postPrimaryServer("http://localhost:" + i + "/gradebook/primary/" + id+"/secondary/port/"+port, "POST");
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            });
      
            } 
            
        } else {
            if(type.equals("secondary")) {             
                if(available_secondary_copy.containsKey(id)) {
                    throw new Exception("Copy already exist on the server");
                } else {
                    if(!gradebookObject.containsKey(id)) {
                        throw new Exception("Grade Book id does not exist");
                    } else {
                        available_secondary_copy.put(id, "");
                    }
                }
                // Create Secondary copy
            }
        }
        
        return null;
    }

    @RequestMapping(value = "/secondary/{id}", method = RequestMethod.DELETE, produces = {"application/xml", "application/json"})
    @ResponseBody
    public ResponseBody secondaryDeleteGradeBook(@PathVariable("id") int id) throws Exception {
        available_secondary_copy.remove(id);

        return null;
    }

    @RequestMapping(value = "/gradebook/{id}/student", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    @ResponseBody
    public StudentList<Student>  searchStudentGradebook(@PathVariable("id") int id) {
        // Allowed on primary and secondary
        
        StudentList<Student> list = new StudentList<>(students.stream().filter(i -> i.getId() == id).collect(Collectors.toList()));
        return list;
    }

    @RequestMapping(value = "/students", method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    @ResponseBody
    public StudentList<Student> getAllStudents() {
        StudentList<Student> list = new StudentList(students);///students;
        return list;
    } 

    @RequestMapping(value = "/gradebook/{id}/student/{name}",
            method = RequestMethod.GET, produces = {"application/xml", "application/json"})
    @ResponseBody
    public StudentList<Student> searchStudentGradebookName(@PathVariable("id") int id,
            @PathVariable("name") String name) {
        // Allowed on primary and secondary
        StudentList<Student> list = new StudentList<>(students.stream().filter(i -> i.getId() == id && i.getName().equals(name)).collect(Collectors.toList()));
        return list;
    }
    
    @RequestMapping(value = "/gradebook/primary/{id}/student/{name}/index/{index}",
            method = RequestMethod.DELETE,
            produces = {"application/xml", "application/json"})
    @ResponseBody
    public List<Student> deleteStudentGradebookNamePrimaryDuplicate(@PathVariable("id") int id,
            @PathVariable("name") String name, @PathVariable("index") int index) throws Exception {
        
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized");
        }
        
        if(index != -1){
            
            students.remove(index);
        }
         
        return null;
    }
    
    int deleteIndex = -1;

    @RequestMapping(value = "/gradebook/{id}/student/{name}",
            method = RequestMethod.DELETE,
            produces = {"application/xml", "application/json"})
    @ResponseBody
    public List<Student> deleteStudentGradebookNamePrimary(@PathVariable("id") int id,
            @PathVariable("name") String name) throws Exception {
        // Allowed on primary and should delete on secondary
        
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized");
        }
        
        int index = -1;
        deleteIndex = -1;
        
        for(int c=0; c < students.size(); c++) {
             Student stud = students.get(c);
                if(stud.getId() == id && stud.getName().equals(name)) {
                    index = c;
                    deleteIndex = c;
                }
        }
        
        if(index == -1) {
            return null;
        } 
       
        students.remove(index);
        allServers.forEach((i -> {
            try {
                if (!i.toString().equals(port)) {
                    postPrimaryServer("http://localhost:" + i + "/gradebook/primary/" + id + "/student/" + name + "/index/"+ deleteIndex, "DELETE");
                }
            } catch(Exception e) {
                
            }
        }));

        if (secondaryServer.containsValue(id)) {
            //Propagate to secondary.
        }

        return null;
    } 

    @RequestMapping(value = "/gradebook/primary/{id}/student/{name}/grade/{grade}",
            method = RequestMethod.POST,
            produces = {"application/xml", "application/json"})
    @ResponseBody
    public Student addStudentgradePrimaryPrimary(@PathVariable("id") int id,
            @PathVariable("name") String name,
            @PathVariable("grade") String grade) throws Exception {
        
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized");
        }

        Student student = new Student();
        student.setId(id);
        student.setGrade(grade);
        student.setName(name);

        if (gradebookObject.containsKey(id)) {
            students.add(student);
        }
        return null;
    }

    @RequestMapping(value = "/gradebook/{id}/student/{name}/grade/{grade}",
            method = RequestMethod.POST,
            produces = {"application/xml", "application/json"})
    @ResponseBody
    public Student addStudentgradePrimary(@PathVariable("id") int id,
            @PathVariable("name") String name,
            @PathVariable("grade") String grade) throws Exception {
        
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized");
        }
        
        boolean found = false;
        
        for(int c =0; c < students.size(); c++) {
            Student student = students.get(c);
            if(student.getId() == id && student.getName().equals(name)) {
                found = true;
            }
        }
        
        
        if(found) {
            throw new Exception(name + " Exist in grade "+ grade);
        }
        
        
        if(name.isEmpty() || !validGrade(grade)) {
            throw new Exception("bad Data");
        }

        Student student = new Student();
        student.setId(id);
        student.setGrade(grade);
        student.setName(name);

        if (gradebookObject.containsKey(id)) {
            students.add(student);
            allServers.forEach(i -> {
                try {
                    if (!i.toString().equals(port)) {
                        postPrimaryServer("http://localhost:" + i + "/gradebook/primary/" + id + "/student/" + name + "/grade/" + grade, "POST");
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    //e.printStackTrace();
                }
            });

        }
        // Send to secondary
        if (secondaryServer.containsValue(id)) {
            //Propagate to secondary.
        }

        return student;

    }
    
    @RequestMapping(value = "/gradebook/primary/{id}/student/{name}/grade/{grade}/index/{index}",
            method = RequestMethod.PUT,
            produces = {"application/xml", "application/json"})
    @ResponseBody
    public Student updateStudentgradePrimaryDuplicate(@PathVariable("id") int id,
            @PathVariable("name") String name,
            @PathVariable("grade") String grade, @PathVariable("index") int index) throws Exception {
        // Allowed on primary and should update on secondary
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized");
        }
        
        if(index!= -1) {
            students.remove(index);
            Student student = new Student();
            student.setId(id);
            student.setGrade(grade);
            student.setName(name);
            students.add(index, student);
            return null;
        }

        return null;

    }
    
    static int indexUpdate = -1;
    @RequestMapping(value = "/gradebook/{id}/student/{name}/grade/{grade}",
            method = RequestMethod.PUT,
            produces = {"application/xml", "application/json"})
    @ResponseBody
    public Student updateStudentgradePrimary(@PathVariable("id") int id,
            @PathVariable("name") String name,
            @PathVariable("grade") String grade) throws Exception {
        
        if (!type.equals("admin")) {
            throw new IllegalAccessException("unathorized");
        }
        // Allowed on primary and should update on secondary
        
        
       
        
        if(!validGrade(grade)) {
            throw new Exception("bad Grade Data");
        }
              
        indexUpdate = -1;
        Student student = new Student();
        student.setId(id);
        student.setGrade(grade);
        student.setName(name);
        int index = -1;
        for(int c=0; c < students.size(); c++) {
            Student stud = students.get(c);
            if(stud.getId() == id && stud.getName().equals(name)) {
                index = c;
            }
        }
        
        if(index == -1) {
            return null;
        }
        
        else {
            indexUpdate= index;
            students.remove(index);
            students.add(index, student);
            allServers.forEach((i) -> {
                try {
                    if (!i.toString().equals(port)) {
                        postPrimaryServer("http://localhost:" + i + "/gradebook/primary/" + id + "/student/" + name + "/grade/" + grade +"/index/"+indexUpdate, "PUT");
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    //e.printStackTrace();
                }
            });
            
        }

        return null;

    }

    private boolean validGrade(String letter) {
        return Pattern.matches("[abcdefiwzABCDEFIWZ][+-]*", letter); 
    }
    
     private boolean validName(String letter) {
        return Pattern.matches("[a-zA-Z]+", letter);
    }

    private boolean notEmpty(String type) {
        return !type.isEmpty();
    }

    private void clearGradeBookDatabase() {

    }

    private void clearStudentDatabase() {

    }

    public int gradbookCount() {
        return 0;
    }

    public static void startServer(String[] args) {
        Thread thread = new Thread(() -> {
            // DemoApplication.main(new String[]{"server.port:8081"});
            Application.main(args);

        });

        thread.setDaemon(false);
        thread.start();
    }

    public static void shutDownServer() {

        try {
            Socket socket = new Socket("localhost", 8080);
            if (socket.isConnected()) {
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                pw.println("SHUTDOWN");//send shut down command 
                pw.close();
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void postPrimaryServer(String urlData, String type) throws Exception {
        URL url = new URL(urlData);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(type);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept", "application/xml");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        String output;
        System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
            System.out.println(output);

        }

        conn.disconnect();

    }

    public void queryPrimaryServer(String type) {
        try {

            URL url = new URL("http://localhost:8081/" + type);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/xml");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            
            
            if(type.equals("student")) {
                readStudentGrade(conn.getInputStream(), conn);
            }

            if (type.equals("gradebook")) {
                readGradebook(conn.getInputStream(), conn);
            } 
            
            if(type.equals("secondaryServers")) {
                readSecondaryport(conn.getInputStream(), conn);
            }
            

        } catch (MalformedURLException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
    
    protected void readSecondaryport(InputStream is, HttpURLConnection conn) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            if (root.getAttribute("id") != null && !root.getAttribute("id").trim().equals("")) {

            }

           
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Port port = new Port();
                Element element = (Element) nodes.item(i);
                for (int c = 0; c < element.getChildNodes().getLength(); c++) {
                     

                    Element e = (Element) element.getChildNodes().item(c);

                    if (e.getTagName().equals("id")) {
                        port.setId(Integer.parseInt(e.getTextContent()));
                    }
                    if (e.getTagName().equals("port")) {
                        port.setPort(e.getTextContent());
                    }      

                   if (port.getPort() != null ) {
                        secondaryServer.put(port.getPort(), port.getId());
                        
                    }
                }
                

            }
            conn.disconnect();

        } catch (Exception e) {

        }
    }

    protected void readStudentGrade(InputStream is, HttpURLConnection conn) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            if (root.getAttribute("id") != null && !root.getAttribute("id").trim().equals("")) {

            }

           
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Student student = new Student();
                Element element = (Element) nodes.item(i);
                for (int c = 0; c < element.getChildNodes().getLength(); c++) {
                     

                    Element e = (Element) element.getChildNodes().item(c);

                    if (e.getTagName().equals("id")) {
                        student.setId(Integer.parseInt(e.getTextContent()));
                    }
                    if (e.getTagName().equals("name")) {
                        student.setName(e.getTextContent());
                    }

                    if (e.getTagName().equals("grade")) {
                        student.setGrade(e.getTextContent());
                    }

                   if (student.getName() != null && student.getGrade() != null) {
                        students.add(student);
                        
                    }
                }
                

            }
            conn.disconnect();

        } catch (Exception e) {

        }
    }

    protected GradeList<Gradebook> readGradebook(InputStream is, HttpURLConnection conn) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            if (root.getAttribute("id") != null && !root.getAttribute("id").trim().equals("")) {

            }
            Gradebook gradebook = new Gradebook();
            NodeList nodes = root.getChildNodes();

            for (int i = 0; i < nodes.getLength(); i++) {

                Element element = (Element) nodes.item(i);
                for (int c = 0; c < element.getChildNodes().getLength(); c++) {

                    Element e = (Element) element.getChildNodes().item(c);

                    if (e.getTagName().equals("id")) {
                        gradebook.setId(Integer.parseInt(e.getTextContent()));
                    }
                    if (e.getTagName().equals("name")) {
                        gradebook.setName(e.getTextContent());
                    }

                    if (gradebook.getName() != null) {
                        gradebookObject.put(gradebook.getId(), gradebook.getName());
                    }

                }

            }
            conn.disconnect();
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            //hrow new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        return null;
    }

    private static Document convertXMLFileToXMLDocument(String filePath) {
        //Parser that produces DOM object trees from XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        //API to obtain DOM Document instance
        DocumentBuilder builder = null;
        try {
            //Create DocumentBuilder with default configuration
            builder = factory.newDocumentBuilder();

            //Parse the content to Document object
            Document doc = builder.parse(new File(filePath));
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    class JsonConverterGradeList {

        @JsonProperty("items")
        public GradeList<Gradebook> list;
    }

    class JsonConverterGradebook {

        @JsonProperty("id")
        public int id;
        @JsonProperty("name")
        public String name;
    }

}
