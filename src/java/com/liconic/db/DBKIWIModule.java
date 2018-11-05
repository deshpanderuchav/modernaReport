package com.liconic.db;

import com.liconic.binding.sys.Job;
import com.liconic.binding.sys.ObjectFactory;
import com.liconic.binding.sys.Plate;
import com.liconic.binding.sys.Task;
import com.liconic.binding.sys.Tube;
import com.liconic.binding.sys.TubePos;
import com.liconic.binding.sys.TubePositions;
import com.liconic.report.Report;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DBKIWIModule {

    private DBKIWIConnection DB;
    private Report report = null;

    public DBKIWIModule(DBKIWIConnection DB, Report report) {
        this.DB = DB;
        this.report = report;
    }
   

    public Job JobByTaskId(int idTask){
        Connection conn;
        PreparedStatement stat;
        String SQLVal;        
        ResultSet rs; 
                
        Job job = null;
        
        try{
            
            conn = DB.getConnection();
            
            //                  1           2           3           4                   5               6
            SQLVal = "SELECT id_task, task_sequence, id_job, task_types.task_type, job_types.job_type, done, user_login " +
                    "FROM tasks, jobs, task_types, job_types, users " +
                    "WHERE link_job=id_job AND tasks.task_type=id_task_type AND " +
                    "jobs.job_type=id_job_type " +
                    "AND id_job IN " +
                    "( " +
                    "  SELECT link_job " +
                    "  FROM tasks " +
                    "  WHERE id_task=? " +
                    ") " +
                    "AND link_user=id_user " +
                    "GROUP BY id_task, task_sequence, id_job, task_types.task_type, job_types.job_type, done, user_login " +
                    "ORDER BY task_sequence";
            
            stat = conn.prepareStatement(SQLVal);
            
            stat.setInt(1, idTask);
            
            rs = stat.executeQuery();
            
            while (rs.next()){

                if (job == null){
                    
                    job = new Job();
                    
                    job.setId(rs.getInt("id_job"));
                    //System.out.println("Job ID: "+ job.getId() + "TaskId: " + idTask);
                    job.setName(rs.getString(5));
                    
                    job.setDone(rs.getString("done"));
                    
                    job.setUser(rs.getString("user_login"));
                }
                
                Task task = new Task();
                
                task.setId(rs.getInt("id_task"));
                task.setName(rs.getString(4));
                
                job.getTasks().add(task);
                
            }            
            
            rs.close();
            stat.close();    
            
            if (job != null){
            
                for (Task task : job.getTasks()){
                
                    stat = conn.prepareStatement("SELECT task_status " +
                                    "FROM task_link_status, task_status " +
                                    "WHERE link_task=? AND link_status=id_task_status " +
                                    "AND id_task_link_status IN " +
                                    "( " +
                                    "  SELECT MAX(id_task_link_status) " +
                                    "  FROM task_link_status " +
                                    "  WHERE link_task=? " +
                                    ")");
                
                    stat.setInt(1, task.getId());
                    stat.setInt(2, task.getId());                
                
                    rs = stat.executeQuery();
                
                    while(rs.next()){
                                                            
                        task.setCstat(rs.getString("task_status"));
                    
                        break;
                    
                    }
                
                }
            }
            
            conn.close();
            
        } catch (Exception E){
            System.out.println("ERROR: JobByTaskId("+idTask+"): "+E.getMessage() );
        }
        
        return job;
    }

    public Plate GetPlateFoTask(int taskId){
        
        Connection conn;
        PreparedStatement stat;
        ResultSet rs; 
        
        Plate plate = null;
        
        ObjectFactory of = new ObjectFactory();
        
        try{
            
            conn = DB.getConnection();
            
            stat = conn.prepareStatement("SELECT plate_bcr " +
                    "FROM task_src_link_trg, plate_link_task, plates " +
                    "WHERE task_src_link_trg.link_task=:LT AND plate_link_task.link_task=id_src_link_trg AND " +
                    "link_plate=id_plate");
            
            stat.setInt(1, taskId);
            
            rs = stat.executeQuery();
            
            while (rs.next()){

                plate = new Plate();                    
                plate.setBarcode(of.createPlateBarcode(rs.getString("plate_bcr")));

                break;
            }            
            
            rs.close();
            stat.close();    

            
            conn.close();
            
        } catch (Exception E){
            System.out.println("ERROR: GetPlateFoTask("+taskId+"): "+E.getMessage() );
        }
        
        return plate;
        
    }
    
    public String GetTaskStepInfo(int taskId){
        
        Connection conn;
        PreparedStatement stat;
        ResultSet rs;
 
        String res = "";
        
        try{
            
            conn = DB.getConnection();
            
            stat = conn.prepareStatement("SELECT src_trg_info " +
                                         "FROM TASK_SRC_LINK_TRG " +
                                         "WHERE link_task=?");
            
            stat.setInt(1, taskId);
            
            rs = stat.executeQuery();
            
            while (rs.next()){

                res = rs.getString(1);
                break;
                
            }            

            stat.close();    

            
            conn.close();
            
        } catch (Exception E){
            System.out.println("ERROR: GetTaskStepInfo("+taskId+"): "+E.getMessage() );
        }
        
        if (res == null)
            res = "";
        
        return res;        
    }
    
    public Plate GetImportPlate(int taskId){
              Connection conn;
        PreparedStatement stat;
        ResultSet rs; 
        int idPlate = 0;
        Plate plate = null;
        TubePositions positions = null;
         List<Plate> plates = new ArrayList<>();
        
        ObjectFactory of = new ObjectFactory();
        
         try{
            
            conn = DB.getConnection();
            
            stat = conn.prepareStatement("select id_plate, plate_bcr, tubes.tube_bcr, tube_x,tube_y from task_src_link_trg\n" +
"inner join plate_link_task on plate_link_task.link_task=task_src_link_trg.id_src_link_trg\n" +
"inner join  plates on plates.id_plate = plate_link_task.link_plate\n" +
"INNER join plate_link_tube_pos on  plates.id_plate=plate_link_tube_pos.link_plate\n" +
"inner join tubes_link_plate on  plate_link_tube_pos.id_link_tube_pos = tubes_link_plate.link_pos\n" +
"inner join tube_positions on tube_positions.id_tube_position=plate_link_tube_pos.link_tube_pos\n" +
"inner join tubes on tubes.id_tube = tubes_link_plate.link_tube\n" +
"where task_src_link_trg.link_task = ?");
            
            stat.setInt(1, taskId);
            
            rs = stat.executeQuery();
            
            while (rs.next()){

                if (idPlate != rs.getInt("id_plate")){
                    
                    idPlate = rs.getInt("id_plate");
                    
                    plate = new Plate();
                    
                    plate.setBarcode(of.createPlateBarcode(rs.getString("plate_bcr"))); 
                    
                    positions = new TubePositions();
                    
                    plate.setTubePositions(of.createPlateTubePositions(positions));
                    
                    plates.add(plate);
                }
                    
                TubePos pos = new TubePos();
                
                pos.setX(of.createTubePosX(rs.getInt("tube_x")));
                pos.setY(of.createTubePosX(rs.getInt("tube_y")));
                    
                Tube tube = new Tube();
                tube.setBarcode(of.createTubeBarcode(rs.getString("tube_bcr")));
                    
                pos.setTube(of.createTubePosTube(tube));
                                         
                positions.getTubePos().add(pos);
                    
                         
            }          
            rs.close();
            stat.close();    

            
            conn.close();
            
        } catch (Exception E){
            System.out.println("ERROR: GetExportPlate("+taskId+"): "+E.getMessage() );
        }
        
        
        return plate;
        /*
        Connection conn;
        PreparedStatement stat;
        ResultSet rs; 
        
        Plate plate = null;
        TubePositions positions = null;
        
        
        ObjectFactory of = new ObjectFactory();
        
        try{
            
            conn = DB.getConnection();
            
            stat = conn.prepareStatement("SELECT id_plate, plate_bcr, id_tube, tube_bcr " +
                    "FROM task_src_link_trg " +
                    "INNER JOIN plate_link_task ON plate_link_task.link_task=id_src_link_trg " +
                    "INNER JOIN plates ON plate_link_task.link_plate=id_plate " +
                    "LEFT JOIN plate_link_tube_pos ON plate_link_tube_pos.link_plate=id_plate " +
                    "LEFT JOIN tubes_link_plate ON link_pos=id_link_tube_pos " +
                    "LEFT JOIN tubes ON link_tube=id_tube " +
                    "WHERE task_src_link_trg.link_task=? " +
                    "GROUP BY id_plate, plate_bcr, id_tube, tube_bcr");
            
            stat.setInt(1, taskId);
            
            rs = stat.executeQuery();
            
            while (rs.next()){

                if (plate == null){
                    
                    plate = new Plate();
                    
                    plate.setBarcode(of.createPlateBarcode(rs.getString("plate_bcr")));
                    
                }
                
                if (rs.getInt("id_tube") != 0){ 
    
                    if (positions == null)
                        positions = new TubePositions();
                    
                    TubePos pos = new TubePos();
                    
                    Tube tube = new Tube();
                    tube.setBarcode(of.createTubeBarcode(rs.getString("tube_bcr")));
                    
                    pos.setTube(of.createTubePosTube(tube));
                    positions.getTubePos().add(pos);
                    
                }
                
            }            
            
            if ( (plate != null) && (positions != null)){
                plate.setTubePositions(of.createPlateTubePositions(positions) );
            }
            
            rs.close();
            stat.close();    

            
            conn.close();
            
        } catch (Exception E){
            System.out.println("ERROR: GetImportPlate("+taskId+"): "+E.getMessage() );
        }
        
        return plate;
      */  
    }
    
    public List<Plate> GetPlatesFromPiclJob(int idTask){
                
        Connection conn;
        PreparedStatement stat;
        ResultSet rs; 
        
        Plate plate = null;
        int idPlate = 0;
        TubePositions positions = null;
                
        ObjectFactory of = new ObjectFactory();
        
        List<Plate> plates = new ArrayList<>();
        
        try{
            
            conn = DB.getConnection();
            
            stat = conn.prepareStatement("SELECT id_plate, plate_bcr, tube_y, tube_x, tube_bcr " +
                    "FROM task_src_link_trg, tube_link_task, tubes, tubes_link_plate, " +
                    "plate_link_tube_pos, tube_positions, plates " +
                    "WHERE task_src_link_trg.link_task=? AND tube_link_task.link_task=id_src_link_trg AND " +
                    "is_done=1 AND tube_link_task.link_tube=id_tube AND tubes_link_plate.link_tube=id_tube AND " +
                    "link_pos=id_link_tube_pos AND link_tube_pos=id_tube_position AND link_plate=id_plate " +
                    "GROUP BY id_plate, plate_bcr, tube_y, tube_x, tube_bcr " +
                    "ORDER BY id_plate, plate_bcr, tube_y, tube_x");
            
            stat.setInt(1, idTask);
            
            rs = stat.executeQuery();
            
            while (rs.next()){

                if (idPlate != rs.getInt("id_plate")){
                    
                    idPlate = rs.getInt("id_plate");
                    
                    plate = new Plate();
                    
                    plate.setBarcode(of.createPlateBarcode(rs.getString("plate_bcr"))); 
                    
                    positions = new TubePositions();
                    
                    plate.setTubePositions(of.createPlateTubePositions(positions));
                    
                    plates.add(plate);
                }
                    
                TubePos pos = new TubePos();
                
                pos.setX(of.createTubePosX(rs.getInt("tube_x")));
                pos.setY(of.createTubePosX(rs.getInt("tube_y")));
                    
                Tube tube = new Tube();
                
                tube.setBarcode(of.createTubeBarcode(rs.getString("tube_bcr")));
                    
                pos.setTube(of.createTubePosTube(tube));
                
                positions.getTubePos().add(pos);
              
            }          
            rs.close();
            stat.close();    

            
            conn.close();
            
        } catch (Exception E){
            System.out.println("ERROR: GetPlatesFromPiclJob("+idTask+"): "+E.getMessage() );
        }
        
        return plates;        
        
    }
    
     public Plate GetExportPlate(int taskId){
        
        Connection conn;
        PreparedStatement stat;
        ResultSet rs; 
        int idPlate = 0;
        Plate plate = null;
        TubePositions positions = null;
        
        ObjectFactory of = new ObjectFactory();
        
         try{
            
            conn = DB.getConnection();
            
            stat = conn.prepareStatement("SELECT id_plate, plate_bcr, id_tube, tube_bcr, tube_x, tube_y\n" +
"                    FROM task_src_link_trg\n" +
"                    INNER JOIN plate_link_task ON plate_link_task.link_task=id_src_link_trg\n" +
"                    INNER JOIN plates ON plate_link_task.link_plate=id_plate\n" +
"                    LEFT JOIN plate_link_tube_pos ON plate_link_tube_pos.link_plate=id_plate\n" +
"\n" +
"                    LEFT JOIN tubes_link_plate ON link_pos=id_link_tube_pos\n" +
"                    left join tube_positions on tube_positions.id_tube_position = plate_link_tube_pos.link_tube_pos\n" +
"                    LEFT JOIN tubes ON link_tube=id_tube\n" +
"                    WHERE task_src_link_trg.link_task= ? \n" +
"                      GROUP BY id_plate, plate_bcr, id_tube, tube_bcr,tube_x, tube_y");
            
            stat.setInt(1, taskId);
            
            rs = stat.executeQuery();
            
           while (rs.next()){

                if (plate == null){
                    
                    plate = new Plate();
                    
                    plate.setBarcode(of.createPlateBarcode(rs.getString("plate_bcr")));
                }
                if (rs.getInt("id_tube") != 0){ 
    
                    if (positions == null)
                        positions = new TubePositions();
                    
                    TubePos pos = new TubePos();
                    
                    pos.setX(of.createTubePosX(rs.getInt(5)));
                    pos.setY(of.createTubePosX(rs.getInt(6)));
                    
                    Tube tube = new Tube();
                    tube.setBarcode(of.createTubeBarcode(rs.getString("tube_bcr")));
                    
                    pos.setTube(of.createTubePosTube(tube));
                                        
                                        
                    positions.getTubePos().add(pos);
                    
                }
                
            }            
            
            if ( (plate != null) && (positions != null)){
                plate.setTubePositions(of.createPlateTubePositions(positions));
            }
            
            rs.close();
            stat.close();    

            
            conn.close();
            
        } catch (Exception E){
            System.out.println("ERROR: GetExportPlate("+taskId+"): "+E.getMessage() );
        }
        
        
        return plate;
        
    }
    
}
