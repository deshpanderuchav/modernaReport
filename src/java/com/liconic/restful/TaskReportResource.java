/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liconic.restful;

import com.liconic.binding.sys.Job;
import com.liconic.binding.sys.Sys;
import com.liconic.binding.sys.Task;
import com.liconic.binding.sys.Property;
import com.liconic.report.Report;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.Element;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;

/**
 * REST Web Service
 *
 * @author GKo
 */
@Path("taskreport")
public class TaskReportResource {
    
    private static final Logger log = Logger.getLogger( TaskReportResource.class.getName() );
    
    @Context
    private UriInfo context;

    private Report report;
    

    public TaskReportResource() {
        report = Report.getInstance();
        
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public void putXml(Sys sys) {

        try {
            StringWriter sw = new StringWriter();

            JAXBContext context = JAXBContext.newInstance(com.liconic.binding.sys.ObjectFactory.class);

            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            m.marshal(sys, sw);
            
            if(sys.getJobs() != null){
                Task task = sys.getJobs().getValue().getJob().get(0).getTasks().get(0);
                if(report != null)
                {
                    
                    System.out.println("REPORT TaskReport:\r" + sw.toString());
                     log.info(sw.toString());
                    if(sys.getJobs().getValue().getJob().get(0).getTasks().get(0).getProps().isEmpty())
                    {
                        //System.out.println(task.getId());  
                        report.TaskReport(task.getId());
                        
                    }
                    else
                    {
                        String val = sys.getJobs().getValue().getJob().get(0).getTasks().get(0).getProps().get(0).getVal();
                        report.errorReport(task.getId(), val); 
                    }
                }
                else{
                    System.out.println("Report NULL");  
                }
            }
            else {
                System.out.println("REPORT TaskReport:\r" + sw.toString());
                  log.info(sw.toString());
                report.inventory2D(sys);
            }

        } catch (Exception E) {
            System.out.println("REPORT Error save XML export command: " + E.toString());
        }
          log.info("Exiting putXML");
    }
}
