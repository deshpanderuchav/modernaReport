package com.liconic.report;

import com.liconic.binding.sys.Job;
import com.liconic.binding.sys.Plate;
import com.liconic.binding.sys.Sys;
import com.liconic.binding.sys.Task;
import com.liconic.binding.sys.TubePos;
import com.liconic.db.DBKIWIConnection;
import com.liconic.db.DBKIWIModule;
import com.liconic.restful.TaskReportResource;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import sun.net.www.http.HttpClient;


public class Report {
    private static final Logger log = Logger.getLogger(Report.class.getName());
    
    private static Report instance;
    
    private DBKIWIConnection KIWIConn = null;
   
    private DBKIWIModule DMKIWI = null;
    
    List LabwareList = null;
     private String iuri, puri, euri;
    String STXAddress = "";
    Integer STXPort = 0;
    
    String KIWIDBDriver = "org.firebirdsql.jdbc.FBDriver";
    
    String KIWIDBPath = "";
       
    String KIWIDBUser = "";
    String KIWIDBPswd = "";   
    
    String ReportPath = "";
    
    public void setKIWIDBDriver(String value){KIWIDBDriver = value;}       
    public void setKIWIDBPath(String value){KIWIDBPath = value;}         
    public void setKIWIDBUser(String value){KIWIDBUser = value;}  
    public void setKIWIDBPswd(String value){KIWIDBPswd = value;}    
    public void setReportPath(String value){ReportPath = value;}
        
    public static Report getInstance(){
        
        if(instance == null)
            instance = new Report();
        
        return instance;
        
    }    

    public void Init()
    {
        try{
            
            KIWIConn = new DBKIWIConnection(KIWIDBDriver, KIWIDBPath, KIWIDBUser, KIWIDBPswd);
            
            DMKIWI = new DBKIWIModule(KIWIConn, this);
                                    
            System.out.println("REPORT KIWI Database connection created");            
                                                      
        }
        catch(Exception E){
            System.out.println("REPORT Eerror: Create KIWI Database connection - "+E.getMessage());
        }
        
    }    
    
    public DBKIWIModule getDMKIWI() {
        return DMKIWI;
    }      

    public void errorReport(int taskid, String error)
    {
        int jobid = DMKIWI.JobByTaskId(taskid).getId();
        JsonObjectBuilder jobReport = Json.createObjectBuilder();
        jobReport.add("freezerId", "liconic01");
        jobReport.add("jobId", jobid);
        jobReport.add("jobType","pick");
        jobReport.add("user", "ADMIN");
        jobReport.add("status","failed");
        jobReport.add("message", error);   
        
        try{
        JsonWriter fileWriter = Json.createWriter(new FileWriter(ReportPath + "\\" + String.valueOf(jobid) + "_pick.txt"));
        JsonObject report = jobReport.build(); 
        fileWriter.writeObject(report);
        fileWriter.close();
          System.out.println("pick job Error Report: " + report.toString());
        String link = getURIPick();
        sendReports(report.toString(),link); 
        }
        
        catch(Exception e)
        {
            System.out.println("Error Report pick job Exception: " + e.getMessage());
        }
    }
    
