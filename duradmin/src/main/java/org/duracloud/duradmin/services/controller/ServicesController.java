/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.duradmin.services.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.duracloud.serviceapi.ServicesManager;
import org.duracloud.serviceconfig.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * 
 * @author Daniel Bernstein
 *
 */
@Controller
public class ServicesController {

    protected final Logger log = LoggerFactory.getLogger(ServicesController.class);
    
	private ServicesManager servicesManager;

    @Autowired
    public ServicesController(
        @Qualifier("servicesManager") ServicesManager servicesManager) {
        this.servicesManager = servicesManager;
    }

    @RequestMapping("/services")
	public ModelAndView getServices(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		if("json".equals(request.getParameter("f"))){
			String method = request.getParameter("method");	
			List<ServiceInfo> services = null;
			if("available".equals(method)){
				services = servicesManager.getAvailableServices();	
			}else{
				services = servicesManager.getDeployedServices();
			}

	        ModelAndView mav = new ModelAndView("jsonView");
			mav.addObject("services",services);
			return mav;
		}else{
	        ModelAndView mav = new ModelAndView("services-manager");
	        return mav;
		}
	}
}