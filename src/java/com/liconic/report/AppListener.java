/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liconic.report;

import com.liconic.binding.conffiles.Parameter;
import com.liconic.binding.conffiles.ParameterGroup;
import com.liconic.binding.conffiles.Parameters;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

/**
 * Web application lifecycle listener.
 *
 * @author GKo
 */
public class AppListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        
        ServletContext context = sce.getServletContext();
               
        String ParamFile = "";

        String KIWIDBDriver = "";
    
        String KIWIDBUser = "";
        String KIWIDBPswd = "";     
        String KIWIConnectString = "";       
        
        String ReportPath = "";
              
        ParamFile = context.getInitParameter("ConfigFile");
        
        System.out.println("REPORT read config: Reading ConfFile:"+ParamFile);
        
          Report report = Report.getInstance();
        try {

            JAXBContext jaxbContent = JAXBContext.newInstance(com.liconic.binding.conffiles.ObjectFactory.class);

            Unmarshaller um = jaxbContent.createUnmarshaller();

            FileInputStream fis = new FileInputStream(ParamFile);

            Parameters params = (Parameters) um.unmarshal(fis);

            for (int i = 0; i < params.getParameterGroup().size(); i++) {

                ParameterGroup paramGroup = (ParameterGroup) params.getParameterGroup().get(i);

                if (paramGroup.getName().equals("KIWI Database")) {

                    for (int j = 0; j < paramGroup.getParameter().size(); j++) {
                        Parameter param = (Parameter) paramGroup.getParameter().get(j);

                        if (param.getName().equals("KIWIDBDriver")) {
                            KIWIDBDriver = param.getValue();
                        } else {
                            if (param.getName().equals("KIWIDBUser")) {
                                KIWIDBUser = param.getValue();
                            } else {
                                if (param.getName().equals("KIWIDBPswd")) {
                                    KIWIDBPswd = param.getValue();
                                } else {
                                    if (param.getName().equals("KIWIConnectString")) {
                                        KIWIConnectString = param.getValue();
                                    }
                                }
                            }
                        }

                    }

                }
                 else if (paramGroup.getName().equals("KIWI CallBack URI")){
                 
                    
                    for(int j=0; j<paramGroup.getParameter().size(); j++)
                    {
                        
                        Parameter param = (Parameter)paramGroup.getParameter().get(j);
                        
                        if (param.getName().equals("uriImport"))
                        {
                            String uri = "";
                            uri = param.getValue();    
                            System.out.println("Callback import :" + uri);
                            report.setURIImport(uri);
                        }
                        
                        else if(param.getName().equals("uriExport"))
                        {
                            String uri = "";
                            uri = param.getValue();
                            System.out.println("Callback export :" + uri);
                            report.setURIExport(uri);
                        }
                        
                        else if(param.getName().equals("uriPick"))
                        {
                            String uri = "";
                            uri = param.getValue();
                            System.out.println("Callback pickjob :" + uri);
                            report.setURIPick(uri);
                        } 
                    }
                }
                
                 if (paramGroup.getName().equals("Report")) {

                    for (ParameterGroup reportGroup : paramGroup.getParameterGroup()) {

                        for (Parameter param : reportGroup.getParameter()) {

                            if (param.getName().equals("Path")) {
                                ReportPath = param.getValue();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
       
        // STX DataBase
        System.out.println("REPORT read config: STX Database Settings --------------------");                
        System.out.println("REPORT read config: KIWIDBDriver: "+KIWIDBDriver);
        System.out.println("REPORT read config: KIWIDBUser: "+KIWIDBUser);
        System.out.println("REPORT read config: KIWIDBPswd: "+KIWIDBPswd);
        System.out.println("REPORT read config: KIWIConnectString: "+KIWIConnectString);
        
        System.out.println("");       
        System.out.println("");                                       
               
        System.out.println("REPORT Path: "+ReportPath);
        
      
        
        report.setKIWIDBDriver(KIWIDBDriver);
        report.setKIWIDBPath(KIWIConnectString);
        report.setKIWIDBPswd(KIWIDBPswd);
        report.setKIWIDBUser(KIWIDBUser);      

        report.setReportPath(ReportPath);
                
        context.setAttribute("DBPath", KIWIConnectString);    
        context.setAttribute("KIWIReport", report);
        
        report.Init();        
                
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        
    }
}