    public void inventory2D(Sys sys){
        
        int nPlates =  sys.getPlates().getValue().getPlate().size();
        for(int i =0;i<nPlates;i++){
         System.out.println("");   
         System.out.println("Plate"+i);
         String barcode = (String) sys.getPlates().getValue().getPlate().get(i).getBarcode().getValue();
         int nTubes = sys.getPlates().getValue().getPlate().get(i).getTubePositions().getValue().getTubePos().size();
            for(int j = 0;j<nTubes; j++){
                String tubeBarcode = sys.getPlates().getValue().getPlate().get(i).getTubePositions().getValue().getTubePos().get(j).getTube().getValue().getBarcode().getValue();
                int tubeX = sys.getPlates().getValue().getPlate().get(i).getTubePositions().getValue().getTubePos().get(j).getX().getValue();
                char tubeY = (char) ((sys.getPlates().getValue().getPlate().get(i).getTubePositions().getValue().getTubePos().get(j).getY().getValue()) + 64);
                 
                String tubePos = tubeY + Integer.toString(tubeX) ;
                String info;
                System.out.println("tubePose: " + tubePos);   
                System.out.println("Tube Barcode: "+  tubeBarcode);
                             
                if(sys.getPlates().getValue().getPlate().get(i).getTubePositions().getValue().getTubePos().get(j).getInfo() !=null){
                   info  = sys.getPlates().getValue().getPlate().get(i).getTubePositions().getValue().getTubePos().get(j).getInfo().getValue();
                    System.out.println("Info: " + info);
                }
            }
        }
    }
    public void TaskReport(int idTask){
         
        Job job = DMKIWI.JobByTaskId(idTask);
        
        Task task = null;
        
        System.out.println("Job id: " + job.getId());
        if (job != null){                       
            
            if (job.getDone().equals("1")){
                                
                if (job.getName().equals("Import")){
                                     
                    for(Task tsk : job.getTasks()){
                        
                        if (tsk.getId() == idTask){
                            task = tsk;
                          //  System.out.println("Import successful "+task.getId());
                             ImportTaskReport(job, task);
                             
                             break;
                        }                                               
                    }
                    
//                    if (task != null){
//
//                        ImportTaskReport(job, task);
//                        
//                    }

                } else if (job.getName().equals("Export")){
                    
                    for(Task tsk : job.getTasks()){
                        
                        if (tsk.getId() == idTask){
                            task = tsk;
                            break;
                        }                                               
                    }
                    
                    if (task != null){

                        ExportTaskReport(job, task);
                        
                    }                    
                    
                    
                } else if (job.getName().equals("Job File")){
                    
                    for(Task tsk : job.getTasks()){
                        
                        if (tsk.getId() == idTask){
                            
                            if (tsk.getName().equals("Export")){
                                
                                task = tsk;
                                
                                ExportTaskReport(job, task);
                                
                                return;
                            }                            
          
                        }                                               
                    }
                    
                }
                
            } else{
                
                if (job.getName().equals("Job File")){
                                     
                    for(Task tsk : job.getTasks()){
                        
                        if (tsk.getId() == idTask){
                            
                            if (tsk.getName().equals("Job File")) {                          
                                task = tsk;
                                PickTaskReport(job, task);
                            return;
                            }
                                
                            if (tsk.getName().equals("Export")){
                                task = tsk;
                                ExportTaskReport(job, task);
                                return;
                            }
                        }                                               
                    }
                    
                    if (task != null){

                        PickTaskReport(job, task);
                    }
                }
            }
            
        }else{
            System.out.println("REPORT Can not find Job by Task Id = "+idTask);
        }
        
    }     
    
  private void ImportTaskReport(Job job, Task task) {

         int jobId = job.getId();
        String jobType = "import";
        String user = job.getUser();
        String status = "";
        String errorInfo = "";
        Plate plate = null;
        JsonObjectBuilder jobReport = null;

        // Plate
        JsonObjectBuilder dataReport = null;
        JsonObjectBuilder plateReport = null;
  
//        JsonArrayBuilder plateArray = null;  
        JsonObjectBuilder tubePos = null;

        if (task.getName().equals("Scan 2D")) {

            if (task.getCstat().equals("Done")) {

                status = "complete";
                plate = DMKIWI.GetExportPlate(task.getId());

                jobReport = Json.createObjectBuilder();

            }else if (task.getCstat().equals("Error")) {
                
                status = "failed";
                errorInfo = DMKIWI.GetTaskStepInfo(task.getId());
                if(errorInfo.equalsIgnoreCase("Decode failure"))
                {
                  plate = DMKIWI.GetExportPlate(task.getId());  
                }
                
                jobReport = Json.createObjectBuilder();

            }

        } else if (task.getName().equals("ImportXfer")) {

            if (task.getCstat().equals("Done")) {
                status = "complete";
                plate = DMKIWI.GetExportPlate(task.getId());

                jobReport = Json.createObjectBuilder();

            }else if (task.getCstat().equals("Error")) {
                
                status = "failed";
                errorInfo = DMKIWI.GetTaskStepInfo(task.getId());
                
                plate = DMKIWI.GetExportPlate(task.getId());  
                              
                jobReport = Json.createObjectBuilder();
            }
        }
        
        try {

            if (jobReport != null) {

                if (plate != null) {

                    // Plate
                    plateReport = Json.createObjectBuilder().add("rackBarcode", plate.getBarcode().getValue());
                    
                      if (plate.getTubePositions() != null) {

                                JsonArrayBuilder tubes = Json.createArrayBuilder();

                                for (TubePos pos : plate.getTubePositions().getValue().getTubePos()) {

                                    String bc = pos.getTube().getValue().getBarcode().getValue();

                                    String tp = GetYA(pos.getY().getValue()) + String.valueOf(pos.getX().getValue());

                                    tubePos = Json.createObjectBuilder();

                                    tubePos.add("barcode", bc).add("location", tp);

                                    tubes.add(tubePos.build());
                                }
                                plateReport.add("tubes", tubes);
                      }
                                JsonArrayBuilder platesArray = Json.createArrayBuilder().add(plateReport);
                                dataReport = Json.createObjectBuilder().add("racks",platesArray);
                      
                }
                jobReport.add("freezerId", "liconic01");
                jobReport.add("jobId", jobId);
                jobReport.add("jobType", jobType);
                jobReport.add("user", user);
                jobReport.add("status", status);

                if (dataReport != null) {
                    jobReport.add("data", dataReport);
                }

                if (!errorInfo.isEmpty()) {
                    jobReport.add("message", errorInfo);
                }
                
                 File file = new File(ReportPath + "\\" + String.valueOf(jobId) + "_" + jobType + ".txt");
                if(file.exists())
                {
                    file = new File(ReportPath + "\\" + String.valueOf(jobId) + "_" + jobType + "-2.txt");
                }
                
                FileWriter fw = new FileWriter(file);
                
                JsonWriter fileWriter = Json.createWriter(fw);
                
                JsonObject report = jobReport.build();
                
             //   System.out.println("Import report : "+ report.toString() );
                           
                fileWriter.writeObject(report);
                
                fileWriter.close();
                
                String link = getURIImport();
                
          //      System.out.println("Import Link: " + link);
                
                sendReports(report.toString(),link);   
            }

        } catch (Exception E) {

            System.out.println(E.getMessage());

        }
    }
    
  private void PickTaskReport(Job job, Task task) {
           
        int jobId = job.getId();
        String jobType = "pick";
        String user = job.getUser();
        String status = "";

        JsonObjectBuilder jobReport = null;
        
        List<Plate> plates = null;

        // Plate
        JsonObjectBuilder dataReport = null;
        
        // Plate
        JsonObjectBuilder platesReport = null;     
        JsonArrayBuilder platesArray = null;
        
        // Tube Pos
        JsonObjectBuilder tubePos = null;        

        if (task.getCstat().equals("Done")) {

            status = "complete";
            plates = DMKIWI.GetPlatesFromPiclJob(task.getId());

            jobReport = Json.createObjectBuilder();

        }
        
        boolean isPlates = false;

        try {

            if (jobReport != null) {

                if ((plates != null) && (!plates.isEmpty())) {
                    
//                    if(plates.size() == 1) {
//                        
//                        Plate plate = plates.get(0);
//
//                        // Plate
//                        dataReport = Json.createObjectBuilder().add("rackBarcode", plate.getBarcode().getValue());
//
//                        if (plate.getTubePositions() != null) {
//
//                            JsonArrayBuilder joa = Json.createArrayBuilder();
//
//                            for (TubePos pos : plate.getTubePositions().getValue().getTubePos()){
//
//                                String bc = pos.getTube().getValue().getBarcode().getValue();
//
//                                String tp = GetYA(pos.getY().getValue())+String.valueOf(pos.getX().getValue());
//
//                                tubePos = Json.createObjectBuilder(); 
//
//                                tubePos.add("barcode", bc).add("location", tp);
//
//                                joa.add(tubePos.build());
//
//                            }                                            
//
//                            dataReport.add("tubes", joa);
//                        }
//                        
//                    }else{
//                        {
                     isPlates = true;
                        
                        for (Plate plate : plates) {

                            if (platesReport == null){

                                platesReport = Json.createObjectBuilder();

                                platesArray = Json.createArrayBuilder();                                                                              

                            }                       

                            // Plate
                            dataReport = Json.createObjectBuilder().add("rackBarcode", plate.getBarcode().getValue());

                            if (plate.getTubePositions() != null) {

                                JsonArrayBuilder tubes = Json.createArrayBuilder();

                                for (TubePos pos : plate.getTubePositions().getValue().getTubePos()) {

                                    String bc = pos.getTube().getValue().getBarcode().getValue();

                                    String tp = GetYA(pos.getY().getValue()) + String.valueOf(pos.getX().getValue());

                                    tubePos = Json.createObjectBuilder();

                                    tubePos.add("barcode", bc).add("location", tp);

                                    tubes.add(tubePos.build());

                                }

                                dataReport.add("tubes", tubes);
                            }

                            platesArray.add(dataReport);                        

                        }

                        platesReport.add("racks", platesArray); 
                        
                    }
                    
                }

                jobReport.add("freezerId", "liconic01");
                jobReport.add("jobId", jobId);
                jobReport.add("jobType", jobType);
                jobReport.add("user", user);
                jobReport.add("status", status);

                if (isPlates){
                    
                    // multi Plates                
                    if (platesReport != null) {
                        jobReport.add("data", platesReport);
                    }

                }else{
                    // Single Plate
                    if (dataReport != null) {
                        jobReport.add("data", dataReport);
                    }                    
                }
                
                JsonWriter fileWriter = Json.createWriter(new FileWriter(ReportPath + "\\" + String.valueOf(jobId) + "_" + jobType + ".txt"));

                JsonObject report = jobReport.build();
                
                fileWriter.writeObject(report);
                
          //      System.out.println("PickJob report : "+ report.toString() );
                
                fileWriter.close();
                
                String link = getURIPick();
                
        //        System.out.println("Pickjob Link: " + link);
                
                sendReports(report.toString(),link); 

        }catch (Exception E) {

            System.out.println("Pick job reporting error:" +E.getMessage());

        }        
    }    
    
  private void ExportTaskReport(Job job, Task task) {

        int jobId = job.getId();
        String jobType = "export";
        String user = job.getUser();
        String status = "";

        Plate plate = null;

        JsonObjectBuilder plateReport = null;
        JsonObjectBuilder jobReport = null;
        JsonArrayBuilder plateArray = null;  
        JsonObjectBuilder tubePos = null;

        JsonObjectBuilder dataReport;
        dataReport = null;

        if (task.getCstat().equals("Done")) {
            status = "complete";
             jobReport = Json.createObjectBuilder();
             plateReport = Json.createObjectBuilder();
          plateArray = Json.createArrayBuilder();
          plate = DMKIWI.GetExportPlate(task.getId());
        }
        
        try {

            if (jobReport != null) {

              if (plate != null) {

                    plateReport = Json.createObjectBuilder().add("rackBarcode", plate.getBarcode().getValue());
                  
                      if (plate.getTubePositions()!= null) {
                   
                    JsonArrayBuilder tubes = Json.createArrayBuilder();

                                for (TubePos pos : plate.getTubePositions().getValue().getTubePos()) {
                                    
                                    String bc = pos.getTube().getValue().getBarcode().getValue();

                                    String tp = GetYA(pos.getY().getValue()) + String.valueOf(pos.getX().getValue());

                                    tubePos = Json.createObjectBuilder();

                                    tubePos.add("barcode", bc).add("location", tp);

                                    tubes.add(tubePos.build());
                                }
                                plateReport.add("tubes", tubes);
                      }
                                JsonArrayBuilder platesArray = Json.createArrayBuilder().add(plateReport);
                                dataReport = Json.createObjectBuilder().add("racks",platesArray);
              }
              
                jobReport.add("freezerId", "liconic01");
                jobReport.add("jobId", jobId);
                jobReport.add("jobType", jobType);
                jobReport.add("user", user);
                jobReport.add("status", status);

                if (dataReport != null) {
                    jobReport.add("data", dataReport);
                }
                
                File file = new File(ReportPath + "\\" + String.valueOf(jobId) + "_" + jobType + ".txt");
                if(file.exists())
                {
                    file = new File(ReportPath + "\\" + String.valueOf(jobId) + "_" + jobType + "-2.txt");
                }
                
                FileWriter fw = new FileWriter(file);
                
                JsonWriter fileWriter = Json.createWriter(fw);
                
                JsonObject report = jobReport.build();
                
            //    System.out.println("Export report : "+ report.toString());
                
                fileWriter.writeObject(report);
                        
                fileWriter.close();
                
                String link = getURIExport();
                
        //        System.out.println("Export Link: " + link);
                
                sendReports(report.toString(),link);               
            }

        } catch (Exception E) {

            System.out.println("Unable to create file " + E.getMessage());

        }

    }    


    private String GetYA(int y){
        
        String ya = "";
        
        if (y == 1)
            ya = "A";
        else if (y == 2)
            ya = "B";
        else if (y == 3)
            ya = "C";
        else if (y == 4)
            ya = "D";
        else if (y == 5)
            ya = "E";
        else if (y == 6)
            ya = "F";
        else if (y == 7)
            ya = "G";
        else if (y == 8)
            ya = "H";

        return ya;
        
    }
    
    public String getURIImport()     {
         return iuri;
     }

public String getURIExport()     {
         return euri;
     }
     
public String getURIPick()     {
         return puri;
     }
    

public void setURIPick(String puri)    {
        this.puri = puri;
    }

public void setURIExport(String euri)    {
        this.euri = euri;
    }

public void setURIImport(String iuri)    {
        this.iuri = iuri;
    }
  
public  void sendReports(String report, String uri){
    String response;
//    System.out.println("Report to Moderna: " + report);
//    System.out.println("Report sent to : " + uri);
    log.info(report);

     DefaultHttpClient httpClient = new DefaultHttpClient();
    try {
      HttpPost postRequest = new HttpPost(uri);
      postRequest.setHeader("Content-Type", "application/json");
      HttpEntity entity = new ByteArrayEntity(report.getBytes("UTF-8"));
      postRequest.setEntity(entity);
      HttpResponse httpResponse = httpClient.execute(postRequest);
  
      byte[] buffer = new byte[1024];
      if (httpResponse != null) {
        InputStream inputStream = httpResponse.getEntity().getContent();
        try {
          int bytesRead = 0;
          BufferedInputStream bis = new BufferedInputStream(inputStream);
          StringBuilder stringBuilder = new StringBuilder();
          while ((bytesRead = bis.read(buffer)) != -1) {
          stringBuilder.append(new String(buffer, 0, bytesRead));
          }
         
         response  = stringBuilder.toString();
         System.out.println("Response from Moderna: " + response);
         log.info(response);
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          try { inputStream.close(); } catch (Exception ignore) {}
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      httpClient.getConnectionManager().shutdown();
    }
}

}
